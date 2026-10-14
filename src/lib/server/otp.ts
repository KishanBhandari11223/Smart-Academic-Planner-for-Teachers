import "server-only";
import crypto from "node:crypto";
import { getEnv } from "./env";

export const OTP_EXPIRY_MINUTES = 5;
export const RESEND_COOLDOWN_SECONDS = 60;
export const MAX_VERIFY_ATTEMPTS = 5;

/**
 * Normalizes an email address (lowercase and trimmed).
 */
export function normalizeEmail(email: string): string {
  return email.trim().toLowerCase();
}

/**
 * Basic RFC 5322-compatible email validation.
 */
export function isValidEmail(email: string): boolean {
  if (!email || email.length > 254) return false;
  const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;
  return emailRegex.test(email);
}

/**
 * Generates a cryptographically secure 6-digit OTP string.
 */
export function generateOtp(): string {
  // crypto.randomInt is cryptographically strong (uses CSPRNG)
  return crypto.randomInt(100000, 1000000).toString();
}

/**
 * Hashes an OTP bound to the normalized email using HMAC-SHA256.
 * Binds the secret key and recipient email so rainbow tables and cross-account code reuse are prevented.
 */
export function hashOtp(email: string, otp: string): string {
  const env = getEnv();
  return crypto
    .createHmac("sha256", env.JWT_SECRET)
    .update(`${normalizeEmail(email)}:${otp}`)
    .digest("hex");
}

/**
 * Compares submitted OTP against stored hash using constant-time comparison to prevent timing attacks.
 */
export function verifyOtpHash(email: string, otp: string, storedHash: string): boolean {
  try {
    const computedHash = hashOtp(email, otp);
    const computedBuf = Buffer.from(computedHash, "hex");
    const storedBuf = Buffer.from(storedHash, "hex");

    if (computedBuf.length !== storedBuf.length) {
      return false;
    }
    return crypto.timingSafeEqual(computedBuf, storedBuf);
  } catch {
    return false;
  }
}
