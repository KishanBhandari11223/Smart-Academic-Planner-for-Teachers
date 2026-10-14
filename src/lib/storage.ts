import type { AppData } from "@/types";
import { initialData } from "@/data/seed";

const STORAGE_KEY = "adaptive-teaching-planner:v1";
const AUTH_KEY = "adaptive-teaching-planner:demo-auth";

export function loadAppData(): AppData {
  if (typeof window === "undefined") return initialData;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return initialData;
    const parsed: unknown = JSON.parse(raw);
    if (
      typeof parsed === "object" &&
      parsed !== null &&
      "version" in parsed &&
      parsed.version === 1 &&
      "subjects" in parsed &&
      Array.isArray(parsed.subjects) &&
      "sessions" in parsed &&
      Array.isArray(parsed.sessions) &&
      "outcomes" in parsed &&
      Array.isArray(parsed.outcomes)
    ) {
      return parsed as AppData;
    }
  } catch {
    // A corrupt or outdated prototype record should never block the demo.
  }
  return initialData;
}

export function saveAppData(data: AppData): void {
  if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function loadDemoAuth(): boolean {
  if (typeof window === "undefined") return false;
  return window.localStorage.getItem(AUTH_KEY) === "signed-in";
}

export function saveDemoAuth(signedIn: boolean): void {
  if (typeof window === "undefined") return;
  if (signedIn) window.localStorage.setItem(AUTH_KEY, "signed-in");
  else window.localStorage.removeItem(AUTH_KEY);
}
