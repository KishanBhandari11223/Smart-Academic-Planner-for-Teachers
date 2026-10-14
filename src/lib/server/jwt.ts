import "server-only";
import { SignJWT, jwtVerify } from "jose";
import { getEnv } from "./env";

export const SESSION_COOKIE_NAME = "auth_session";

export interface SessionPayload {
  sub: string;
  email: string;
}

/**
 * Creates and signs a JWT for the authenticated user session using `jose`.
 */
export async function signSessionToken(payload: SessionPayload): Promise<string> {
  const env = getEnv();
  const secretKey = new TextEncoder().encode(env.JWT_SECRET);

  return new SignJWT({ email: payload.email })
    .setSubject(payload.sub)
    .setProtectedHeader({ alg: "HS256" })
    .setIssuedAt()
    .setIssuer(env.JWT_ISSUER)
    .setAudience(env.JWT_AUDIENCE)
    .setExpirationTime(env.JWT_EXPIRES_IN)
    .sign(secretKey);
}

/**
 * Verifies a JWT session token against the secret, issuer, and audience.
 */
export async function verifySessionToken(token: string): Promise<SessionPayload | null> {
  try {
    const env = getEnv();
    const secretKey = new TextEncoder().encode(env.JWT_SECRET);

    const { payload } = await jwtVerify(token, secretKey, {
      issuer: env.JWT_ISSUER,
      audience: env.JWT_AUDIENCE,
    });

    if (!payload.sub || typeof payload.email !== "string") {
      return null;
    }

    return {
      sub: payload.sub,
      email: payload.email,
    };
  } catch {
    return null;
  }
}

/**
 * Standard cookie options to ensure HttpOnly, SameSite, and Secure flags are set consistently.
 */
export function getSessionCookieOptions() {
  const isProduction = process.env.NODE_ENV === "production";
  return {
    name: SESSION_COOKIE_NAME,
    httpOnly: true,
    secure: isProduction,
    sameSite: "lax" as const,
    path: "/",
    maxAge: 7 * 24 * 60 * 60, // 7 days in seconds
  };
}
