import { afterEach, describe, expect, it, vi } from "vitest";

describe("browser planner-data migration", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.resetModules();
  });

  it("reports when no previous browser workspace exists", async () => {
    vi.stubGlobal("window", {
      localStorage: { getItem: vi.fn().mockReturnValue(null) },
    });
    const { readStoredAppData } = await import("@/lib/storage");

    expect(readStoredAppData("kishan@gehu.test")).toBeNull();
  });

  it("refuses to migrate unreadable data instead of silently replacing it with sample data", async () => {
    vi.stubGlobal("window", {
      localStorage: { getItem: vi.fn().mockReturnValue("{invalid-json") },
    });
    const { readStoredAppData } = await import("@/lib/storage");

    expect(() => readStoredAppData("kishan@gehu.test")).toThrow("Saved browser planner data is unreadable.");
  });

  it("refuses to migrate unsupported data formats", async () => {
    vi.stubGlobal("window", {
      localStorage: { getItem: vi.fn().mockReturnValue(JSON.stringify({ version: 2 })) },
    });
    const { readStoredAppData } = await import("@/lib/storage");

    expect(() => readStoredAppData("kishan@gehu.test")).toThrow("Saved browser planner data has an unsupported format.");
  });

  it("keeps local recovery data scoped to its account email", async () => {
    const stored = new Map<string, string>();
    vi.stubGlobal("window", {
      localStorage: {
        getItem: vi.fn((key: string) => stored.get(key) ?? null),
        setItem: vi.fn((key: string, value: string) => stored.set(key, value)),
      },
    });
    const { readStoredAppData, saveAppData } = await import("@/lib/storage");
    const data = { version: 1, subjects: [], sessions: [], outcomes: [], lastProposal: null };
    stored.set("adaptive-teaching-planner:v1", JSON.stringify(data));

    expect(readStoredAppData("kishan@gehu.test")).toBeNull();

    saveAppData(data, "Kishan@gehu.test");

    expect(readStoredAppData("kishan@gehu.test")).toEqual(data);
    expect(readStoredAppData("kartikey@gehu.test")).toBeNull();
  });

  it("creates an empty planner workspace when a new account has no recovery copy", async () => {
    const { createEmptyAppData, readStoredAppData } = await import("@/lib/storage");
    const accountEmail = "banana@gehu.test";

    const data = readStoredAppData(accountEmail) ?? createEmptyAppData();

    expect(data).toEqual({ version: 1, subjects: [], sessions: [], outcomes: [], lastProposal: null });
  });
});
