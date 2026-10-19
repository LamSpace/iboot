# Tasks

## 1. Baseline and theme contract

- [x] 1.1 Capture light-mode baseline screenshots for the 10 key pages at 1440/1280/1024/768 and verify the image set is complete
- [x] 1.2 Inventory every `--ib-*` token and Element Plus control family used by first-party surfaces, and verify the inventory accounts for the styles in `tokens.css` and `element.css`
- [x] 1.3 Document the theme contract in `iboot-portal/docs/design-system.md` (modes, storage key, dark token table, iframe boundary), and verify every documented path and class exists

## 2. Dark token foundation

- [x] 2.1 Add `html.dark` overrides for all semantic `--ib-*` color tokens in `src/styles/tokens.css`, and verify no semantic token used by views or components remains without a dark value
- [x] 2.2 Add `html.dark` Element Plus mappings in `src/styles/element.css`, and verify a table, form, dialog, dropdown, tag, and pagination control render with dark surfaces and text
- [x] 2.3 Set `color-scheme` and dark scrollbar/native-control behavior, and verify the browser renders native controls and scrollbars in the dark scheme
- [x] 2.4 Add `scripts/check-contrast.mjs` covering body text, secondary text, buttons, focus rings, and semantic badges, and verify the reported pairs meet WCAG AA

## 3. Theme runtime

- [x] 3.1 Implement `src/composables/useTheme.ts` with `mode`, `resolvedTheme`, and `setMode`, and verify light/dark/system resolution in the browser
- [x] 3.2 Add the pre-mount theme bootstrap to `index.html`, and verify a cold load with a stored dark preference paints dark without a light flash
- [x] 3.3 Add the theme selector component and `common` locale labels, and verify the selector appears in the shell and on Home / Login / Register
- [x] 3.4 Verify persistence across reloads, invalid stored values falling back to system, and system mode reacting to an emulated `prefers-color-scheme` change

## 4. First-party surfaces

- [x] 4.1 Audit the application shell and shared components for light-only surfaces, and verify each renders correctly in dark mode
- [x] 4.2 Audit list and CRUD pages, and verify tables, forms, filters, empty states, and dialogs in dark mode
- [x] 4.3 Audit Home / Login / Register, and verify their editorial layouts and form controls in dark mode
- [x] 4.4 Audit dashboards and charts, and verify bars, progress indicators, KPI accents, and status tags in dark mode
- [x] 4.5 Verify loading, empty, error, and confirmation feedback states in dark mode

## 5. Embedded monitor pages

- [x] 5.1 Verify all 8 monitor pages render the shared dark wrapper and failure state while the third-party iframe content remains unchanged
- [x] 5.2 Document the per-tool dark-mode setting for third-party pages (Grafana, Kibana, and any other supporting tool) in `docs/design-system.md`

## 6. Verification

- [x] 6.1 Run the 165-check responsive/i18n audit with dark mode enabled and verify 0 failures
- [x] 6.2 Capture dark-mode screenshots for the key pages at 1440/1280/1024/768 and verify the baseline set is complete
- [x] 6.3 Run the contrast audit and verify every checked pair meets WCAG AA
- [x] 6.4 Run `cnpm run build`, `cnpm exec vue-tsc --noEmit`, `cnpm run lint`, and `node scripts/check-ui-tokens.mjs`, and verify all succeed
- [x] 6.5 Run the core flow in dark mode (login, dashboard navigation, CRUD, export, file upload, monitor visit) and verify behavior is unchanged
- [x] 6.6 Run the token audit again and verify no hardcoded palette values or light-only literals were introduced
