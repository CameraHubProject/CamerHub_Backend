---
name: CameraHub Vietnam
colors:
  surface: '#fbf9f9'
  surface-dim: '#dbdad9'
  surface-bright: '#fbf9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#efeded'
  surface-container-high: '#e9e8e7'
  surface-container-highest: '#e3e2e2'
  on-surface: '#1b1c1c'
  on-surface-variant: '#444748'
  inverse-surface: '#303031'
  inverse-on-surface: '#f2f0f0'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#080808'
  on-primary: '#ffffff'
  primary-container: '#202020'
  on-primary-container: '#898787'
  inverse-primary: '#c8c6c5'
  secondary: '#5b6148'
  on-secondary: '#ffffff'
  secondary-container: '#e0e6c6'
  on-secondary-container: '#61674e'
  tertiary: '#000b05'
  on-tertiary: '#ffffff'
  tertiary-container: '#002617'
  on-tertiary-container: '#579475'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1b1c1c'
  on-primary-fixed-variant: '#474746'
  secondary-fixed: '#e0e6c6'
  secondary-fixed-dim: '#c4caac'
  on-secondary-fixed: '#181e0a'
  on-secondary-fixed-variant: '#434932'
  tertiary-fixed: '#b0f1cd'
  tertiary-fixed-dim: '#95d4b2'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#0e5137'
  background: '#fbf9f9'
  on-background: '#1b1c1c'
  surface-variant: '#e3e2e2'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 42px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
  price-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
  price-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 24px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 3rem
  margin-mobile: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system targets photography enthusiasts, professionals, and vintage optics collectors in Vietnam seeking verified, high-grade pre-owned cameras and lenses. The visual language blends Japanese mechanical minimalism with modern editorial curation. 

It balances two primary moods:
- **Mechanical Precision**: Clean lines, disciplined data presentation, strict structural grids, and technical confidence reminiscent of Leica, Contax, and Hasselblad hardware.
- **Warm Curatorial Warmth**: An organic gallery aesthetic that counters clinical e-commerce tropes, elevating pre-owned cameras into collectible artifacts through ample negative space and tactile tone.

The aesthetic philosophy merges **Modern Editorial Minimalism** with structural utility. Surfaces remain restrained and quiet, ensuring product photography—patinas, brassing, coated glass, and dial knurling—remains the focal centerpiece.

## Colors

The palette operates on calibrated contrast, drawing the eye naturally toward imagery and verification signals without aggressive saturation:

- **Primary (`#202020`)**: Charcoal black used for foundational text, critical buttons, and crisp structural accents. Softer than pure black to preserve an analog paper feel.
- **Background Canvas (`#F7F6F2`)**: Warm archival off-white providing depth behind white product surfaces and reducing visual fatigue.
- **Surface (`#FFFFFF`)**: Pure white reserved strictly for elevated containers, product preview plates, sheet dialogs, and interactive card surfaces.
- **Secondary / Accent (`#858B70`)**: Muted olive green inspired by vintage field straps and weathered canvas bags. Used for secondary focus points, subtle interactive states, and camera category flags.
- **Tertiary / Trust & Warranty (`#2E6B4F`)**: Deep emerald olive reserved exclusively for inspection stamps, sensor health checkmarks, and 6–12 month warranty badges.
- **Neutral / Secondary Text (`#777777`)**: Balanced medium gray for shutter counts, optical condition notes, timestamps, and secondary specifications.
- **Structural Border (`#E5E5E5`)**: Ultra-refined neutral line tone used for hairline dividing rules and container contours.

## Typography

Typographic scale is anchored by **Plus Jakarta Sans**, chosen for its crisp modern geometry, open apertures, and distinctive humanist weight balance that supports Vietnamese diacritics natively without clashing glyph heights.

### Localization & Currency Directives
- **Vietnamese Diacritics**: Ensure strict minimum `1.4x` line-height on all body copy to prevent high tone marks (hỏi, ngã, sắc, nặng) from colliding with baseline accents or descenders.
- **Vietnamese Đồng Formatting**:
  - Always render pricing using strict periods for thousands separators: `24.500.000 ₫`.
  - Maintain the currency symbol `₫` immediately following a non-breaking space (`&nbsp;₫`).
  - For editorial display cards, style the `₫` glyph at `80%` scale and medium weight aligned to the cap height.
- **Condition Grading & Optical Specs**: Use uppercase medium weights with `+0.04em` tracking for technical microcopy (e.g., `SHUTTER: 1,420`, `GRADE: 99% NEAR MINT`, `MOUNT: LEICA M`).

## Layout & Spacing

The structural layout utilizes a responsive **12-column fixed-max grid** that expands fluidly up to `1440px`, centered gracefully on ultra-wide screens.

- **Breakpoints**:
  - **Desktop (≥ 1024px)**: 12 columns, 48px canvas margin (`margin`), 24px column gutter (`gutter`).
  - **Tablet (768px – 1023px)**: 8 columns, 32px canvas margin, 20px column gutter.
  - **Mobile (≤ 767px)**: 4 columns, 20px canvas margin (`margin-mobile`), 16px column gutter (`gutter-mobile`).

### Layout Behavior
- **Product Listing Grid**: 4 columns on large desktop, 3 columns on standard desktop, 2 columns on tablet, and single or asymmetric 2-column list on mobile.
- **Whitespace Integrity**: Product imagery must maintain generous breathing room. Minimum inner card padding must scale from `space-md` on mobile to `space-lg` on desktop to retain an uncluttered, art-book ambiance.

## Elevation & Depth

Visual hierarchy uses a crisp architectural approach: **structural boundaries and warm tonal layering** take precedence over heavy dropshadows.

- **Layer 0 (Canvas Base)**: `#F7F6F2` warm archival off-white background.
- **Layer 1 (Card & Section Surfaces)**: Pure `#FFFFFF` surfaces delineated by 1px hairline borders (`#E5E5E5`). No ambient shadow is applied in resting state.
- **Layer 2 (Floating & Hover Elements)**: Interactive cards on hover and filter dropdowns lift slightly using an ultra-subtle optical shadow tinted with the primary hue: `0 8px 24px -4px rgba(32, 32, 32, 0.05), 0 2px 6px -1px rgba(32, 32, 32, 0.03)`.
- **Layer 3 (Overlays, Drawers & Lightboxes)**: High-resolution image zoom inspect drawers and inspection condition sheets sit on pure white with high-blur backdrop dimming: `rgba(32, 32, 32, 0.45)` with `backdrop-filter: blur(8px)`.

## Shapes

The design system adopts a **Soft (Level 1)** shape language. The subtle radius softens mechanical technicality without sacrificing precision.

- **Base Radii (`0.25rem` / `4px`)**: Applied to technical metadata pills, inspection stamps, input fields, and condition tags.
- **Medium Radii (`0.5rem` / `8px`)**: Applied to product image containers, surface cards, and modal dialogs.
- **Buttons**: Strict 4px corners to reflect the machined silhouette of vintage camera bodies and shutter buttons. Circles are allowed exclusively for floating action icon triggers (e.g., image gallery arrows, wishlist icon).

## Components

### Buttons
- **Primary Action**: Solid `#202020` fill, pure `#FFFFFF` label, 4px corner radius, 48px height for desktop (`44px` on mobile). Subtle hover shift to `#383838`.
- **Secondary Action**: Pure `#FFFFFF` surface with 1px `#202020` border, charcoal text. Hover reveals `#F7F6F2` tint.
- **Tertiary / Ghost**: Transparent background, `#202020` text, subtle underline on hover; used for editorial links and secondary specification disclosures.

### Product Cards
- Built on a `#FFFFFF` surface with an explicit 1px `#E5E5E5` perimeter border.
- **Aspect Ratio**: 4:5 or 1:1 neutral framing with `#F7F6F2` image stage backing to standardize diverse product shots.
- **Card Hierarchy**:
  1. Top bar: Verified Condition Badge (left) + Warranty Tag (right).
  2. Central image: Clean studio cut or high-end context shot.
  3. Brand & Model: Bold `headline-sm`.
  4. Metadata strip: Shutter count, sensor size, condition tier (`99%`, `Like New`, `Collector`).
  5. Pricing line: Bold VND formatted price in `price-md`.

### Inspection & Condition Badges
- **Condition Tag**: Clean rectangular chip with 4px radius, `#F7F6F2` fill, 1px `#E5E5E5` outline, and `#202020` label (e.g., `Grade A+ • 98%`).
- **Warranty & Verification Badge**: Tinted `#2E6B4F` at 10% opacity background, solid `#2E6B4F` border and text, featuring a micro-check icon (e.g., `12M CameraHub Guarantee`).

### Form Inputs & Search
- Inputs feature `#FFFFFF` fill, 1px `#E5E5E5` border, 44px height, and `body-md` typography.
- Active focus state: Hairline border switches to `#202020` without loud outline rings.
- Placeholder text set in `#777777`.

### Filters & Chips
- Horizontal scrolling pill lists on mobile, vertical accordion on desktop.
- Unselected: Outline 1px `#E5E5E5`, background `#FFFFFF`, text `#777777`.
- Selected: Solid `#202020` background, `#FFFFFF` text, 0px border.

### Shutter & Sensor Condition Indicator
- Specialized mechanical rating component: segmented horizontal bar showing mechanical score (1–10), optical glass condition (No Fungus / No Scratches), and shutter actuation counter against rated lifespan (e.g., `4,210 / 150,000 actuations`).