---
name: OrionVault
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#c7c4d7'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#908fa0'
  outline-variant: '#464554'
  surface-tint: '#c0c1ff'
  primary: '#c0c1ff'
  on-primary: '#1000a9'
  primary-container: '#8083ff'
  on-primary-container: '#0d0096'
  inverse-primary: '#494bd6'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#ffb783'
  on-tertiary: '#4f2500'
  tertiary-container: '#d97721'
  on-tertiary-container: '#452000'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e1e0ff'
  primary-fixed-dim: '#c0c1ff'
  on-primary-fixed: '#07006c'
  on-primary-fixed-variant: '#2f2ebe'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffdcc5'
  tertiary-fixed-dim: '#ffb783'
  on-tertiary-fixed: '#301400'
  on-tertiary-fixed-variant: '#703700'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  code-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 20px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style
The design system for this product is centered on the "Digital Vault" concept—a high-security environment that feels impenetrable yet technologically advanced. The brand personality is authoritative, precise, and sophisticated.

The visual style blends **Modern Corporate** reliability with **Glassmorphism** and **Minimalist** precision. It utilizes deep, dark surfaces to minimize eye strain and emphasize the "void-like" security of the vault. To convey encryption and active protection, the system uses subtle inner glows and translucent layers that suggest depth without sacrificing the perception of structural integrity.

## Colors
This design system defaults to a **Dark Mode** experience to reinforce the feeling of a secure, private terminal.

- **Primary (Indigo):** Used for high-priority actions, brand moments, and focus states. It represents the "intelligence" of the vault.
- **Secondary (Emerald):** Reserved strictly for "Secure," "Active," "Verified," and "Success" states. This color is the primary indicator of safety.
- **Neutral (Midnight Gray):** The foundation of the UI. Backgrounds use the deepest value, while surface containers use lighter steps to create hierarchy.
- **Accent/Alert:** A high-visibility Red (#EF4444) should be used sparingly for "Insecure," "Breached," or "Delete" actions.

## Typography
The typography system prioritizes legibility and technical clarity. **Inter** is the workhorse for all interface elements, providing a neutral and modern feel.

For sensitive data—such as passwords, recovery keys, and 2FA codes—**JetBrains Mono** is utilized. The monospaced nature of the font ensures that ambiguous characters (like '0' vs 'O' or '1' vs 'l') are easily distinguishable, reducing user error during manual entry.

Headlines should be kept compact with slight negative letter-spacing to feel "locked-in" and sturdy.

## Layout & Spacing
The layout follows a **Fluid Grid** model with strict 4px increments. This creates a rhythmic, systematic feel that aligns with the software's mathematical nature.

- **Desktop:** 12-column grid with a maximum content width of 1280px. Use 24px gutters.
- **Mobile:** Single column with 16px side margins.

Large sections should be separated by clear, 1px borders rather than wide gaps to maintain a sense of "enclosure" within the vault. Padding within cards should be generous (24px) to ensure data doesn't feel cramped or "leaking" out of its container.

## Elevation & Depth
In this design system, depth is achieved through **Tonal Layers** and **Glassmorphism** rather than traditional heavy shadows.

1.  **Level 0 (Background):** The deepest Midnight Gray (#0F172A).
2.  **Level 1 (Panels):** Raised surface (#1E293B) with a subtle 1px border (#334155).
3.  **Level 2 (Modals/Popovers):** Semi-transparent surfaces using a backdrop blur (20px) and a faint inner glow (top-down) to simulate thick, protective glass.

Shadows, when used, are colored (tinted with the background) and extremely diffused, creating an "ambient glow" rather than a hard drop shadow.

## Shapes
The shape language balances approachability with structural discipline.

- **Containers & Cards:** Use a consistent 12px radius (`rounded-lg`).
- **Buttons & Inputs:** Use a 8px radius (`rounded-md`) to feel slightly more precise and mechanical.
- **Badges:** Use a fully rounded pill shape to distinguish them from interactive buttons.

Decorative elements, such as encryption progress bars, should use sharp inner corners and slightly rounded outer corners to mimic machined hardware.

## Components

- **Buttons:** Primary buttons use a solid Deep Purple fill with high-contrast white text. Secondary buttons use a "Ghost" style with a 1px border and subtle hover glow.
- **Input Fields:** Backgrounds should be slightly darker than their parent container. On focus, the border transitions to Primary Indigo with a soft outer glow. Use monospaced font for password fields.
- **Security Badges:** Small, pill-shaped indicators. "Secure" badges feature the Emerald Green color and a minimalist shield icon. "Insecure" badges use Red and an alert icon.
- **Vault Cards:** These house individual credentials. They must feature a prominent icon (company logo or generic key) and a "Copy" quick-action button that provides immediate visual feedback (color change to Emerald) when clicked.
- **Biometric Prompts:** Full-screen or centered overlays using heavy backdrop blur and a large, centered biometric icon (FaceID/Fingerprint) with a pulsing "Scanning" animation.
