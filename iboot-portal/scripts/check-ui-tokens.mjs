#!/usr/bin/env node
/**
 * Guards the design-system contract: business views must not hardcode
 * Element Plus palette values, literal colors, or decorative gradients.
 * Literal colors belong in the token layer and would break dark mode.
 *
 * Usage: node scripts/check-ui-tokens.mjs [root]
 * Default root: src/views and src/components
 */
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

const PALETTE = [
  "#409eff",
  "#303133",
  "#909399",
  "#f56c6c",
  "#67c23a",
  "#e6a23c",
  "#606266",
  "#f5f7fa",
  "#ebeef5",
  "#dcdfe6",
  "#c0c4cc",
];

// Unused Vue scaffolding retained as-is; all active views consume tokens.
const ALLOWED = {
  "src/components/HelloWorld.vue": ["#42b983"],
};

const roots = process.argv.slice(2).length
  ? process.argv.slice(2).map((value) => resolve(value))
  : [resolve("src/views"), resolve("src/components")];
const violations = [];

function walk(dir) {
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      walk(path);
      continue;
    }
    if (!name.endsWith(".vue")) continue;

    const source = readFileSync(path, "utf8");
    const file = relative(process.cwd(), path);
    const allowed = ALLOWED[file] ?? [];
    const lines = source.split("\n");
    lines.forEach((line, index) => {
      const lower = line.toLowerCase();
      const report = (rule) =>
        violations.push({ file, line: index + 1, rule });

      const color = PALETTE.find((value) => lower.includes(value));
      if (color) {
        report(`palette ${color}`);
      }
      if (lower.includes("linear-gradient")) {
        report("decorative gradient");
      }

      const literals = [
        ...line.matchAll(/#[0-9a-fA-F]{3,8}\b/g),
        ...line.matchAll(/rgba?\([^)]*\)/g),
        ...line.matchAll(/:\s*(white|black)\b/g),
      ].map((match) => match[0]);
      for (const literal of literals) {
        const normalized = literal.trim();
        if (allowed.some((value) => normalized.includes(value))) continue;
        // The palette rule above already reports these values.
        if (PALETTE.some((value) => normalized.toLowerCase() === value)) continue;
        report(`literal ${normalized}`);
      }
    });
  }
}

for (const root of roots) walk(root);

if (violations.length > 0) {
  console.error(`UI token check failed: ${violations.length} violation(s)`);
  for (const violation of violations) {
    console.error(`  ${violation.file}:${violation.line}  ${violation.rule}`);
  }
  process.exit(1);
}

console.log("UI token check passed.");
