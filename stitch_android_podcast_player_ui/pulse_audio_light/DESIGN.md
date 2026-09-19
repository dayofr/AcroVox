---
name: Pulse Audio Light
colors:
  surface: '#f8f9ff'
  surface-dim: '#d8dadf'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3f9'
  surface-container: '#eceef3'
  surface-container-high: '#e6e8ee'
  surface-container-highest: '#e1e2e8'
  on-surface: '#191c20'
  on-surface-variant: '#5b4039'
  inverse-surface: '#2e3135'
  inverse-on-surface: '#eff0f6'
  outline: '#907067'
  outline-variant: '#e4beb4'
  surface-tint: '#b02f00'
  primary: '#b02f00'
  on-primary: '#ffffff'
  primary-container: '#ff5722'
  on-primary-container: '#541200'
  inverse-primary: '#ffb5a0'
  secondary: '#545f73'
  on-secondary: '#ffffff'
  secondary-container: '#d5e0f8'
  on-secondary-container: '#586377'
  tertiary: '#565d63'
  on-tertiary: '#ffffff'
  tertiary-container: '#6f757c'
  on-tertiary-container: '#fcfcff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdbd1'
  primary-fixed-dim: '#ffb5a0'
  on-primary-fixed: '#3b0900'
  on-primary-fixed-variant: '#862200'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#dde3eb'
  tertiary-fixed-dim: '#c1c7cf'
  on-tertiary-fixed: '#161c22'
  on-tertiary-fixed-variant: '#41474e'
  background: '#f8f9ff'
  on-background: '#191c20'
  surface-variant: '#e1e2e8'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '800'
    lineHeight: 48px
    letterSpacing: -0.03em
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '800'
    lineHeight: 36px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 26px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 17px
    fontWeight: '600'
    lineHeight: 22px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.01em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  gutter-desktop: 2rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 3rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1.25rem
  space-xl: 2rem
---

## Brand & Style
This design system crafts an energetic, lucid, and modern audio streaming experience engineered specifically for high-mobility Android interfaces. Built on a crisp, elevated foundation, it blends clean architectural minimalism with vibrant tactical moments of color. 

The aesthetic avoids sterile flat layouts in favor of subtle layered depth, soft physical surfaces, and precise typographic cadence. The dynamic coral-orange signature brings an unmistakable pulse to interactions, playback states, and call-to-actions, establishing a sense of forward momentum, discovery, and effortless legibility under varying daylight conditions.

## Colors
The color palette relies on luminous white backgrounds juxtaposed with neutral structural tiers and sharp dark slate typography.

- **Primary (`#ff5722`)**: The signature electric coral pulse. Dedicated to interactive touchpoints, dynamic audio scrubbers, progress fills, playback toggles, and live podcast badges.
- **Secondary (`#1e293b`)**: Deep graphite slate. Used for structural hierarchy, active tabs, prominent secondary iconography, and high-emphasis subheadings.
- **Tertiary (`#e2e8f0`)**: Refined architectural border and divider tint. Guarantees hairline separation between dense media feeds without visual clutter.
- **Neutral (`#111418`)**: Inky carbon black calibrated for maximum reading comfort, used for podcast titles, primary headlines, and active text states.

### Supporting Surfaces & Neutral Roles
- **Base Canvas**: `#ffffff` to `#f8f9fa` for clean, breathable screen planes.
- **Container Surfaces**: `#ffffff` for elevated cards, `#f1f3f5` for inset player panels and chips, and `#e9ecef` for active selection backgrounds and pressed states.
- **Secondary Body Text**: `#64748b` for episode release dates, duration metrics, creator credits, and descriptions.

## Typography
Plus Jakarta Sans serves as the sole typographic engine, providing geometric precision, contemporary rhythm, and friendly optical openness tailored for reading show notes and dense episode catalogs on mobile viewports.

- Use tighter negative tracking on `display` and `headline` tiers to give titles immediate punch and compact editorial polish.
- `label-sm` is reserved for uppercase timestamps, audio bitrate tags, and live broadcast indicators.
- Maintain strict line-height adherence to ensure podcast show descriptions retain vertical stability next to square album thumbnails.

## Layout & Spacing
The layout follows an 8pt architectural rhythm integrated within an adaptive fluid grid structure calibrated for handheld Android ergonomics.

- **Mobile Viewports (<600dp)**: Single-column feed or dual-column discovery grid. Outer margins are anchored at `margin` (`1rem`), with 16dp column gutters ensuring dense yet readable episode lists. Bottom content features an automated persistent offset to clear the floating mini-player.
- **Tablet & Foldable Viewports (600dp - 1024dp)**: Two-column split interface: left pane houses the feed and filter chips with `gutter-tablet` (`1.5rem`), while the right pane holds a persistent expanded player and playback queue.
- **Desktop / Large Screens (>1024dp)**: Fixed centered shell with `margin-desktop` (`3rem`), deploying 3-to-4 column modular cards for explore and podcast channel grids.

## Elevation & Depth
Depth is created through soft ambient diffusions, crisp hairline boundaries, and tiered tonal planes instead of heavy drop shadows.

- **Tier 0 (Canvas Base)**: `#f8f9fa` baseline layer for views and scroll streams.
- **Tier 1 (Surface Cards & Panels)**: `#ffffff` surface, bounded by a 1px hairline border in `#e2e8f0` and an ultra-soft atmospheric shadow: `0 2px 8px -2px rgba(30, 41, 59, 0.04), 0 8px 16px -4px rgba(30, 41, 59, 0.03)`.
- **Tier 2 (Floating Mini-Player & Bottom Sheets)**: `#ffffff` background with 92% opacity and `backdrop-filter: blur(12px)`. Elevated by an airy ambient lift: `0 8px 24px -4px rgba(17, 20, 24, 0.08), 0 2px 6px -1px rgba(17, 20, 24, 0.03)`.
- **Tier 3 (Modals & Overlays)**: Full white card bounded by `0 16px 36px -6px rgba(17, 20, 24, 0.12)` over an scrim tinted with `rgba(17, 20, 24, 0.35)`.
- **Accent Glow**: Selected floating controls (e.g., active play fab) utilize a low-spread primary color diffusion: `0 6px 16px -2px rgba(255, 87, 34, 0.35)`.

## Shapes
A balanced rounded shape language (`roundedness: 2`) gives the interface an approachable, handheld-friendly aesthetic that mirrors Android's modern hardware contours.

- **Base Radius (`0.5rem`)**: Applied to square episode artwork, input fields, checkboxes, and inline control toggles.
- **Large Radius (`1rem`)**: Applied to podcast cards, persistent bottom sheets, playback modal sheets, and dialogue panels.
- **Pill Form factor (`9999px`)**: Reserved for audio chips, duration tags, scrub head handles, and primary playback action triggers.

## Components

### Buttons
- **Primary Playback Buttons**: Circular or pill-shaped, filled with vibrant primary coral (`#ff5722`), text and iconography in pure white (`#ffffff`). Subtle scale down (0.97) on tap, accompanied by the primary accent glow.
- **Secondary Buttons**: Background in `#f1f3f5`, text in `#1e293b`. On active press, transitions to `#e9ecef`.
- **Ghost / Icon Buttons**: Transparent background, `#1e293b` icon color. Pressed state uses a soft circular ripple of `rgba(30, 41, 59, 0.06)`.

### Chips & Filter Pills
- **Unselected**: Flat `#f1f3f5` container, 1px border in `#e2e8f0`, text in `#64748b`.
- **Selected**: Solid `#1e293b` container with pure white text, or solid `#ff5722` when signifying active audio playback modes (e.g., "Speed 1.5x", "Trim Silence").

### Lists & Episode Rows
- Episode items rest on `#ffffff` with a bottom divider in `#e2e8f0`.
- Left-aligned square thumbnail (rounded to `0.5rem`), followed by title in `#111418` (`headline-sm`), description excerpt in `#64748b` (`body-sm`), and a pill tag indicating remaining time and release date.
- Quick action buttons (download, queue, bookmark) align on the right edge with a minimum 48x48dp touch target.

### Cards
- Constructed with `#ffffff` backgrounds, 1px `#e2e8f0` structural borders, and rounded corners (`1rem`).
- Podcast showcase cards feature full-width cover imagery on top with a gentle bottom-to-top subtle tonal gradient, preserving legibility for floating bookmark triggers.

### Checkboxes, Radios & Switches
- **Switches (Audio Settings)**: Track uses `#e2e8f0` (off) and `#ff5722` (on). The thumb is pure `#ffffff` with a gentle drop shadow.
- **Checkboxes & Radios**: Unchecked states use a 1.5px border in `#64748b`. Checked states display a smooth fill of `#ff5722` with a white checkmark/dot.

### Input Fields
- Container filled with `#f8f9fa`, framed by a 1px border in `#e2e8f0`.
- Text typed in `#111418`, placeholder in `#64748b`.
- On focus: Background shifts to `#ffffff`, border switches to `#ff5722` with an ambient ring of `rgba(255, 87, 34, 0.12)`.

### Specialized Audio Components
- **Floating Mini-Player**: Docked 16dp above the bottom navigation bar. Blurred `#ffffff` backdrop with 1px border `#e2e8f0`. Features a micro progress bar running across the top edge in `#ff5722`, play/pause pill toggle on the right, and title marquee text.
- **Audio Waveform Scrubber**: Unplayed rail in `#e2e8f0`, dynamic played rail in `#ff5722`, and a draggable 16dp circular scrubber thumb with a subtle drop shadow.