# Token and Control Inventory

Generated: 2026-10-18

## Semantic color tokens

All counts are references outside `src/styles/` (views, components, composables).

| Token | Light value | Refs |
|---|---|---:|
| `--ib-color-canvas` | `#ffffff` | 31 |
| `--ib-color-surface` | `#f7f8fa` | 27 |
| `--ib-color-surface-soft` | `#f2f3f5` | 2 |
| `--ib-color-hairline` | `#e5e7eb` | 53 |
| `--ib-color-hairline-soft` | `#eaecf0` | 19 |
| `--ib-color-ink` | `#0a0a0a` | 61 |
| `--ib-color-ink-strong` | `#000000` | 4 |
| `--ib-color-charcoal` | `#222222` | 9 |
| `--ib-color-slate` | `#45515e` | 8 |
| `--ib-color-steel` | `#5f5f5f` | 47 |
| `--ib-color-stone` | `#8e8e93` | 1 |
| `--ib-color-muted` | `#a8aab2` | 11 |
| `--ib-color-primary` | `#0a0a0a` | 10 |
| `--ib-color-on-primary` | `#ffffff` | 6 |
| `--ib-color-primary-pressed` | `#222222` | 2 |
| `--ib-color-primary-disabled` | `#e5e7eb` | 1 |
| `--ib-color-disabled-text` | `#a8aab2` | 1 |
| `--ib-color-brand-coral` | `#ff5530` | 6 |
| `--ib-color-brand-magenta` | `#ea5ec1` | 5 |
| `--ib-color-brand-blue` | `#1456f0` | 33 |
| `--ib-color-brand-blue-mid` | `#3b82f6` | 2 |
| `--ib-color-brand-blue-deep` | `#1d4ed8` | 2 |
| `--ib-color-brand-cyan` | `#3daeff` | 4 |
| `--ib-color-brand-blue-200` | `#bfdbfe` | 1 |
| `--ib-color-brand-purple` | `#a855f7` | 5 |
| `--ib-color-success-bg` | `#e8ffea` | 1 |
| `--ib-color-success-text` | `#1ba673` | 11 |
| `--ib-color-warning-bg` | `#fff7e6` | 1 |
| `--ib-color-warning-text` | `#b45309` | 6 |
| `--ib-color-danger-bg` | `#fef0f0` | 1 |
| `--ib-color-danger-text` | `#d45656` | 14 |
| `--ib-color-info-text` | `#5f5f5f` | 2 |

Every color token above is referenced by first-party source, so every one
needs a `html.dark` value.

## Non-color tokens

| Category | Count | Dark-mode handling |
|---|---:|---|
| Typography (`--ib-font-*`, `--ib-text-*`, `--ib-leading-*`, `--ib-weight-*`) | 24 | Unchanged |
| Spacing (`--ib-space-*`) | 12 | Unchanged |
| Radius (`--ib-radius-*`) | 9 | Unchanged |
| Elevation (`--ib-shadow-*`) | 4 | Reduced in dark mode |
| Motion (`--ib-duration-*`, `--ib-ease`) | 3 | Unchanged |
| Layout (`--ib-sidebar-*`, `--ib-header-height`, `--ib-content-gutter`) | 4 | Unchanged |

## Element Plus control families

Used by first-party surfaces (reference counts from `<el-*` tags):

| Family | Refs | Dark tokens required |
|---|---:|---|
| `el-button` | 203 | primary/light variants, default, text, disabled |
| `el-table` / `el-table-column` | 30 / 190 | header, row, hover, border, text |
| `el-form` / `el-form-item` | 37 / 179 | label, error, asterisk |
| `el-input` / `el-input-number` | 98 / 5 | wrapper, placeholder, disabled, focus ring |
| `el-select` / `el-option` / `el-tree-select` | 30 / 45 / 3 | wrapper, popper, selected item |
| `el-tag` | 81 | default and semantic variants |
| `el-card` | 44 | surface, header divider |
| `el-descriptions` | 15 / 94 | label and content |
| `el-dialog` / `el-drawer` | 27 / 1 | surface, overlay |
| `el-pagination` | 18 | background, active page |
| `el-radio` / `el-radio-group` / `el-radio-button` | 27 / 22 / 7 | label, checked state |
| `el-switch` | 9 | on/off track and handle |
| `el-date-picker` | 5 | panel, cell, range highlight |
| `el-tabs` / `el-tab-pane` | 2 / 5 | nav, active bar |
| `el-tooltip` / `el-popover` | 22 / — | surface, arrow |
| `el-dropdown` / `el-dropdown-item` | 2 / 4 | popper, hover |
| `el-badge` | 3 | background |
| `el-tree` | 2 | node hover/selected |
| `el-alert` | 2 | semantic surfaces |
| `el-empty` / `el-result` / `el-skeleton` | 4 / 1 / 1 | illustration, text |
| `el-upload` | 1 | dragger surface |
| `el-breadcrumb` | 1 | separator, link |
| `el-divider` | 6 | border |
| `el-progress` | 12 | track |
| `el-menu` (shell) | 1 | item, active, sub-menu |
| `el-link` | 6 | default/hover |

Element Plus's own `dark/css-vars.css` is already imported in `src/main.ts`.
`element.css` must still remap the tokens it overrides so the dark values stay
consistent with `--ib-*`.

## Hardcoded values outside the token layer

The palette-only audit (`scripts/check-ui-tokens.mjs`) does not catch literal
light surfaces such as `#fff`, `#fafafa`, `rgba(0, 0, 0, …)` shadows, or
`#f0f9eb`/`#eef0f0`-style tints. The dark-mode audit (tasks 4.x and 6.6) must
migrate these to tokens; the list below is the working inventory.

| File | Values |
|---|---|
| `src/views/HomeView.vue` | `rgba(255, 255, 255, 0.9)` sticky header |
| `src/views/DashboardView.vue` | `rgba(10, 10, 10, 0.32)` drawer overlay |
| `src/views/monitor/*.vue` (8 files) | `#fff` wrapper, `rgba(0,0,0,.08)` shadow |
| `src/views/monitor/RedisMonitorView.vue` | `#fff`, `rgba(0,0,0,.08)` |
| `src/views/monitor/MinioMonitorView.vue` | icon colors `#00C853`, `#FF9800`, `#2196F3` |
| `src/views/monitor/PerformanceView.vue` | `#fff`, `rgba(0,0,0,.08)` |
| `src/views/security/AuditLogView.vue` | `#1e1e1e` / `#d4d4d4` JSON viewer |
| `src/views/organization/OrgChartView.vue` | `white`, `#e8e8e8`, `#fafafa`, `#ecf5ff` |
| `src/components/SliderCaptcha.vue` | `#666`, `#fff`, `#f0f9eb`, `#fef0f0`, `#f0f0f0` |
| `src/components/MessagePopup.vue` | `#f0f9ff`, `#f0f0f0` |
| `src/components/NoticePopup.vue` | `#f0f0f0` |
| `src/components/Notification/NotificationBell.vue` | `#fff`, `#ecf5ff`, `#e1f3d8`, `#fafafa` |
| `src/components/Notification/NotificationList.vue` | `#fff`, `#ecf5ff`, `#d9ecff` |
| `src/components/OrgTreeNode.vue` | `white`, `#f5f5f5`, `#f0f0f0`, `#fef0f0`, `rgba(64,158,255,…)` |
| `src/components/HelloWorld.vue` | `#42b983` (unused scaffolding component) |
