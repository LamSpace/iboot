#!/usr/bin/env node
/**
 * WCAG AA contrast audit for the portal token palettes.
 *
 * Usage: node scripts/check-contrast.mjs
 *
 * Reads :root (light) and html.dark from src/styles/tokens.css and checks the
 * foreground/background pairs used by body text, secondary text, buttons,
 * focus rings, and semantic badges.
 */
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const tokensCss = readFileSync(resolve("src/styles/tokens.css"), "utf8");
const [lightBlock, darkBlock] = tokensCss.split("html.dark");

function parseTokens(block) {
  return Object.fromEntries(
    [...block.matchAll(/(--ib-[a-z0-9-]+):\s*(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3});/g)].map(
      (match) => [match[1], match[2]],
    ),
  );
}

function channel(value) {
  const normalized = value / 255;
  return normalized <= 0.03928
    ? normalized / 12.92
    : ((normalized + 0.055) / 1.055) ** 2.4;
}

function luminance(hex) {
  const value = hex.replace("#", "");
  const full =
    value.length === 3
      ? value
          .split("")
          .map((part) => part + part)
          .join("")
      : value;
  const [r, g, b] = [0, 2, 4].map((offset) =>
    parseInt(full.slice(offset, offset + 2), 16),
  );
  return (
    0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
  );
}

function contrast(foreground, background) {
  const a = luminance(foreground);
  const b = luminance(background);
  const [high, low] = a > b ? [a, b] : [b, a];
  return (high + 0.05) / (low + 0.05);
}

const PAIRS = [
  // Body and secondary text (4.5:1 for normal text)
  ["Body text on canvas", "--ib-color-ink", "--ib-color-canvas", 4.5],
  ["Body text on surface", "--ib-color-ink", "--ib-color-surface", 4.5],
  ["Body text on soft surface", "--ib-color-ink", "--ib-color-surface-soft", 4.5],
  ["Regular text on canvas", "--ib-color-charcoal", "--ib-color-canvas", 4.5],
  ["Secondary text on canvas", "--ib-color-steel", "--ib-color-canvas", 4.5],
  ["Secondary text on surface", "--ib-color-steel", "--ib-color-surface", 4.5],
  ["Secondary text on soft surface", "--ib-color-steel", "--ib-color-surface-soft", 4.5],
  ["Slate text on canvas", "--ib-color-slate", "--ib-color-canvas", 4.5],
  ["Info text on canvas", "--ib-color-info-text", "--ib-color-canvas", 4.5],
  ["Placeholder on canvas", "--ib-color-muted", "--ib-color-canvas", 3.0],
  ["Placeholder on surface", "--ib-color-muted", "--ib-color-surface", 3.0],

  // Primary button label on its fill
  ["Primary button label", "--ib-color-on-primary", "--ib-color-primary", 4.5],

  // Focus ring against page surfaces (3:1 for UI boundaries)
  ["Focus ring on canvas", "--ib-color-brand-blue-deep", "--ib-color-canvas", 3.0],
  ["Focus ring on surface", "--ib-color-brand-blue-deep", "--ib-color-surface", 3.0],

  // Semantic badges
  ["Success badge", "--ib-color-success-text", "--ib-color-success-bg", 4.5],
  ["Warning badge", "--ib-color-warning-text", "--ib-color-warning-bg", 4.5],
  ["Danger badge", "--ib-color-danger-text", "--ib-color-danger-bg", 4.5],
];

const palettes = [
  { name: "light", tokens: parseTokens(lightBlock) },
  { name: "dark", tokens: parseTokens(darkBlock) },
];

let failures = 0;
for (const palette of palettes) {
  for (const [label, foreground, background, minimum] of PAIRS) {
    const fg = palette.tokens[foreground];
    const bg = palette.tokens[background];
    if (!fg || !bg) {
      console.error(`${palette.name}: missing token ${!fg ? foreground : background}`);
      failures += 1;
      continue;
    }
    const ratio = contrast(fg, bg);
    const pass = ratio >= minimum;
    if (!pass) failures += 1;
    console.log(
      `${pass ? "PASS" : "FAIL"}  ${palette.name.padEnd(5)}  ${ratio
        .toFixed(2)
        .padStart(5)}:1  (min ${minimum.toFixed(1)})  ${label}`,
    );
  }
}

if (failures > 0) {
  console.error(`\nContrast check failed: ${failures} pair(s)`);
  process.exit(1);
}

console.log("\nContrast check passed.");
