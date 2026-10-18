# Design

## Context

See `proposal.md` — Why for motivation and `specs/portal-design-system/spec.md` for the behavior contract.

Current frontend facts that shape the approach:

- 34 routes / 34 views, 44 Vue SFCs; all 44 carry scoped styles.
- 27 of 44 files hardcode Element Plus palette values; 8 files use decorative gradients.
- 21 views use `el-table`, 15 use the inline `search-form`, 20 share a `content-wrapper` structure.
- The authenticated shell is `views/DashboardView.vue`: `el-container` + 200px dark sidebar (`#545c64`, active `#ffd04b`) + header with breadcrumb.
- Navigation is data-driven: `menuList = convertMenuTree(permissionStore.menuTree)`, ultimately from `sys_menu`. Route paths and permission codes must not change.
- 8 monitor pages are iframe hosts; their internal UIs are third-party and not styleable.
- Element Plus is 2.6.x; dark css-vars are imported in `main.ts` but no theme toggle exists.
- No chart library is present; dashboard visuals are custom. Three.js is used only by Home / Login / Register for background effects.
- No frontend test suite; verification is `vue-tsc`, lint, build, and manual/screenshot review.

## Goals / Non-Goals

**Goals:**

- One token source that controls the whole portal and maps cleanly onto Element Plus CSS variables.
- A coherent shell and five page archetypes that new pages can reuse without bespoke styling.
- A repeatable migration path that keeps each phase shippable and reviewable.
- Visual baselines and automated checks that prevent hardcoded styling from returning.

**Non-Goals:**

- Rebuilding business logic, routes, permission wiring, or the `sys_menu` tree.
- Upgrading Element Plus or replacing it.
- Dark mode, mobile-first redesign, command palette, or multi-tab navigation.
- Restyling the content inside third-party iframe pages.

## Decisions

### D1. Keep Element Plus and skin it through CSS variables plus a contained override layer

Element Plus already provides `--el-*` variables and covers the table/form/dialog surface required by the admin. Replacing it would rewrite 202 buttons and 190 table columns for no functional gain.

- `src/styles/tokens.css` — raw and semantic tokens (`--ib-*`).
- `src/styles/element.css` — maps semantic tokens to `--el-*` and contains the few `:deep`-style component overrides.
- `src/styles/global.css` — reset, base typography, utilities.
- Import order in `main.ts`: Element Plus CSS → tokens → Element overrides → global styles.

Alternatives considered: upgrade to a newer Element Plus (rejected — unrelated risk), switch to a headless UI kit (rejected — cost far exceeds visual benefit).

### D2. Monochrome UI with brand colors reserved for data and identity moments

Primary actions, navigation, and surfaces use ink/canvas/hairline tokens. Coral, magenta, blue, and purple are used only for charts, KPI accents, module identity, or status where meaning is explicit. This follows the source design language, which forbids using product colors for general buttons or body text.

Element Plus semantic colors are mapped to accessible equivalents; warning/danger remain distinguishable from the brand accents.

### D3. Pair the brand typeface for Latin with a CJK system fallback, self-hosted

DM Sans covers Latin and numerals; Chinese falls back to `"Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif`. Only the Latin subset of DM Sans is self-hosted as woff2; shipping a full CJK webfont is rejected for payload and licensing complexity.

The admin type scale is a subset of the source document: 12 / 13 / 14 / 16 / 18 / 20 / 24 / 32 / 40px. The 56–80px display sizes are reserved for Home / Login / Register.

### D4. Radius and elevation follow semantic roles, not a single global value

- Buttons: pill (`9999px`).
- Inputs and compact controls: 8px.
- Standard cards and panels: 16px.
- Auth/landing and dashboard highlight surfaces: 24–32px.
- Default surfaces are flat with 1px hairline borders; shadows are retained only for dialogs, dropdowns, popovers, and sticky panels.

Element Plus's `--el-border-radius-base` is not enough for pill buttons; the override layer targets button selectors directly and defines a compact action variant for table toolbars.

### D5. Rework the shell without touching navigation logic

`DashboardView.vue` keeps `permissionStore.menuTree`, route handling, and permission checks unchanged. Only its template classes, sidebar surface, typography, collapse behavior, and responsive drawer are reworked. Breadcrumb behavior stays route-driven.

- Desktop sidebar: light canvas/surface, active item uses surface + medium weight.
- Collapse: retained via `el-menu` collapse.
- < 1024px: sidebar becomes an overlay drawer controlled by shell state.
- Content: fluid width with a 24px gutter; page header owns title and page-level actions.

### D6. Introduce a small set of shared components rather than a generic framework

Concrete components, each replacing a repeated pattern found in the views:

- `PageHeader` — title, optional description, action slot.
- `FilterBar` — inline search/filter layout and reset/submit placement.
- `TableToolbar` — primary/secondary actions and density.
- `EmptyState` — icon, message, optional action.
- `MetricCard` — KPI label/value/trend/accent.
- `IframePanel` — loading, failure, and reload handling for monitor pages.
- `StatusTag` — dict-driven status rendering.

Global state, business API calls, and table data handling stay in the existing views/stores. No state-management or data-fetching abstraction is introduced.

### D7. Page archetypes define migration, not new routes

- **List/CRUD**: page header → filter bar → toolbar → table → pagination; forms stay in dialogs/drawers.
- **Dashboard**: KPI row → trend/visualization → health/status → shortcuts.
- **Auth/landing**: editorial black-on-white hero, restrained motion.
- **Embedded monitor**: page header → `IframePanel` only.
- **Detail/form**: page header → sectioned form → sticky action bar where needed.

The Three.js background used by Home / Login / Register conflicts with the source design's stark editorial language. Decision: remove the canvas effect and decorative gradients from these pages; keep a subtle CSS-based motion only if it does not compete with typography. If no other usage remains, the `three` dependency is removed.

### D8. Verify through baselines and a hard-coded-color gate

- Capture screenshot baselines for the 10 Ring 2 pages at 1440/1280/1024/768 before editing.
- Add a grep-based check and lint guidance that fails when business views contain Element Plus palette hex values.
- Run `cnpm run build`, `cnpm run lint`, and `vue-tsc --noEmit` after each phase.

## Risks / Trade-offs

- **Element Plus 2.6 internal selectors may resist CSS-variable overrides** → keep all `:deep` selectors in `element.css`, document each one, and verify the affected component before moving on.
- **Dense admin layouts conflict with marketing spacing** → define compact variants for toolbars and tables; never apply landing-page spacing to authenticated pages.
- **CJK fallback may look different across platforms** → prefer system CJK fonts, test on Linux/macOS/Windows, and avoid relying on exact glyph metrics.
- **Brand colors can be overused** → codify the usage rule in the token comments and review each page against it.
- **No automated visual tests** → screenshot baselines plus phased review; a Playwright smoke suite is a later option, not a prerequisite.
- **27 files hardcode colors** → the grep gate plus token migration in Ring 3 prevents silent regressions.
- **Iframe pages cannot be themed** → accept external styling as-is and limit work to the wrapper and failure states.
- **Removing Three.js changes the landing pages noticeably** → review Home / Login / Register with the user before finalizing; the alternative is a monochrome, subdued canvas effect.

## Migration Plan

1. Baseline: capture screenshots and inventory hardcoded styles.
2. Foundation: tokens, fonts, Element Plus mapping; no page-level edits yet.
3. Shell: sidebar, header, breadcrumb, content container, responsive drawer.
4. Shared components and archetypes; migrate one representative page per archetype for review.
5. Ring 2 deep pages, then Ring 3 cleanup, then Ring 4 wrappers.
6. QA: four breakpoints, zh/en, core flows, screenshot comparison, quality commands.

Rollback is per phase: token and component phases are additive, shell and page edits are isolated to their files, and no database or API changes are involved.

## Open Questions

- Exact chart color assignment (which brand color maps to which metric) can be decided during Ring 2 without changing the approach.
- Whether to add a global command search or multi-tab navigation is deferred to a separate change.
