---
name: Pulse Audio
colors:
  surface: '#111418'
  surface-dim: '#111418'
  surface-bright: '#36393e'
  surface-container-lowest: '#0b0e12'
  surface-container-low: '#191c20'
  surface-container: '#1d2024'
  surface-container-high: '#272a2e'
  surface-container-highest: '#323539'
  on-surface: '#e1e2e8'
  on-surface-variant: '#e4beb4'
  inverse-surface: '#e1e2e8'
  inverse-on-surface: '#2e3135'
  outline: '#ab8980'
  outline-variant: '#5b4039'
  surface-tint: '#ffb5a0'
  primary: '#ffb5a0'
  on-primary: '#5f1500'
  primary-container: '#ff5722'
  on-primary-container: '#541200'
  inverse-primary: '#b02f00'
  secondary: '#cdbdff'
  on-secondary: '#370096'
  secondary-container: '#5203d5'
  on-secondary-container: '#c0acff'
  tertiary: '#ffb59e'
  on-tertiary: '#54200f'
  tertiary-container: '#c97e66'
  on-tertiary-container: '#4c1a09'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#ffb5a0'
  on-primary-fixed: '#3b0900'
  on-primary-fixed-variant: '#862200'
  secondary-fixed: '#e8deff'
  secondary-fixed-dim: '#cdbdff'
  on-secondary-fixed: '#20005f'
  on-secondary-fixed-variant: '#4f00d0'
  tertiary-fixed: '#ffdbd0'
  tertiary-fixed-dim: '#ffb59e'
  on-tertiary-fixed: '#390c01'
  on-tertiary-fixed-variant: '#713623'
  background: '#111418'
  on-background: '#e1e2e8'
  surface-variant: '#323539'
typography:
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
  headline-md:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Space Grotesk
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  title-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style

The design system embodies an expressive, tactile, and immersive audio sanctuary. Engineered primarily for modern Android experiences (Android 14+ / Material You evolution), it balances functional clarity with sensory richness. The aesthetic merges contemporary dark-mode minimalism with vivid, glowing accents that echo the energy of live voice, music, and storytelling.

### Emotional Signature
- **Immersive & Focused:** Deep charcoal and slate surfaces eliminate eye fatigue during long listening sessions and nighttime discovery.
- **Dynamic & Kinetic:** Kinetic coral accents and nocturnal violet ambient touches evoke sound waves, equalizers, and real-time audio movement.
- **Tactile Comfort:** Pill-shaped controls, soft layered cards, and bouncy press states invite fluid one-handed thumb interaction.

### Aesthetic Paradigm
The visual philosophy combines **Tonal Layering** with **Tactile Modernism**:
- Deep obsidian and charcoal backdrops.
- Floating container surfaces differentiated through subtle tonal shifts and delicate luminous ghost borders rather than heavy drop shadows.
- Vibrant, high-contrast focal points dedicated to playback states, progress bars, and call-to-actions.

## Colors

The color architecture is built natively for dark mode, using luminance tiers to construct depth without muddying saturated accent notes.

### Role Assignments
- **Primary (`#FF5722` - Electric Coral):** Core interactive focal points, dynamic progress indicators, current episode scrubbers, active playback toggles, and primary Floating Action Buttons (FAB).
- **Secondary (`#7C4DFF` - Nocturnal Violet):** Contextual depth, category badges, chapter markers, active equalizer spikes, and secondary actions.
- **Tertiary (`#FFAB91` - Peach Glow):** Soft highlight states, subtle progress track backgrounds, duration timestamps, and muted audio waveform tracks.
- **Neutral Base (`#111418` - Dark Slate Charcoal):** Root canvas background.

### Surface Elevation Tokens (Prose Guidance)
- **Surface Level 0 (Canvas):** `#111418`
- **Surface Level 1 (Resting Cards / Lists):** `#181C22`
- **Surface Level 2 (Floating Player / Bottom Nav / Menus):** `#20252D`
- **Surface Level 3 (Dialogs / Modals / Tooltips):** `#2A303A`
- **Outline Subtle (Card & Pill Borders):** `rgba(255, 255, 255, 0.08)`
- **Outline Active (Focused Controls):** `rgba(255, 87, 34, 0.40)`
- **Text Primary:** `#F3F4F6`
- **Text Secondary:** `#9CA3AF`
- **Text Disabled / Muted:** `#4B5563`

## Typography

The typographical pairing unites the technological precision of **Space Grotesk** for show titles, episode banners, and player headers with the legible, warm humanist geometry of **Plus Jakarta Sans** for metadata, descriptions, timestamps, and controls.

### Implementation Guidelines
- **Space Grotesk (Display & Headlines):** Used exclusively for structural anchor points: podcast titles, player screen episode titles, and high-impact section banners. Kerning is kept tight (`letter-spacing: -0.02em`) to deliver modern editorial weight.
- **Plus Jakarta Sans (Body & Controls):** Tuned for glanceability in motion. Use `title-md` for podcast episode lists where rapid scrolling requires clear distinction between listened and unlistened tracks.
- **Micro-Metrics:** Durations (`48 min left`), bitrates, and audio speeds (`1.2x`) rely on `label-sm` set in medium weight with tabular figure alignment to avoid jitter during real-time timeline updates.

## Layout & Spacing

The layout is optimized for high-density audio browsing and ergonomic one-handed thumb interaction on mobile devices.

### Mobile Grid & Layout Structure
- **Columns & Margins:** 4-column layout on standard phones with `margin: 1rem` (16px) and `gutter: 1rem` (16px).
- **Vertical Stack Clearance:** The primary scroll area must include a bottom content padding reservation equivalent to the combined height of the bottom navigation bar (64px), the floating mini-player (64px), and the inter-component spacing (12px), totaling a minimal clearance of `140px`.

### Responsive Scaling
- **Tablet / Foldable Expanded View (600dp+):** Switches to an 8-column layout with fixed master-detail dual panes: Left panel houses show navigation and current queue; right panel hosts the expanded active player and episode transcript.
- **Horizontal Scrolling Shelves:** Used for "New Releases", "Continue Listening", and "Top Subscriptions", with card snapshots peeking 24px off-screen edge to reinforce horizontal affordance.

## Elevation & Depth

Visual hierarchy does not use diffuse blurs or generic black shadows; instead, it utilizes **tonal luminous surfaces** alongside **subtle boundary outlines** that preserve OLED contrast.

### Tiers of Elevation
1. **Root Surface (`#111418`):** The ground plane. Pure flat canvas with zero shadow.
2. **Resting Cards (`#181C22`):** Episode rows and show grids sit on Surface 1 with an inset stroke: `1px solid rgba(255, 255, 255, 0.05)`.
3. **Floating Mini-Player (`#20252D`):** Sits persistently 8px above the bottom navigation bar. Features an ultra-soft directional ambient drop: `0px 8px 24px rgba(0, 0, 0, 0.45)` and a top rim highlight: `1px solid rgba(255, 255, 255, 0.12)`.
4. **Persistent Bottom Navigation (`#181C22` with 90% opacity & backdrop-blur):** Blurs underlying content passing beneath it (`backdrop-filter: blur(16px)`), separated by a single top divider `rgba(255, 255, 255, 0.06)`.
5. **Contextual Modals / Full Player Sheet (`#20252D`):** Deep ambient spread `0px -12px 32px rgba(0, 0, 0, 0.6)`.

## Shapes

The design system adheres to a full **Pill-shaped (Level 3)** paradigm for interactive targets, balanced by squircle-softened artwork and surfaces that harmonize with modern Android system geometry.

### Radius Specifications
- **Pills / CTAs / Chips / Mini-Player:** Full radius (`9999px` or `rounded-full`) delivering immediate tactile affordance.
- **Podcast Album Artwork:** Squircle corners with standard `16px` radius (`rounded-xl`) to prevent sharp corners while preserving cover art legibility.
- **Content Cards / Episode Sheets:** `24px` to `32px` corner radii, creating cozy, friendly containers.
- **Equalizer Bars & Badges:** Fully rounded capsule endpoints.

## Components

### 1. Persistent Floating Mini-Player
- **Positioning:** Fixed float hovering 8px above the bottom navigation dock, with 12px horizontal side margins.
- **Geometry & Background:** Pill container (`height: 64px`), background `#20252D`, stroke `1px solid rgba(255, 255, 255, 0.12)`.
- **Content Flow:** 
  - Left: 48px square episode artwork (`rounded-lg`).
  - Center: Truncated Episode title (`title-md`) stacked over Show title (`body-sm`, `#9CA3AF`).
  - Right: Quick action group: 10s Rewind (`icon-button`), Play/Pause circle FAB (40px, primary `#FF5722` with white icon), and Queue swipe handle.
- **Progress Line:** Micro 2px progress bar pinned to the bottom border of the capsule, filled with Coral `#FF5722`.

### 2. Android Bottom Navigation Dock
- **Structure:** 4-5 destinations: *Home, Search, Library, Activity/Downloads*.
- **Height & Style:** 64px height, Surface `#181C22` with 16px blur, zero elevation drop shadow, top stroke `1px solid rgba(255, 255, 255, 0.06)`.
- **Active State:** Pill indicator background behind active icon (`#7C4DFF` at 20% opacity) with active label colored `#FF5722`.

### 3. Buttons & Play Controls
- **Primary Play FAB:** Full pill or circle (`56px`), background `#FF5722`, content `#FFFFFF`. Tap scale compression: `transform: scale(0.96)`.
- **Secondary Actions:** Subtle pill container with `#20252D` fill and `1px solid rgba(255, 255, 255, 0.08)` border.
- **Interactive Equalizer Graphic:** Dynamic 3 to 4 vertical capsule bars (`width: 3px`, height animating between 4px and 16px) tinted `#FF5722` to signal live playback inside episode lists.

### 4. Episode Cards & Lists
- **Layout:** Horizontal split item. Left side displays a 64px rounded square thumbnail with duration pill badge anchored on the bottom edge.
- **Body Area:** Title (2 lines max, `title-md`), release date, and remaining playback duration in `#9CA3AF`.
- **Status Badges:** 
  - Downloaded: Capsule pill badge `#20252D` featuring a violet `#7C4DFF` checkmark icon.
  - Duration Badge: Translucent black capsule `rgba(0, 0, 0, 0.6)` with `label-sm` text.

### 5. Chips & Filters
- **Form:** Pill shape (`height: 32px`), padding `0 14px`.
- **Default State:** Transparent fill with `1px solid rgba(255, 255, 255, 0.12)`, text `#9CA3AF`.
- **Selected State:** Filled with `#FF5722` (or `#7C4DFF` for category moods), text `#FFFFFF`, stroke transparent.

### 6. Inputs & Search Fields
- **Container:** 48px height pill field (`rounded-full`), background `#181C22`, border `1px solid rgba(255, 255, 255, 0.08)`.
- **Focus State:** Border switches to `#FF5722` with a subtle inner glow `0 0 0 1px #FF5722`. Placeholder text in `#9CA3AF`.