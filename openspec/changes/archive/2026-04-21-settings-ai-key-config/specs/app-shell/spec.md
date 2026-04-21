## MODIFIED Requirements

### Requirement: Sidebar navigation items
The system SHALL provide a fixed left sidebar with navigation entries: "模拟面试" (`/interview`), "简历优化" (`/resume`), and "设置" (`/settings`).

#### Scenario: Navigate to settings page
- **WHEN** user clicks "设置" in the sidebar
- **THEN** the route changes to `/settings` and the settings view is displayed with the correct active state indicator

## ADDED Requirements

### Requirement: Settings route registration
The system SHALL register a `/settings` route that renders the SettingsView component within AppLayout.

#### Scenario: Direct URL access to settings
- **WHEN** user navigates directly to `http://localhost:5173/settings`
- **THEN** the settings page loads correctly within the app layout with sidebar
