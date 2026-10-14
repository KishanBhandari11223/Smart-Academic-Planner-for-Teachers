import { defineConfig } from "vitest/config";
import path from "node:path";

export default defineConfig({
  test: {
    environment: "node",
    globals: true,
  },
  resolve: {
    alias: {
      "server-only": path.resolve(import.meta.dirname, "./src/lib/server/__mocks__/server-only.ts"),
      "@": path.resolve(import.meta.dirname, "./src"),
    },
  },
});
