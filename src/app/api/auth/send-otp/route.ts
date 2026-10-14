import { NextResponse } from "next/server";
import { prisma } from "@/lib/server/prisma";
import {
  generateOtp,
  hashOtp,
  isValidEmail,
  normalizeEmail,
  OTP_EXPIRY_MINUTES,
  RESEND_COOLDOWN_SECONDS,
} from "@/lib/server/otp";
import { sendOtpEmail } from "@/lib/server/email";
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
  const email = normalizeEmail(emailRaw);

  if (!isValidEmail(email)) {
    return NextResponse.json({ error: "Please enter a valid email address." }, { status: 400 });
  }

  // 1. Resend cooldown: enforce wait period between OTP requests for the same email
  const cooldownThreshold = new Date(Date.now() - RESEND_COOLDOWN_SECONDS * 1000);
  const recentChallenge = await prisma.otpChallenge.findFirst({
    where: {
      email,
      createdAt: { gte: cooldownThreshold },
    },
    orderBy: { createdAt: "desc" },
  });

  if (recentChallenge) {
    return NextResponse.json(
      { error: `Please wait ${RESEND_COOLDOWN_SECONDS} seconds before requesting another code.` },
      { status: 429 }
    );
  }

  // 2. Window rate limit: max 5 requests per email in 15 minutes
  const windowThreshold = new Date(Date.now() - 15 * 60 * 1000);
  const recentCount = await prisma.otpChallenge.count({
    where: {
      email,
      createdAt: { gte: windowThreshold },
    },
  });

  if (recentCount >= 5) {
    return NextResponse.json(
      { error: "Too many OTP requests. Please try again later." },
      { status: 429 }
    );
  }

  // 3. Invalidate any existing unused challenges for this email
  await prisma.otpChallenge.updateMany({
    where: {
      email,
      consumedAt: null,
    },
    data: {
      consumedAt: new Date(),
    },
  });

  // 4. Generate cryptographically secure OTP and store its HMAC hash
  const otp = generateOtp();
  const codeHash = hashOtp(email, otp);
  const expiresAt = new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000);

  await prisma.otpChallenge.create({
    data: {
      email,
      codeHash,
      expiresAt,
    },
  });

  // 5. Deliver OTP via Resend from server-side code
  const delivery = await sendOtpEmail(email, otp);
  if (!delivery.success) {
    return NextResponse.json(
      { error: "Could not send verification code. Please try again shortly." },
      { status: 500 }
    );
  }

  // Generic message returned to reduce account enumeration; OTP is never returned
  return NextResponse.json({
    success: true,
    message: "If the email is valid, a verification code has been sent.",
  });
}
