#!/usr/bin/env node
/**
 * Guards the design-system contract: business views must not hardcode
 * Element Plus palette values or decorative gradients.
 *
 * Usage: node scripts/check-ui-tokens.mjs [root]
 * Default root: src/views
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
    const lines = source.split("\n");
    lines.forEach((line, index) => {
      const lower = line.toLowerCase();
      const color = PALETTE.find((value) => lower.includes(value));
      if (color) {
        violations.push({
          file: relative(process.cwd(), path),
          line: index + 1,
          rule: `palette ${color}`,
        });
      }
      if (lower.includes("linear-gradient")) {
        violations.push({
          file: relative(process.cwd(), path),
          line: index + 1,
          rule: "decorative gradient",
        });
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
