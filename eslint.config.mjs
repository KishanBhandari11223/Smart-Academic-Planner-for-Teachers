import js from "@eslint/js";
import { defineConfig, globalIgnores } from "eslint/config";
import tseslint from "typescript-eslint";

const eslintConfig = defineConfig([
  js.configs.recommended,
  ...tseslint.configs.recommended,
  { rules: { "no-undef": "off" } },
  globalIgnores(["out/**", "build/**", "dist/**"]),
]);

export default eslintConfig;
