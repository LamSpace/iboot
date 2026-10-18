# Tasks

## 1. Baseline and inventory

- [x] 1.1 Capture baseline screenshots for the 10 Ring 2 pages at 1440/1280/1024/768 and verify the image set is complete before any edit
- [x] 1.2 Inventory hardcoded palette values, decorative gradients, and scoped-style outliers across all 34 views, and verify the report accounts for the 27 palette files and 8 gradient files
- [x] 1.3 Record the navigation baseline (route paths, menu keys, permission codes) and verify the recorded set can be diffed after the shell work

## 2. Design tokens and typography

- [x] 2.1 Create `src/styles/tokens.css` with color, typography, spacing, radius, elevation, and motion tokens, and verify a sample page consumes them through computed styles
- [x] 2.2 Self-host the DM Sans Latin subset and add the CJK fallback stack, and verify the app renders with external font services blocked
- [x] 2.3 Map tokens to Element Plus variables in `src/styles/element.css`, and verify primary button, input focus, table header, tag, and dialog states use the new tokens
- [x] 2.4 Add the compact action variant for table toolbars and verify row height does not grow on a representative list page
- [x] 2.5 Flatten standard cards and panels to hairline borders and retain shadows only for dialogs, dropdowns, popovers, and sticky panels, and verify each surface visually
- [x] 2.6 Add a hardcoded-palette audit script for business views, and verify it reports current violations and passes against a clean fixture
- [x] 2.7 Write `iboot-portal/docs/design-system.md` covering token names, component usage, and brand-color rules, and verify every documented path and class exists

## 3. Application shell

- [x] 3.1 Rework the sidebar, header, and content container in `DashboardView.vue` to the token system, and verify the default 1440px layout
- [x] 3.2 Implement desktop collapse and the sub-1024px overlay drawer, and verify both states at 1024px and 768px
- [x] 3.3 Integrate the route-driven breadcrumb and page-header region, and verify breadcrumb text updates on navigation
- [x] 3.4 Diff the navigation baseline and verify menu tree, route paths, and permission-based visibility are unchanged

## 4. Shared components and page archetypes

- [x] 4.1 Implement `PageHeader` and verify it renders title, description, and actions on at least one page
- [x] 4.2 Implement `FilterBar` and `TableToolbar` and verify a list page renders search/reset and page-level actions with consistent spacing
- [x] 4.3 Implement `EmptyState` plus shared loading and error presentation, and verify an empty response renders the empty state instead of a blank region
- [x] 4.4 Implement `MetricCard` and verify a dashboard renders label, value, trend, and accent
- [x] 4.5 Implement `IframePanel` with loading, failure, and reload states, and verify a monitor page shows a visible failure state when the target is unavailable
- [x] 4.6 Implement `StatusTag` for dict-driven statuses and verify existing status columns render equivalently
- [x] 4.7 Migrate `UserView.vue` to the list/CRUD archetype as the reference implementation, and verify search, create, edit, reset password, delete, status change, and export still work
- [x] 4.8 Extend `docs/design-system.md` with the five archetypes and verify the documented structure matches `UserView` and the shared components

## 5. Ring 2 core pages

- [x] 5.1 Redesign `HomeView.vue` with the editorial hero and remove the Three.js canvas and decorative gradients, and verify the landing page at four breakpoints in both locales
- [x] 5.2 Redesign `LoginView.vue` and verify login succeeds with the existing credentials flow
- [x] 5.3 Redesign `RegisterView.vue` and verify registration validation and submission still behave as before
- [x] 5.4 Restructure `DashboardHomeView.vue` into KPI, trend, health, and shortcut sections, and verify all data sources and loading states still render
- [x] 5.5 Migrate `RoleView.vue` to the CRUD archetype and verify role CRUD and permission assignment
- [x] 5.6 Rework `FileManageView.vue` and verify upload, download, preview, and category operations
- [x] 5.7 Rework `UsageView.vue` and verify its data presentation in both locales
- [x] 5.8 Rework `ReportView.vue` and verify export and filtering behavior
- [x] 5.9 Rework `MessageCenterView.vue` and verify message list, read state, and SSE updates
- [x] 5.10 Remove the `three` dependency if no usage remains and verify the production build succeeds

## 6. Ring 3 migration and cleanup

- [x] 6.1 Migrate the remaining system, organization, message, security, and analysis list pages to the shared archetype, and verify each page's primary CRUD or query flow
- [x] 6.2 Migrate the tree and specialized pages (`MenuView`, `DeptView`, `OrgChartView`) without changing their data operations, and verify tree interactions
- [x] 6.3 Remove remaining hardcoded palette values and decorative gradients from business views, and verify the audit script reports zero violations
- [x] 6.4 Audit all pages in `zh-CN` and `en-US` for clipping and overflow, and verify no text is truncated outside intentional ellipsis
- [x] 6.5 Audit all 34 views at 1440/1280/1024/768, and verify no page-level horizontal overflow and wide tables scroll within their containers

## 7. Ring 4 embedded monitor wrappers

- [x] 7.1 Move the 8 iframe monitor pages onto `IframePanel`, and verify loading, reload, and failure states for each
- [x] 7.2 Verify no iframe-internal page (Grafana, Kibana, Prometheus, Thanos, Swagger, Druid, Alertmanager, Server) was modified beyond its wrapper

## 8. Integration verification

- [x] 8.1 Run the core flow end to end (login, dashboard navigation, user CRUD, export, file upload, monitor visit) and verify behavior matches the baseline
- [x] 8.2 Run `cnpm run lint`, `cnpm run build`, and `cnpm exec vue-tsc --noEmit` in `iboot-portal`, and verify all succeed
- [x] 8.3 Compare the final screenshots against the baselines for all Ring 2 pages and verify each difference is intentional
- [x] 8.4 Verify the final route/menu/permission diff against the navigation baseline is empty
