## ADDED Requirements

### Requirement: Vue 3 project with TypeScript and Vite
The system SHALL initialize a Vue 3 project with TypeScript and Vite as the build tool.

#### Scenario: Project starts successfully
- **WHEN** developer runs `npm run dev` in the frontend directory
- **THEN** the dev server starts on `http://localhost:5173` with hot module replacement

### Requirement: Tailwind CSS with design system tokens
The system SHALL use Tailwind CSS with custom theme tokens matching the design system: colors (surface, primary, secondary, error), fonts (Manrope for headlines, Inter for body), and rounded corners.

#### Scenario: Design tokens are available in templates
- **WHEN** developer uses classes like `bg-surface`, `text-primary`, `font-headline` in Vue templates
- **THEN** the corresponding design system values are applied

### Requirement: Sidebar navigation
The system SHALL display a fixed left sidebar with two navigation items: "面试演练室" and "简历调优台". The active item SHALL be visually highlighted.

#### Scenario: Navigate between pages
- **WHEN** user clicks "简历调优台" in the sidebar
- **THEN** the main content area displays the Resume Optimizer page and the sidebar item is highlighted

#### Scenario: Default route
- **WHEN** user opens the app without a specific path
- **THEN** the app redirects to `/interview` (面试演练室)

### Requirement: Vue Router configuration
The system SHALL configure Vue Router with routes: `/interview` → MockInterviewRoomView, `/resume` → ResumeOptimizerView, `/` → redirect to `/interview`.

#### Scenario: Direct URL access
- **WHEN** user navigates to `http://localhost:5173/resume`
- **THEN** the Resume Optimizer page loads directly

### Requirement: Responsive layout shell
The system SHALL use a layout with fixed sidebar (w-64) and scrollable main content area. On narrow screens, the sidebar SHALL collapse to icons only.

#### Scenario: Desktop layout
- **WHEN** viewport width is 1024px or wider
- **THEN** sidebar shows full text labels and main content fills remaining width
