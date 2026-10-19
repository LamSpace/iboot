# Design

## Context

See `proposal.md` — Why for motivation and `specs/portal-design-system/spec.md` for the behavior contract.

The frontend already has the foundation this change needs:

- `src/styles/tokens.css` defines every semantic color as a `--ib-*` custom property under `:root`.
- `src/styles/element.css` maps those tokens to Element Plus `--el-*` variables.
- `src/main.ts` already imports `element-plus/theme-chalk/dark/css-vars.css`.
- Business views and shared components are token-based; `scripts/check-ui-tokens.mjs` rejects hardcoded palette values and decorative gradients.
- The shell header already hosts `LocalePicker`, giving the theme selector a natural placement.
- Embedded monitor pages use `IframePanel`; their internal UIs are third-party and not styleable.

The source design analysis does not publish a dark palette, so the dark values must be derived from the light semantic tokens and checked for contrast.

## Goals / Non-Goals

**Goals:**

- A three-mode theme system (light / dark / system) with persistence and live system-following.
- Dark values for every semantic token and Element Plus control.
- No first-paint flash and no per-page theme rewrites.
- Measurable contrast and visual baselines for both themes.

**Non-Goals:**

- Theming the internals of Grafana, Kibana, Prometheus, Thanos, Swagger, Druid, Alertmanager, or SBA.
- Storing theme preference on the backend or syncing it across devices.
- Introducing a second brand palette or per-module dark identities.
- Repainting images or illustrations; any future assets must supply their own dark variants.

## Decisions

### D1. Derive dark values from semantic tokens, not from a global invert filter

A CSS `filter: invert()` would also invert charts, images, and third-party iframes. Instead, add a `html.dark` block in `tokens.css` that overrides the semantic tokens only. Business views keep using the same token names and require no conditional styles.

Representative mapping:

| Token | Light | Dark |
|---|---|---|
| `--ib-color-canvas` | `#ffffff` | `#0a0a0a` |
| `--ib-color-surface` | `#f7f8fa` | `#141414` |
| `--ib-color-surface-soft` | `#f2f3f5` | `#1c1c1c` |
| `--ib-color-hairline` | `#e5e7eb` | `#2a2a2a` |
| `--ib-color-hairline-soft` | `#eaecf0` | `#232323` |
| `--ib-color-ink` | `#0a0a0a` | `#f5f5f5` |
| `--ib-color-charcoal` | `#222222` | `#d4d4d4` |
| `--ib-color-steel` | `#5f5f5f` | `#a3a3a3` |
| `--ib-color-muted` | `#a8aab2` | `#6f6f6f` |
| `--ib-color-primary` | `#0a0a0a` | `#f5f5f5` |
| `--ib-color-on-primary` | `#ffffff` | `#0a0a0a` |
| `--ib-color-brand-coral` | `#ff5530` | `#ff6b4a` |
| `--ib-color-brand-magenta` | `#ea5ec1` | `#f472d0` |
| `--ib-color-brand-blue` | `#1456f0` | `#4d8dff` |
| `--ib-color-brand-purple` | `#a855f7` | `#b57bff` |
| `--ib-color-success-text` | `#1ba673` | `#4ade80` |
| `--ib-color-warning-text` | `#b45309` | `#fbbf24` |
| `--ib-color-danger-text` | `#d45656` | `#f87171` |

The dark primary action becomes a light pill on dark surfaces (the inverse of the light theme's black pill). Shadows are reduced; hierarchy relies on surface steps and hairlines.

### D2. Apply the theme with `html.dark` and map Element Plus in `element.css`

Element Plus's dark css-vars activate under `html.dark`. The change adds:

- `html.dark { --ib-*: ... }` overrides in `tokens.css`.
- `html.dark { --el-*: ... }` overrides in `element.css` for text, surfaces, borders, primary/semantic colors, and overlays.
- `color-scheme: dark` on the root so native controls and scrollbars match.

Business views do not receive theme-specific selectors.

### D3. A small `useTheme` composable owns mode, persistence, and system listening

`src/composables/useTheme.ts` exposes:

- `mode`: `light | dark | system`
- `resolvedTheme`: `light | dark`
- `setMode(value)`

Rules:

- Storage key `theme`, consistent with the existing `locale` key.
- `system` mode listens to `window.matchMedia("(prefers-color-scheme: dark)")` and updates when the OS preference changes.
- Any stored value other than the three modes falls back to `system`.
- Applying a theme toggles `html.dark` and sets `document.documentElement.style.colorScheme`.

Alternatives considered: a Pinia store (overkill for one value) and per-component media queries (cannot support an explicit override).

### D4. Apply the theme before mount to avoid a first-paint flash

A tiny inline script in `index.html` reads `localStorage.theme` and the media query, then adds the `dark` class before the app bundle executes. It duplicates only the resolution rule, not the whole composable. `useTheme` reuses the same rule and keeps both in sync.

Loading a theme stylesheet after paint or waiting for Vue to mount is rejected because it would flash the light theme on dark-preferring devices.

### D5. Put a theme selector in the shell and on the public pages

- Authenticated shell: a circular icon button next to `LocalePicker`; clicking opens a three-item menu (Light / Dark / System).
- Home / Login / Register: the same control in the top-right area.
- Labels live in `common.json` for `zh-CN` and `en-US`.

A cycling icon-only button was rejected because users cannot see which mode is active; the dropdown makes the three states explicit.

### D6. Reuse tokens for charts, tables, dialogs, and feedback states

- Bars, progress indicators, KPI accents, tags, and empty/loading/error states already consume tokens; dark mode only changes the token values.
- Element Plus tables, forms, dialogs, dropdowns, and pagination follow the `--el-*` dark mapping.
- Any surface found with a literal light-only color is migrated to a token; the audit gate stays in place.

### D7. Theme the embedded monitor wrapper only

`IframePanel` and monitor page wrappers use tokens, so they switch automatically. The iframe document remains untouched. Some third-party tools expose their own dark setting (for example Grafana and Kibana); the portal can only document that, not toggle it.

### D8. Verify with contrast checks, dark screenshots, and the existing audits

- Add a contrast check for the token pairs used by body text, secondary text, buttons, focus rings, and semantic badges (WCAG AA: 4.5:1 for normal text, 3:1 for large text and UI boundaries).
- Capture dark-mode baselines for the key pages at 1440/1280/1024/768.
- Re-run the 165-check responsive/i18n audit with the dark theme seeded.
- Keep `check:ui`, `cnpm run build`, `cnpm exec vue-tsc --noEmit`, and `cnpm run lint` green.

## Risks / Trade-offs

- **Brand colors lose contrast on dark surfaces** → brighten dark variants and verify each pair with the contrast check.
- **Shadows become invisible** → use surface steps and hairline borders for hierarchy; keep shadows only for overlays.
- **Element Plus 2.6 dark coverage may have gaps** → verify each control family and add contained overrides in `element.css`.
- **First-paint flash** → inline bootstrap script plus `color-scheme`; verify with a cold load in dark mode.
- **Third-party iframe internals stay light** → accept and document; only the wrapper follows the theme.
- **Light-only images or illustrations** → audit assets; future assets must provide dark variants.
- **Dark palette drift** → keep dark values colocated with light tokens and make the contrast check part of the quality gate.
- **Audit scripts need theme control** → seed `localStorage.theme` in the test setup before navigation.

## Migration Plan

1. Add dark token overrides and Element Plus dark mapping; verify a representative table/form/dialog page.
2. Add `useTheme`, the pre-mount bootstrap, and the theme selector.
3. Audit the shell, shared components, and all first-party pages for light-only surfaces; replace any literal colors with tokens.
4. Verify charts, monitor wrappers, and feedback states in dark mode.
5. Add contrast checks and dark screenshot baselines; re-run the responsive/i18n audit.

Rollback is a single toggle: removing the `html.dark` overrides and the selector returns the portal to light-only behavior.

## Open Questions

- Exact dark values for chart series can be tuned during implementation without changing the approach.
- Backend-synced theme preference is deliberately deferred to a future change.
