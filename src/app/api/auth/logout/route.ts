import { NextResponse } from "next/server";
import { getSessionCookieOptions } from "@/lib/server/jwt";
import { validateCsrf } from "@/lib/server/csrf";

export async function POST(request: Request) {
  if (!validateCsrf(request)) {
    return NextResponse.json({ error: "Invalid request origin (CSRF protection)." }, { status: 403 });
  }

  const cookieOptions = getSessionCookieOptions();
  const response = NextResponse.json({
    success: true,
    message: "Logged out successfully.",
  });

  // Clear session cookie using identical path, domain, sameSite, and httpOnly attributes
  response.cookies.set(cookieOptions.name, "", {
    ...cookieOptions,
    maxAge: 0,
    expires: new Date(0),
  });

  return response;
}
