import type { AppData } from "@/types";

let csrfToken: string | null = null;

function getCsrfCookieToken(): string | null {
  if (typeof document === "undefined") return null;

  const cookie = document.cookie
    .split(";")
    .map((part) => part.trim())
    .find((part) => part.startsWith("ATP-XSRF-TOKEN="));
  if (!cookie) return null;

  try {
    return decodeURIComponent(cookie.slice("ATP-XSRF-TOKEN=".length));
  } catch {
    throw new Error("The browser returned an invalid request security cookie.");
  }
}

async function getCsrfToken(): Promise<string> {
  if (typeof document === "undefined" && csrfToken) return csrfToken;

  const cookieToken = getCsrfCookieToken();
  if (cookieToken) {
    csrfToken = cookieToken;
    return cookieToken;
  }

  const response = await fetch("/api/auth/csrf", { credentials: "include" });
  if (!response.ok) {
    throw new Error("Could not initialize request security. Refresh the page and try again.");
  }

  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("token" in body) ||
    typeof body.token !== "string" ||
    body.token.length === 0
  ) {
    throw new Error("The server returned an invalid request security token.");
  }

  const responseToken = body.token;
  if (typeof document !== "undefined" && getCsrfCookieToken() !== responseToken) {
    throw new Error("The browser did not retain the request security cookie. Refresh the page and try again.");
  }

  csrfToken = responseToken;
  return csrfToken;
}

export async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const method = (init.method ?? "GET").toUpperCase();
  const requiresCsrf = !["GET", "HEAD", "OPTIONS"].includes(method);
  const headers = new Headers(init.headers);

  if (requiresCsrf) {
    headers.set("X-XSRF-TOKEN", await getCsrfToken());
  }

  const response = await fetch(input, {
    ...init,
    credentials: "include",
    headers,
  });

  if (requiresCsrf && response.status === 403) {
    csrfToken = null;
  }

  return response;
}

export async function fetchPlannerWorkspace(): Promise<Response> {
  return apiFetch("/api/planner/workspace");
}

export async function savePlannerWorkspace(
  data: AppData,
  version: number | null,
): Promise<number> {
  const response = await apiFetch("/api/planner/workspace", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ version, data }),
  });
  if (!response.ok) {
    const body: unknown = await response.json();
    const message = typeof body === "object" && body !== null && "error" in body && typeof body.error === "string"
      ? body.error
      : `Could not save planner data (${response.status}).`;
    throw new Error(message);
  }

  const body: unknown = await response.json();
  if (
    typeof body !== "object" ||
    body === null ||
    !("version" in body) ||
    typeof body.version !== "number" ||
    !Number.isInteger(body.version)
  ) {
    throw new Error("The server returned an invalid planner workspace version.");
  }
  return body.version;
}

export async function responseError(response: Response, fallback: string): Promise<string> {
  try {
    const text = await response.clone().text();
    try {
      const body: unknown = JSON.parse(text);
      if (typeof body === "object" && body !== null && "error" in body && typeof body.error === "string") {
        return body.error;
      }
    } catch {
      if (text.trim() === "Invalid CORS request") {
        return "The sign-in request was blocked by the backend CORS policy. Check that the frontend address is allowed.";
      }
    }
  } catch {
    return fallback;
  }
  return fallback;
}
