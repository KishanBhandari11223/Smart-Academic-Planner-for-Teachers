import { beforeEach, describe, expect, it, vi } from "vitest";
import type { AppData } from "@/types";

describe("API CSRF handling", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("fetches a CSRF token and sends it with state-changing requests", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-1" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const { apiFetch } = await import("@/lib/api");

    await apiFetch("/api/auth/logout", { method: "POST" });

    expect(fetchMock).toHaveBeenNthCalledWith(1, "/api/auth/csrf", { credentials: "include" });
    const requestOptions = fetchMock.mock.calls[1]?.[1] as RequestInit;
    expect(new Headers(requestOptions.headers).get("X-XSRF-TOKEN")).toBe("csrf-1");
    expect(requestOptions.credentials).toBe("include");
  });

  it("refreshes the token after a CSRF rejection rather than reusing a stale token", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-old" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ error: "CSRF token missing or invalid." }), { status: 403 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-new" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const { apiFetch } = await import("@/lib/api");

    await apiFetch("/api/auth/logout", { method: "POST" });
    await apiFetch("/api/auth/logout", { method: "POST" });

    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get("X-XSRF-TOKEN")).toBe("csrf-old");
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/auth/csrf", { credentials: "include" });
    expect(new Headers(fetchMock.mock.calls[3]?.[1]?.headers).get("X-XSRF-TOKEN")).toBe("csrf-new");
  });

  it("refreshes a cached token when the browser no longer has its CSRF cookie", async () => {
    const browser = { cookie: "" };
    vi.stubGlobal("document", browser);
    const fetchMock = vi.fn()
      .mockImplementationOnce(async () => {
        browser.cookie = "ATP-XSRF-TOKEN=csrf-old";
        return new Response(JSON.stringify({ token: "csrf-old" }), { status: 200 });
      })
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockImplementationOnce(async () => {
        browser.cookie = "ATP-XSRF-TOKEN=csrf-new";
        return new Response(JSON.stringify({ token: "csrf-new" }), { status: 200 });
      })
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const { apiFetch } = await import("@/lib/api");

    await apiFetch("/api/auth/logout", { method: "POST" });
    browser.cookie = "";
    await apiFetch("/api/auth/logout", { method: "POST" });

    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/auth/csrf", { credentials: "include" });
    expect(new Headers(fetchMock.mock.calls[3]?.[1]?.headers).get("X-XSRF-TOKEN")).toBe("csrf-new");
  });

  it("does not send an unsafe request when the CSRF cookie was not retained", async () => {
    vi.stubGlobal("document", { cookie: "" });
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ token: "csrf-without-cookie" }), { status: 200 }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const { apiFetch } = await import("@/lib/api");

    await expect(apiFetch("/api/auth/logout", { method: "POST" })).rejects.toThrow(
      "The browser did not retain the request security cookie.",
    );
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it("does not fetch or send CSRF tokens for safe requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const { apiFetch } = await import("@/lib/api");

    await apiFetch("/api/auth/me");

    expect(fetchMock).toHaveBeenCalledOnce();
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: "include" });
  });
});

describe("planner workspace API", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
  });

  it("saves the workspace with its expected version and CSRF header", async () => {
    const data: AppData = { version: 1, subjects: [], sessions: [], outcomes: [], lastProposal: null };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-2" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ version: 4, data }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const { savePlannerWorkspace } = await import("@/lib/api");

    await expect(savePlannerWorkspace(data, 3)).resolves.toBe(4);

    const request = fetchMock.mock.calls[1]?.[1] as RequestInit;
    expect(fetchMock.mock.calls[1]?.[0]).toBe("/api/planner/workspace");
    expect(request.method).toBe("PUT");
    expect(new Headers(request.headers).get("X-XSRF-TOKEN")).toBe("csrf-2");
    expect(JSON.parse(String(request.body))).toEqual({ version: 3, data });
  });

  it("surfaces version conflicts instead of treating them as successful saves", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ token: "csrf-3" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ error: "Reload before saving." }), { status: 409 }));
    vi.stubGlobal("fetch", fetchMock);
    const { savePlannerWorkspace } = await import("@/lib/api");

    await expect(savePlannerWorkspace(
      { version: 1, subjects: [], sessions: [], outcomes: [], lastProposal: null },
      2,
    )).rejects.toThrow("Reload before saving.");
  });
});

describe("API error responses", () => {
  it("explains a plain-text CORS rejection without attempting to treat it as JSON", async () => {
    const { responseError } = await import("@/lib/api");
    const response = new Response("Invalid CORS request", { status: 403 });

    await expect(responseError(response, "Request failed.")).resolves.toBe(
      "The sign-in request was blocked by the backend CORS policy. Check that the frontend address is allowed.",
    );
    await expect(response.text()).resolves.toBe("Invalid CORS request");
  });
});
