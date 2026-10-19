# iBoot Portal Design System

The portal uses a single token source derived from the MiniMax design analysis.
Business views must consume tokens instead of hardcoded palette values.

## Files

| File | Purpose |
|---|---|
| `src/styles/fonts.css` | Self-hosted DM Sans Latin subset and fallback stack |
| `src/styles/tokens.css` | Raw and semantic `--ib-*` tokens |
| `src/styles/element.css` | Element Plus variable mapping and component skin |
| `src/styles/global.css` | Reset, base typography, utilities |
| `src/assets/fonts/dm-sans-latin.woff2` | Self-hosted brand typeface |
| `scripts/check-ui-tokens.mjs` | Audit for hardcoded palette values and gradients |
| `scripts/check-contrast.mjs` | WCAG AA contrast audit for token pairs |
| `scripts/ui-capture.mjs` | Responsive screenshot capture through CDP |

## Theme modes

The portal supports three theme modes:

| Mode | Behavior |
|---|---|
| `light` | Always use the light palette |
| `dark` | Always use the dark palette |
| `system` | Follow `prefers-color-scheme` and update when the OS preference changes |

The explicit choice is stored in `localStorage` under the key `theme`. A
missing or invalid value falls back to `system`. `useTheme` (in
`src/composables/useTheme.ts`) owns resolution and persistence; a small inline
script in `index.html` applies the same rule before the app mounts so the first
paint never flashes the wrong theme.

The resolved theme is applied as `html.dark` plus
`document.documentElement.style.colorScheme`. Element Plus's dark css-vars are
already imported in `src/main.ts`; `element.css` remaps the `--el-*` variables
to `--ib-*` tokens in both themes.

Theme selection is available in the authenticated shell header (next to the
locale picker) and in the top-right area of Home, Login, and Register. Labels
live in `common.json` under `theme.*`.

### Dark token values

Dark values are derived from the semantic tokens; business views never receive
theme-specific selectors.

| Token | Light | Dark |
|---|---|---|
| `--ib-color-canvas` | `#ffffff` | `#0a0a0a` |
| `--ib-color-surface` | `#f7f8fa` | `#141414` |
| `--ib-color-surface-soft` | `#f2f3f5` | `#1c1c1c` |
| `--ib-color-hairline` | `#e5e7eb` | `#2a2a2a` |
| `--ib-color-hairline-soft` | `#eaecf0` | `#232323` |
| `--ib-color-overlay` | `rgba(10, 10, 10, 0.32)` | `rgba(10, 10, 10, 0.32)` |
| `--ib-color-code-bg` | `#1e1e1e` | `#1e1e1e` |
| `--ib-color-code-text` | `#d4d4d4` | `#d4d4d4` |
| `--ib-color-ink` | `#0a0a0a` | `#f5f5f5` |
| `--ib-color-ink-strong` | `#000000` | `#ffffff` |
| `--ib-color-charcoal` | `#222222` | `#d4d4d4` |
| `--ib-color-slate` | `#45515e` | `#b8c2cc` |
| `--ib-color-steel` | `#5f5f5f` | `#a3a3a3` |
| `--ib-color-stone` | `#8e8e93` | `#8a8a8f` |
| `--ib-color-muted` | `#8e8e93` | `#828282` |
| `--ib-color-primary` | `#0a0a0a` | `#f5f5f5` |
| `--ib-color-on-primary` | `#ffffff` | `#0a0a0a` |
| `--ib-color-primary-pressed` | `#222222` | `#d4d4d4` |
| `--ib-color-primary-disabled` | `#e5e7eb` | `#2a2a2a` |
| `--ib-color-disabled-text` | `#a8aab2` | `#828282` |
| `--ib-color-brand-coral` | `#ff5530` | `#ff6b4a` |
| `--ib-color-brand-magenta` | `#ea5ec1` | `#f472d0` |
| `--ib-color-brand-blue` | `#1456f0` | `#4d8dff` |
| `--ib-color-brand-blue-mid` | `#3b82f6` | `#60a5fa` |
| `--ib-color-brand-blue-deep` | `#1d4ed8` | `#3b82f6` |
| `--ib-color-brand-cyan` | `#3daeff` | `#67c3ff` |
| `--ib-color-brand-blue-200` | `#bfdbfe` | `#1e3a8a` |
| `--ib-color-brand-purple` | `#a855f7` | `#b57bff` |
| `--ib-color-on-brand` | `#ffffff` | `#0a0a0a` |
| `--ib-color-success-bg` | `#e8ffea` | `#10291a` |
| `--ib-color-success-text` | `#0f7a54` | `#4ade80` |
| `--ib-color-warning-bg` | `#fff7e6` | `#2b1f0b` |
| `--ib-color-warning-text` | `#b45309` | `#fbbf24` |
| `--ib-color-danger-bg` | `#fef0f0` | `#2c1414` |
| `--ib-color-danger-text` | `#c53a3a` | `#f87171` |
| `--ib-color-info-text` | `#5f5f5f` | `#a3a3a3` |

Dark mode inverts the primary action (light pill on dark surfaces) and reduces
shadows; hierarchy relies on surface steps and hairlines. Every value above is
checked by `scripts/check-contrast.mjs`.

### Embedded monitor boundary

Embedded pages (`Druid`, `Swagger`, `Spring Boot Admin`, `Prometheus`,
`Grafana`, `Kibana`, `Alertmanager`, `Thanos`) use the shared page wrapper and
`IframePanel`. The wrapper, header, and loading/failure states follow the
theme; the iframe document is third-party and is never restyled by the portal.

Some third-party tools expose their own dark setting:

| Tool | Where to set it |
|---|---|
| Grafana | User preferences → UI theme → Dark |
| Kibana | Stack Management → Advanced Settings → Theme → Dark |
| Spring Boot Admin | Follows the browser `prefers-color-scheme` only when the deployed version supports it; otherwise stays light |
| Prometheus / Alertmanager / Thanos / Druid / Swagger | No built-in dark theme; content stays light |

The portal does not toggle these settings.

Import order in `src/main.ts`: Element Plus CSS → fonts → tokens → Element
skin → global styles.

## Typography

- Latin and numerals: `DM Sans` (self-hosted, variable weights 400–700).
- Chinese: `Noto Sans SC`, `PingFang SC`, `Microsoft YaHei` system fallback.
- Base size: `--ib-text-body-sm` (14px) for admin surfaces.
- Scale: `--ib-text-micro` (12px) through `--ib-text-hero` (80px).
- Display sizes (`--ib-text-display-lg`, `--ib-text-hero`) are reserved for
  Home, Login, and Register.
- Weights: 400 regular, 500 medium, 600 semibold for headings and buttons,
  700 bold for strong inline emphasis.

## Color

Surfaces and actions are monochrome:

- `--ib-color-ink` — primary text and primary actions.
- `--ib-color-canvas` / `--ib-color-surface` / `--ib-color-surface-soft` —
  page and panel surfaces.
- `--ib-color-hairline` / `--ib-color-hairline-soft` — borders and dividers.

Brand accents are **not** general-purpose UI colors:

- `--ib-color-brand-coral`, `--ib-color-brand-magenta`,
  `--ib-color-brand-blue`, `--ib-color-brand-purple` are reserved for
  charts, KPI values, module identity, and meaningful status accents.
- Do not use brand accents for standard buttons, body text, or large
  surfaces.
- Semantic feedback uses `--ib-color-success-*`, `--ib-color-warning-*`,
  `--ib-color-danger-*`.

## Shape and elevation

- Pill actions: `--ib-radius-full` on every non-link button and tag.
- Inputs and compact controls: `--ib-radius-md` (8px).
- Standard cards and panels: `--ib-radius-xl` (16px).
- Auth and dashboard highlight surfaces: `--ib-radius-xxl`–`--ib-radius-hero`.
- Default surfaces are flat with a 1px hairline border.
- Shadows are reserved for dialogs, dropdowns, popovers, and sticky panels
  (`--ib-shadow-2`–`--ib-shadow-4`).

## Spacing and layout

- Spacing follows a 4px base with tokens `--ib-space-xxs` through
  `--ib-space-hero`.
- Sidebar width: `--ib-sidebar-width` (232px), collapsed
  `--ib-sidebar-collapsed-width` (64px).
- Header height: `--ib-header-height` (56px).
- Content gutter: `--ib-content-gutter` (24px).

## Component rules

- **Buttons**: Element Plus buttons are pill-shaped and use semibold labels.
  Use the default size for page-level actions and `.ib-btn-compact` for
  actions inside dense table toolbars. Link buttons (`.is-link`) keep no pill
  geometry and no padding.
- **Cards**: use `el-card` for standard panels; the skin removes the default
  shadow and applies a hairline border and 16px radius.
- **Tables**: header uses the surface token, rows use hairline-soft dividers,
  and body text is 13px. Wide tables must scroll inside their container.
- **Inputs**: 8px radius, hairline rest border, 2px `brand-blue-deep` focus
  ring.
- **Tags**: pill-shaped and semibold.
- **Dialogs and poppers**: elevated shadow and large radius.

## Shared components

| Component | Usage |
|---|---|
| `src/components/PageHeader.vue` | Page title, optional description, and an actions slot |
| `src/components/FilterBar.vue` | Search/filter card wrapping an inline form |
| `src/components/TableToolbar.vue` | Page-level actions above a table, with an optional actions slot |
| `src/components/EmptyState.vue` | Empty result state with an optional action |
| `src/components/MetricCard.vue` | KPI label, value, trend, and a restrained accent |
| `src/components/IframePanel.vue` | Embedded monitor wrapper with loading, failure, and reload states |
| `src/components/StatusTag.vue` | Dict-driven status pill |

## Page archetypes

### List / CRUD

`PageHeader → FilterBar → TableToolbar → table (+ EmptyState in the #empty
slot) → pagination`

Reference implementation: `src/views/system/UserView.vue`.

### Dashboard

Metric cards first, then trend/visualization, health/status, and shortcuts.

### Auth / landing

Editorial hero with restrained motion. Display sizes come from the token
scale; avoid decorative gradients and background canvases.

### Embedded monitor

`PageHeader → IframePanel`. Do not restyle the iframe content; only the
wrapper and its loading/failure states are in scope.

### Detail / form

`PageHeader → sectioned form → sticky action bar` when actions must remain
visible while scrolling.

## Brand accent usage checklist

Before adding a brand color, ask:

1. Is this a chart series, KPI accent, module identity, or meaningful status?
2. Would the interface lose meaning if the color were removed?
3. Is the same color already assigned to something else on this page?

If the answer to any of the first two is "no", use ink/slate/steel instead.

## Enforcement

Run the audit from `iboot-portal`:

```bash
cnpm run check:ui
```

The check fails when a business view contains an Element Plus palette value,
a literal hex/rgb color, a `white`/`black` value, or a decorative
`linear-gradient`. Intentional theme-neutral literals are listed in the
script's `ALLOWED` map.

```bash
cnpm run check:ui
node scripts/check-contrast.mjs
```

`scripts/ui-capture.mjs` and `scripts/ui-audit.mjs` drive headless Chromium
through CDP for screenshots and the responsive/i18n audit. Pass
`--theme light|dark` to run either theme; both scripts seed `localStorage.theme`
before navigation.
