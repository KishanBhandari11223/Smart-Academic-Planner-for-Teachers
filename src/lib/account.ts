export interface AuthenticatedUser {
  id: string;
  email: string;
}

export function parseAuthenticatedUser(value: unknown): AuthenticatedUser {
  const user = typeof value === "object" && value !== null && "user" in value
    ? value.user
    : null;
  if (
    typeof user !== "object" ||
    user === null ||
    !("id" in user) ||
    typeof user.id !== "string" ||
    !user.id.trim() ||
    !("email" in user) ||
    typeof user.email !== "string" ||
    !user.email.trim()
  ) {
    throw new Error("The server returned an invalid sign-in response.");
  }
  return { id: user.id, email: user.email.trim() };
}

export function accountDisplayName(email: string): string {
  const localPart = email.split("@", 1)[0] ?? "";
  const words = localPart.split(/[._+-]+/).filter(Boolean);
  if (words.length === 0) return email;
  return words.map((word) => word[0]?.toLocaleUpperCase() + word.slice(1)).join(" ");
}

export function accountInitials(displayName: string): string {
  return displayName
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0]?.toLocaleUpperCase() ?? "")
    .join("");
}
