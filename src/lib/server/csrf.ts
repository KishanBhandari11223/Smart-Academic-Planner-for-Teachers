import "server-only";

/**
 * Validates state-changing requests to protect against Cross-Site Request Forgery (CSRF).
 * Checks the Origin/Referer header against the request's host to verify same-origin invocation.
 */
export function validateCsrf(request: Request): boolean {
  // Safe methods do not modify state
  if (["GET", "HEAD", "OPTIONS"].includes(request.method)) {
    return true;
  }

  const origin = request.headers.get("origin");
  const host = request.headers.get("host") || request.headers.get("x-forwarded-host");

  if (!origin) {
    // Fall back to Referer header if Origin is omitted
    const referer = request.headers.get("referer");
    if (!referer) {
      // For automated non-browser clients or tests without origin/referer,
      // allow if content-type is application/json (cannot be forged by cross-site forms without preflight)
      const contentType = request.headers.get("content-type") || "";
      return contentType.includes("application/json");
    }
    try {
      const refererUrl = new URL(referer);
      return !host || refererUrl.host === host;
    } catch {
      return false;
    }
  }

  try {
    const originUrl = new URL(origin);
    // Allow matching host
    if (host && originUrl.host === host) {
      return true;
    }
    return false;
  } catch {
    return false;
  }
}
