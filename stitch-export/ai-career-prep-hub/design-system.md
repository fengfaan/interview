# AI Career Prep Hub - Design System

Project: AI Career Prep Hub  
Project ID: 14471870673985272686  
Asset ID: assets/8fb27216ae9a4876aeb7a8942db8ac7d  
Requested screen ID: asset-stub-assets-8fb27216ae9a4876aeb7a8942db8ac7d-1776736485140

Note: Stitch exposes this item as a design-system asset instance, not as a normal screen. The screen endpoint returned "Requested entity was not found" for both the stub ID and the underlying asset-instance ID, so no hosted HTML or screenshot URL was available for direct download.

## Theme

- Display name: Foundry Pro
- Color mode: Light
- Color variant: Fidelity
- Font: Manrope
- Headline font: Manrope
- Body font: Inter
- Label font: Inter
- Roundness: ROUND_EIGHT
- Spacing scale: 2
- Primary override: #3B82F6
- Secondary override: #10B981
- Neutral override: #1A1A1B

## Design System Strategy: Executive Clarity

### Creative North Star

The system is centered on "The Precision Path": a polished career-prep workspace that feels calm, authoritative, and editorial rather than like a generic SaaS dashboard. It uses intentional asymmetry, wide margins, tonal depth, and a curated assistant-like tone.

### Colors

The workspace uses warm, near-white surfaces and a deep inverse sidebar:

- surface: #FCF8F9
- surface-container-low: #F6F3F4
- surface-container: #F0EDEE
- surface-container-high: #EAE7E8
- surface-container-highest: #E5E2E3
- surface-container-lowest: #FFFFFF
- inverse-surface: #303031
- primary: #0058BE
- primary-container: #2170E4
- secondary: #006C49
- secondary-container: #6CF8BB

Avoid 1px divider lines for sectioning. Prefer tonal shifts between surface tiers. Use subtle primary-to-primary-container gradients for high-impact CTAs.

### Typography

Use Manrope for display and headline text, with Inter for body text and labels. Large headline scales should feel editorial and airy. Body text should prioritize long-form readability for resumes, transcripts, and coaching feedback.

### Elevation

Depth is created through tonal layering. Place lighter cards on slightly darker surface bands. When real elevation is needed, use ambient shadows with soft opacity rather than pure black shadows. Use ghost borders only as a fallback where accessibility or clarity requires them.

### Components

- Primary buttons: rounded, primary gradient, white text.
- Secondary buttons: surface-based or success-state with secondary-container.
- Tertiary buttons: text-only, low emphasis.
- Cards and lists: no divider lines; use spacing and alternating surface tiers.
- Inputs: surface-container-highest backgrounds with a primary bottom accent on focus.
- AI feedback glass: semi-transparent primary-fixed treatment with backdrop blur.

### Do

- Use generous spacing and editorial section headers.
- Use large container roundness for friendly authority.
- Separate interview questions from user answers with whitespace.

### Do Not

- Do not use pure black text.
- Do not use solid borders to define the sidebar.
- Do not crowd the page; move density into nested surface tiers.
