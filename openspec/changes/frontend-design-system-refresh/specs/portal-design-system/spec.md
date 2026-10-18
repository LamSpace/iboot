# Spec Delta

## Purpose

Defines how the `iboot-portal` frontend presents a single, consistently applied visual system across its application shell, page archetypes, and business views, while preserving existing navigation, permissions, and business behavior.

## ADDED Requirements

### Requirement: Unified design tokens

The portal SHALL derive colors, typography, spacing, corner radii, elevation, and motion from a single design-token source, and business views SHALL NOT hardcode Element Plus palette values or ad-hoc decorative colors.

#### Scenario: Token change propagates

- **WHEN** a color, radius, or spacing token is changed in the token source
- **THEN** all components and pages that use that token reflect the change without editing individual views

#### Scenario: Hardcoded palette audit

- **WHEN** the business view sources are scanned for Element Plus palette values such as `#409eff`, `#303133`, or `#909399`
- **THEN** no business view contains a hardcoded palette value outside the token definitions

### Requirement: Bilingual typography

The portal SHALL render Latin text and numerals with the brand typeface and Chinese text with a CJK-capable fallback, using self-hosted font assets so rendering does not depend on external font CDNs.

#### Scenario: Chinese and English rendering

- **WHEN** the user switches between `zh-CN` and `en-US`
- **THEN** headings, labels, table headers, buttons, and form text render in the intended font stack without missing glyphs, clipping, or overflow

#### Scenario: Offline font availability

- **WHEN** the application is loaded without access to external font services
- **THEN** the configured typefaces still render from locally served assets

### Requirement: Consistent component appearance

The portal SHALL present Element Plus buttons, forms, tables, tags, dialogs, and pagination through the shared token system, including a compact action variant for dense toolbars, so that default framework styling does not leak into the interface.

#### Scenario: Primary action styling

- **WHEN** a page renders its primary action button
- **THEN** the button uses the shared primary style and pill-shaped geometry

#### Scenario: Dense table toolbar

- **WHEN** a list page renders row-level or toolbar actions inside a data table
- **THEN** those actions use the compact variant and do not increase row height beyond the table's standard density

### Requirement: Unified application shell

The portal SHALL render every authenticated page inside one application shell providing a sidebar, header, breadcrumb, and content region; the sidebar SHALL be collapsible on desktop and become a drawer on narrow viewports.

#### Scenario: Sidebar collapse

- **WHEN** the user collapses the sidebar
- **THEN** the content region expands and the active menu item remains identifiable

#### Scenario: Navigation data unchanged

- **WHEN** the shell is rendered
- **THEN** the menu tree, route paths, and permission-based visibility are identical to the existing `sys_menu`-driven navigation

### Requirement: Consistent page archetypes and feedback states

List, form, dashboard, authentication, and embedded-monitor pages SHALL share defined page archetypes, and every data-driven page SHALL expose loading, empty, error, and confirmation feedback states.

#### Scenario: Empty list

- **WHEN** a data-driven page receives an empty result set
- **THEN** it displays the shared empty state rather than an unexplained blank region

#### Scenario: Embedded monitor failure

- **WHEN** an embedded monitor page cannot load its target
- **THEN** it displays a visible error or fallback state inside the shared page wrapper

### Requirement: Responsive behavior

The portal SHALL remain usable at the 1440px, 1280px, 1024px, and 768px width breakpoints, with no page-level horizontal overflow and with wide tables contained in scrollable regions.

#### Scenario: Tablet layout

- **WHEN** the viewport width is 1024px
- **THEN** the sidebar collapses to its compact form and the content region remains fully operable

#### Scenario: Wide table on narrow viewport

- **WHEN** a table page is viewed at 768px
- **THEN** the table scrolls within its container instead of overflowing the page

### Requirement: Behavior preservation

The visual refresh SHALL NOT change business logic, API contracts, route definitions, permission rules, or the `sys_menu` menu tree.

#### Scenario: Core workflow unchanged

- **WHEN** a user completes login, dashboard navigation, a CRUD operation, export, file upload, and a monitor page visit after the refresh
- **THEN** each flow produces the same observable business results as before the refresh

### Requirement: Quality gates

The portal SHALL pass type checking, linting, and production build after the refresh, and key pages SHALL have a visual baseline for regression comparison.

#### Scenario: Build verification

- **WHEN** the frontend quality commands are run
- **THEN** type checking, linting, and the production build complete successfully

#### Scenario: Visual regression baseline

- **WHEN** the key pages are captured before and after the refresh
- **THEN** each intentional visual difference is reviewed against the baseline
