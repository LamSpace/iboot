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

The check fails when a business view contains an Element Plus palette value
or a decorative `linear-gradient`. A clean fixture should pass:

```bash
node scripts/check-ui-tokens.mjs /tmp/ui-clean-fixture
```
