import { describe, it, expect, beforeEach, afterAll } from "vitest";
import { prisma } from "@/lib/server/prisma";
import {
  generateOtp,
  hashOtp,
  verifyOtpHash,
  normalizeEmail,
  isValidEmail,
  MAX_VERIFY_ATTEMPTS,
} from "@/lib/server/otp";
import {
  signSessionToken,
  verifySessionToken,
  SESSION_COOKIE_NAME,
} from "@/lib/server/jwt";
import { POST as sendOtpHandler } from "@/app/api/auth/send-otp/route";
import { POST as verifyOtpHandler } from "@/app/api/auth/verify-otp/route";
import { GET as meHandler } from "@/app/api/auth/me/route";
import { POST as logoutHandler } from "@/app/api/auth/logout/route";

describe("Email OTP Authentication Test Suite", () => {
  const testEmail = "faculty.test@university.edu";

  beforeEach(async () => {
    // Clean up test data before each test
    await prisma.otpChallenge.deleteMany({
      where: { email: { in: [testEmail, "second.user@university.edu", "reuse@university.edu"] } },
    });
    await prisma.user.deleteMany({
      where: { email: { in: [testEmail, "second.user@university.edu", "reuse@university.edu"] } },
    });
  });

  afterAll(async () => {
    await prisma.otpChallenge.deleteMany({
      where: { email: { in: [testEmail, "second.user@university.edu", "reuse@university.edu"] } },
    });
    await prisma.user.deleteMany({
      where: { email: { in: [testEmail, "second.user@university.edu", "reuse@university.edu"] } },
    });
    await prisma.$disconnect();
  });

  describe("Unit: OTP Security & Hashing", () => {
    it("generates a 6-digit cryptographically secure OTP string", () => {
      const otp = generateOtp();
      expect(otp).toMatch(/^\d{6}$/);
    });

    it("correctly normalizes and validates email addresses", () => {
      expect(normalizeEmail("  FACULTY@University.EDU  ")).toBe("faculty@university.edu");
      expect(isValidEmail("faculty@university.edu")).toBe(true);
      expect(isValidEmail("invalid-email")).toBe(false);
      expect(isValidEmail("")).toBe(false);
    });

    it("verifies hash matches correct OTP and rejects incorrect OTP in constant time", () => {
      const otp = "123456";
      const wrongOtp = "654321";
      const hash = hashOtp(testEmail, otp);

      expect(verifyOtpHash(testEmail, otp, hash)).toBe(true);
      expect(verifyOtpHash(testEmail, wrongOtp, hash)).toBe(false);
      expect(verifyOtpHash("other@university.edu", otp, hash)).toBe(false);
    });
  });

  describe("Unit: JWT Verification", () => {
    it("signs and genuinely verifies a JWT session token", async () => {
      const payload = { sub: "test-user-uuid", email: testEmail };
      const token = await signSessionToken(payload);

      expect(typeof token).toBe("string");
      expect(token.split(".").length).toBe(3);

      const verified = await verifySessionToken(token);
      expect(verified).not.toBeNull();
      expect(verified?.sub).toBe("test-user-uuid");
      expect(verified?.email).toBe(testEmail);
    });

    it("rejects tampered or forged JWT tokens", async () => {
      const validToken = await signSessionToken({ sub: "u1", email: testEmail });
      const tamperedToken = validToken.slice(0, -6) + "abcdef";

      const verified = await verifySessionToken(tamperedToken);
      expect(verified).toBeNull();
    });

    it("rejects empty or malformed token strings", async () => {
      expect(await verifySessionToken("not-a-jwt")).toBeNull();
      expect(await verifySessionToken("")).toBeNull();
    });
  });

  describe("Endpoint: Unauthenticated Requests", () => {
    it("GET /api/auth/me returns 401 when no session cookie is provided", async () => {
      const req = new Request("http://localhost:3000/api/auth/me", {
        method: "GET",
      });

      const res = await meHandler(req);
      expect(res.status).toBe(401);

      const json = await res.json();
      expect(json.error).toBeDefined();
    });

    it("GET /api/auth/me returns 401 when an invalid cookie is supplied", async () => {
      const req = new Request("http://localhost:3000/api/auth/me", {
        method: "GET",
        headers: {
          cookie: `${SESSION_COOKIE_NAME}=invalid.jwt.token`,
        },
      });

      const res = await meHandler(req);
      expect(res.status).toBe(401);
    });
  });

  describe("Endpoint: POST /api/auth/send-otp", () => {
    it("validates email and creates an unconsumed challenge with 5-minute expiry without exposing OTP", async () => {
      const req = new Request("http://localhost:3000/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail }),
      });

      const res = await sendOtpHandler(req);
      expect(res.status).toBe(200);

      const body = await res.json();
      expect(body.success).toBe(true);
      expect(body.otp).toBeUndefined(); // Never returns OTP

      const challenge = await prisma.otpChallenge.findFirst({
        where: { email: testEmail },
      });
      expect(challenge).not.toBeNull();
      expect(challenge?.consumedAt).toBeNull();
      expect(challenge?.attempts).toBe(0);

      // Verify expiration is roughly 5 minutes from now
      const diffMs = challenge!.expiresAt.getTime() - challenge!.createdAt.getTime();
      expect(diffMs).toBeGreaterThanOrEqual(4.9 * 60 * 1000);
      expect(diffMs).toBeLessThanOrEqual(5.1 * 60 * 1000);
    });

    it("enforces resend cooldown limit when requested again immediately", async () => {
      // First request
      const req1 = new Request("http://localhost:3000/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail }),
      });
      const res1 = await sendOtpHandler(req1);
      expect(res1.status).toBe(200);

      // Second immediate request
      const req2 = new Request("http://localhost:3000/api/auth/send-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail }),
      });
      const res2 = await sendOtpHandler(req2);
      expect(res2.status).toBe(429);

      const body2 = await res2.json();
      expect(body2.error).toMatch(/wait/i);
    });
  });

  describe("Endpoint: POST /api/auth/verify-otp (Failure Modes)", () => {
    it("rejects invalid OTP code and increments attempt counter", async () => {
      const validCode = "654321";
      await prisma.otpChallenge.create({
        data: {
          email: testEmail,
          codeHash: hashOtp(testEmail, validCode),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000),
          attempts: 0,
        },
      });

      const req = new Request("http://localhost:3000/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, code: "000000" }),
      });

      const res = await verifyOtpHandler(req);
      expect(res.status).toBe(400);

      const body = await res.json();
      expect(body.error).toMatch(/invalid/i);

      const challenge = await prisma.otpChallenge.findFirst({
        where: { email: testEmail },
      });
      expect(challenge?.attempts).toBe(1);
    });

    it("rejects expired OTP challenge", async () => {
      const code = "112233";
      // Create an expired challenge (expired 1 minute ago)
      await prisma.otpChallenge.create({
        data: {
          email: testEmail,
          codeHash: hashOtp(testEmail, code),
          expiresAt: new Date(Date.now() - 60 * 1000),
          attempts: 0,
        },
      });

      const req = new Request("http://localhost:3000/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, code }),
      });

      const res = await verifyOtpHandler(req);
      expect(res.status).toBe(400);

      const body = await res.json();
      expect(body.error).toMatch(/expired/i);
    });

    it("enforces attempt limits and rejects after maximum failed attempts", async () => {
      const code = "889900";
      await prisma.otpChallenge.create({
        data: {
          email: testEmail,
          codeHash: hashOtp(testEmail, code),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000),
          attempts: MAX_VERIFY_ATTEMPTS, // already at max attempts
        },
      });

      const req = new Request("http://localhost:3000/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, code }),
      });

      const res = await verifyOtpHandler(req);
      expect(res.status).toBe(400);

      const body = await res.json();
      expect(body.error).toMatch(/maximum/i);
    });

    it("prevents OTP reuse once already consumed", async () => {
      const code = "556677";
      await prisma.otpChallenge.create({
        data: {
          email: testEmail,
          codeHash: hashOtp(testEmail, code),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000),
          consumedAt: new Date(), // Already used
          attempts: 0,
        },
      });

      const req = new Request("http://localhost:3000/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, code }),
      });

      const res = await verifyOtpHandler(req);
      expect(res.status).toBe(400);

      const body = await res.json();
      expect(body.error).toMatch(/no active|already been used/i);
    });
  });

  describe("End-to-End: Successful Login, Session Check, and Logout", () => {
    it("completes full authentication flow: send -> verify -> me -> logout", async () => {
      const otpCode = "246810";
      // 1. Manually insert challenge to know the secret OTP
      await prisma.otpChallenge.create({
        data: {
          email: testEmail,
          codeHash: hashOtp(testEmail, otpCode),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000),
        },
      });

      // Verify user does not exist before verification
      const userBefore = await prisma.user.findUnique({ where: { email: testEmail } });
      expect(userBefore).toBeNull();

      // 2. POST /api/auth/verify-otp
      const verifyReq = new Request("http://localhost:3000/api/auth/verify-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: testEmail, code: otpCode }),
      });

      const verifyRes = await verifyOtpHandler(verifyReq);
      expect(verifyRes.status).toBe(200);

      const verifyData = await verifyRes.json();
      expect(verifyData.success).toBe(true);
      expect(verifyData.user.email).toBe(testEmail);
      expect(verifyData.user.id).toBeDefined();

      // Verify user was created in database
      const userAfter = await prisma.user.findUnique({ where: { email: testEmail } });
      expect(userAfter).not.toBeNull();
      expect(userAfter?.id).toBe(verifyData.user.id);

      // Verify challenge was consumed
      const challengeAfter = await prisma.otpChallenge.findFirst({
        where: { email: testEmail },
      });
      expect(challengeAfter?.consumedAt).not.toBeNull();

      // Extract session cookie from Set-Cookie header
      const setCookie = verifyRes.headers.get("set-cookie");
      expect(setCookie).toBeDefined();
      expect(setCookie).toContain(SESSION_COOKIE_NAME);
      expect(setCookie?.toLowerCase()).toContain("httponly");
      expect(setCookie?.toLowerCase()).toContain("samesite=lax");

      // Extract cookie value for subsequent requests
      const match = setCookie!.match(new RegExp(`${SESSION_COOKIE_NAME}=([^;]+)`));
      const sessionToken = match ? match[1] : "";
      expect(sessionToken).toBeTruthy();

      // 3. GET /api/auth/me with session cookie
      const meReq = new Request("http://localhost:3000/api/auth/me", {
        method: "GET",
        headers: {
          cookie: `${SESSION_COOKIE_NAME}=${sessionToken}`,
        },
      });

      const meRes = await meHandler(meReq);
      expect(meRes.status).toBe(200);

      const meData = await meRes.json();
      expect(meData.user.email).toBe(testEmail);
      expect(meData.user.id).toBe(userAfter?.id);

      // 4. POST /api/auth/logout clears the session cookie
      const logoutReq = new Request("http://localhost:3000/api/auth/logout", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          cookie: `${SESSION_COOKIE_NAME}=${sessionToken}`,
        },
      });

      const logoutRes = await logoutHandler(logoutReq);
      expect(logoutRes.status).toBe(200);

      const logoutCookie = logoutRes.headers.get("set-cookie");
      expect(logoutCookie).toContain(`${SESSION_COOKIE_NAME}=;`);
      expect(logoutCookie).toContain("Max-Age=0");
    });
  });
});
