import { NextResponse } from "next/server";
import { prisma } from "@/lib/server/prisma";
import {
  isValidEmail,
  MAX_VERIFY_ATTEMPTS,
  normalizeEmail,
  verifyOtpHash,
} from "@/lib/server/otp";
import { getSessionCookieOptions, signSessionToken } from "@/lib/server/jwt";
import { validateCsrf } from "@/lib/server/csrf";

export async function POST(request: Request) {
  if (!validateCsrf(request)) {
    return NextResponse.json({ error: "Invalid request origin (CSRF protection)." }, { status: 403 });
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON request body." }, { status: 400 });
  }

  const emailRaw = typeof body === "object" && body !== null && "email" in body ? String(body.email) : "";
  const codeRaw = typeof body === "object" && body !== null && "code" in body ? String(body.code).trim() : "";
  const email = normalizeEmail(emailRaw);

  if (!isValidEmail(email)) {
    return NextResponse.json({ error: "Please enter a valid email address." }, { status: 400 });
  }

  if (!/^\d{6}$/.test(codeRaw)) {
    return NextResponse.json({ error: "Please enter a valid 6-digit code." }, { status: 400 });
  }

  // 1. Locate the latest active, unconsumed challenge for this email
  const challenge = await prisma.otpChallenge.findFirst({
    where: {
      email,
      consumedAt: null,
    },
    orderBy: { createdAt: "desc" },
  });

  if (!challenge) {
    return NextResponse.json(
      { error: "No active verification code found. Please request a new code." },
      { status: 400 }
    );
  }

  // 2. Check if challenge has exceeded maximum allowed failed attempts
  if (challenge.attempts >= MAX_VERIFY_ATTEMPTS) {
    return NextResponse.json(
      { error: "Maximum verification attempts exceeded. Please request a new code." },
      { status: 400 }
    );
  }

  // 3. Check OTP expiration (5 minutes)
  if (challenge.expiresAt < new Date()) {
    return NextResponse.json(
      { error: "Verification code has expired. Please request a new code." },
      { status: 400 }
    );
  }

  // 4. Secure constant-time hash comparison
  const isMatch = verifyOtpHash(email, codeRaw, challenge.codeHash);

  if (!isMatch) {
    // Record failed attempt to prevent brute force
    const updated = await prisma.otpChallenge.update({
      where: { id: challenge.id },
      data: { attempts: { increment: 1 } },
    });

    if (updated.attempts >= MAX_VERIFY_ATTEMPTS) {
      return NextResponse.json(
        { error: "Maximum verification attempts exceeded. Please request a new code." },
        { status: 400 }
      );
    }

    return NextResponse.json(
      { error: "Invalid verification code. Please check and try again." },
      { status: 400 }
    );
  }

  // 5. Concurrency protection: atomically consume challenge so concurrent requests cannot race
  const consumeResult = await prisma.otpChallenge.updateMany({
    where: {
      id: challenge.id,
      consumedAt: null,
      attempts: { lt: MAX_VERIFY_ATTEMPTS },
    },
    data: {
      consumedAt: new Date(),
    },
  });

  if (consumeResult.count === 0) {
    return NextResponse.json(
      { error: "This verification code has already been used. Please request a new code." },
      { status: 400 }
    );
  }

  // 6. Find or create user only AFTER successful OTP verification
  const user = await prisma.user.upsert({
    where: { email },
    update: {},
    create: { email },
  });

  // 7. Issue genuinely signed JWT session token
  const token = await signSessionToken({
    sub: user.id,
    email: user.email,
  });

  // 8. Set HttpOnly, SameSite=Lax, Secure (prod) cookie
  const cookieOptions = getSessionCookieOptions();
  const response = NextResponse.json({
    success: true,
    user: {
      id: user.id,
      email: user.email,
      createdAt: user.createdAt.toISOString(),
    },
  });

  response.cookies.set(cookieOptions.name, token, cookieOptions);
  return response;
}
