import "server-only";

/**
 * Validated server-side environment variables.
 * Sensitive values are never exposed to the client.
 */
export function getEnv() {
  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) {
    throw new Error("DATABASE_URL environment variable is missing.");
  }

  const jwtSecret = process.env.JWT_SECRET || "development-default-jwt-secret-min-32-chars-long";
  if (process.env.NODE_ENV === "production" && jwtSecret.length < 32) {
    throw new Error("JWT_SECRET must be at least 32 characters long in production.");
  }

  return {
    DATABASE_URL: databaseUrl,
    RESEND_API_KEY: process.env.RESEND_API_KEY || "",
    EMAIL_FROM: process.env.EMAIL_FROM || "Teaching Planner <onboarding@resend.dev>",
    JWT_SECRET: jwtSecret,
    JWT_ISSUER: process.env.JWT_ISSUER || "adaptive-teaching-planner",
    JWT_AUDIENCE: process.env.JWT_AUDIENCE || "adaptive-teaching-planner-faculty",
    JWT_EXPIRES_IN: process.env.JWT_EXPIRES_IN || "7d",
    NODE_ENV: process.env.NODE_ENV || "development",
    APP_URL: process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000",
  };
}
