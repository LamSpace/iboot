# Spec Delta

## ADDED Requirements

### Requirement: Theme modes

The portal SHALL offer three theme modes—light, dark, and system—and the system mode SHALL follow the operating system's color-scheme preference, updating when that preference changes.

#### Scenario: Switch to dark mode

- **WHEN** the user selects dark mode from the theme selector
- **THEN** the authenticated shell and the current page render with the dark palette without a reload

#### Scenario: Follow the system preference

- **WHEN** the user selects system mode and the operating system switches between light and dark
- **THEN** the portal follows the new preference without requiring a manual theme change

### Requirement: Theme persistence

The portal SHALL persist a user's explicit light or dark choice across reloads and SHALL fall back to system mode when no valid stored choice exists.

#### Scenario: Choice survives reload

- **WHEN** the user selects dark mode and reloads the application
- **THEN** the application starts in dark mode

#### Scenario: Invalid stored value

- **WHEN** the stored theme value is missing or not one of light, dark, or system
- **THEN** the portal uses system mode

### Requirement: Dark token coverage

Every semantic color token used by first-party surfaces SHALL have a dark value, and business views and shared components SHALL consume tokens rather than hardcoded colors. Element Plus controls SHALL follow the dark theme.

#### Scenario: Token audit in both themes

- **WHEN** the UI token audit scans views and components
- **THEN** it reports no hardcoded palette values or decorative gradients outside the token definitions

#### Scenario: Element Plus controls

- **WHEN** the portal is in dark mode and a page renders a table, form, dialog, dropdown, tag, or pagination control
- **THEN** those controls use the dark surface, text, and border tokens

### Requirement: No first-paint theme flash

The effective theme SHALL be applied before the application renders so the first paint never shows the opposite theme.

#### Scenario: Cold load in dark mode

- **WHEN** a user with a stored dark preference loads the application
- **THEN** the first rendered frame is dark and no light flash occurs

### Requirement: Contrast and readability

Body text, secondary text, interactive controls, focus indicators, and semantic badges SHALL meet WCAG AA contrast in both light and dark themes.

#### Scenario: Dark-mode contrast check

- **WHEN** the contrast audit runs against the dark palette
- **THEN** the checked foreground/background pairs meet the WCAG AA threshold

### Requirement: Embedded monitor boundary

Embedded monitor pages SHALL render inside the shared dark wrapper, while third-party iframe internals remain outside the portal's theming scope.

#### Scenario: Dark monitor wrapper

- **WHEN** a monitor page is opened in dark mode
- **THEN** its page wrapper and failure state use the dark palette and the iframe content is not modified by the portal

### Requirement: Dark-mode quality gates

The responsive/i18n audit and the build, lint, type-check, and token audits SHALL pass in dark mode, and key pages SHALL have dark-mode visual baselines.

#### Scenario: Dark-mode audit

- **WHEN** the responsive and i18n audit runs with dark mode enabled
- **THEN** no page-level overflow or clipping is introduced by the dark palette

#### Scenario: Dark-mode baseline

- **WHEN** the key pages are captured in dark mode
- **THEN** a baseline set exists for regression comparison
