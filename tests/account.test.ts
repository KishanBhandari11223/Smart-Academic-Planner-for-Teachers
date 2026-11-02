import { describe, expect, it } from "vitest";
import { accountDisplayName, accountInitials, parseAuthenticatedUser } from "@/lib/account";

describe("authenticated account identity", () => {
  it("uses the account response as the source for display identity", () => {
    const user = parseAuthenticatedUser({
      user: { id: "teacher-1", email: "kishan@gehu.test" },
    });

    expect(user.email).toBe("kishan@gehu.test");
    expect(accountDisplayName(user.email)).toBe("Kishan");
    expect(accountInitials(accountDisplayName(user.email))).toBe("K");
  });

  it("formats separated email names without inventing a stored full name", () => {
    expect(accountDisplayName("kartikey.joshi@gehu.test")).toBe("Kartikey Joshi");
    expect(accountInitials(accountDisplayName("kartikey.joshi@gehu.test"))).toBe("KJ");
  });

  it("rejects authentication responses without an account identity", () => {
    expect(() => parseAuthenticatedUser({ user: { email: "kishan@gehu.test" } })).toThrow(
      "The server returned an invalid sign-in response.",
    );
  });
});
