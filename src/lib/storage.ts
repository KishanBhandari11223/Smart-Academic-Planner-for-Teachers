import type { AppData } from "@/types";

const STORAGE_KEY = "adaptive-teaching-planner:v1";

export function createEmptyAppData(): AppData {
  return { version: 1, subjects: [], sessions: [], outcomes: [], lastProposal: null };
}

function storageKeyFor(email: string): string {
  const normalizedEmail = email.trim().toLocaleLowerCase();
  if (!normalizedEmail) throw new Error("An account email is required to access saved planner data.");
  return `${STORAGE_KEY}:${encodeURIComponent(normalizedEmail)}`;
}

export function isAppData(parsed: unknown): parsed is AppData {
  return (
    typeof parsed === "object" &&
    parsed !== null &&
    "version" in parsed &&
    parsed.version === 1 &&
    "subjects" in parsed &&
    Array.isArray(parsed.subjects) &&
    "sessions" in parsed &&
    Array.isArray(parsed.sessions) &&
    "outcomes" in parsed &&
    Array.isArray(parsed.outcomes) &&
    "lastProposal" in parsed &&
    (parsed.lastProposal === null || typeof parsed.lastProposal === "object")
  );
}

export function readStoredAppData(email: string): AppData | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(storageKeyFor(email));
  if (raw === null) return null;

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error("Saved browser planner data is unreadable. It has been left untouched.");
  }
  if (!isAppData(parsed)) {
    throw new Error("Saved browser planner data has an unsupported format. It has been left untouched.");
  }
  return parsed;
}

export function loadAppData(email: string): AppData {
  if (typeof window === "undefined") return createEmptyAppData();
  try {
    return readStoredAppData(email) ?? createEmptyAppData();
  } catch {
    // A corrupt or outdated local record should not populate a different workspace.
  }
  return createEmptyAppData();
}

export function saveAppData(data: AppData, email: string): void {
  if (typeof window !== "undefined") window.localStorage.setItem(storageKeyFor(email), JSON.stringify(data));
}
