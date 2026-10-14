import { NextResponse } from "next/server";
import { prisma } from "@/lib/server/prisma";
import { SESSION_COOKIE_NAME, verifySessionToken } from "@/lib/server/jwt";

function extractSessionCookie(request: Request): string | null {
  const cookieHeader = request.headers.get("cookie");
  if (!cookieHeader) return null;

  const cookies = cookieHeader.split(";").map((item) => item.trim());
  for (const item of cookies) {
    if (item.startsWith(`${SESSION_COOKIE_NAME}=`)) {
      return decodeURIComponent(item.substring(`${SESSION_COOKIE_NAME}=`.length));
    }
  }
  return null;
}

export async function GET(request: Request) {
  const token = extractSessionCookie(request);

  if (!token) {
    return NextResponse.json({ error: "Authentication required." }, { status: 401 });
  }

  const payload = await verifySessionToken(token);
  if (!payload) {
    return NextResponse.json({ error: "Invalid or expired session." }, { status: 401 });
  }

  // Look up user by verified subject ID to ensure user still exists in the database
  const user = await prisma.user.findUnique({
    where: { id: payload.sub },
    select: {
      id: true,
      email: true,
      createdAt: true,
    },
  });

  if (!user) {
    return NextResponse.json({ error: "User not found." }, { status: 401 });
  }

  return NextResponse.json({
    user: {
      id: user.id,
      email: user.email,
      createdAt: user.createdAt.toISOString(),
    },
  });
}
