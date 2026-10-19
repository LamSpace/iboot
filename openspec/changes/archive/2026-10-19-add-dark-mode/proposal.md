# Proposal

## Why

`iboot-portal` currently ships only a light theme. Users working in low-light environments have no way to reduce glare, and the portal ignores the operating system's color-scheme preference. The design-token layer introduced by `portal-design-system` already centralizes every color, so a dark theme can be added without per-page rewrites. The source design analysis does not publish a dark palette, which is why this needs its own derived token set.

## What Changes

- Derive a dark palette from the existing semantic `--ib-*` tokens (canvas, surface, hairline, text hierarchy, brand accents, semantic feedback) and add `html.dark` overrides in `src/styles/tokens.css`.
- Add Element Plus dark-variable mapping in `src/styles/element.css` so buttons, inputs, tables, tags, dialogs, dropdowns, and pagination follow the theme.
- Add a `useTheme` composable supporting three modes—`light`, `dark`, and `system`—with `localStorage` persistence and a `prefers-color-scheme` listener.
- Apply the theme before the application mounts (bootstrap script or pre-mount class) so the first paint does not flash the wrong theme.
- Add a theme selector to the authenticated shell and to the Home / Login / Register pages.
- Theme all first-party surfaces: shell, list pages, forms, dialogs, dashboards, charts, empty/loading/error states.
- Keep embedded monitor pages (Druid, Swagger, SBA, Prometheus, Grafana, Kibana, Alertmanager, Thanos) on the shared dark wrapper only; third-party page internals are out of scope.
- Extend the UI token audit to reject hardcoded colors in both light and dark blocks, add contrast checks, and capture dark-mode visual baselines.
- No **BREAKING** changes: APIs, routes, permissions, and business logic are untouched.

## Capabilities

### New Capabilities
<!-- None: dark mode extends the existing design-system capability. -->

### Modified Capabilities
- `portal-design-system`: add theme-mode, persistence, dark token coverage, FOUC, contrast, and iframe-boundary requirements.

## Impact

- Frontend code: `src/styles/tokens.css`, `src/styles/element.css`, `src/main.ts`, `index.html`, a new `useTheme` composable, the application shell, auth/landing pages, shared components, and any view with custom surfaces.
- Localization: theme selector labels in `zh-CN` and `en-US`.
- No backend, API, database, Flyway, permission, or `sys_menu` changes.
- Third-party iframe internals remain unchanged; only their surrounding container follows the theme.
- Verification: dark-mode screenshots, contrast audit, the existing 165-check responsive/i18n audit, and the build/lint/type-check/token checks.
