# Landing Page Setup Guide

This document is intended for **AI agents** and developers who need to customize the boilerplate landing page for a new business project.

---

## Quick Start

1. Open `app/landing.config.ts`
2. Replace all placeholder values with your project's content
3. Replace images in `public/images/`
4. Update `app/components/AppLogo.vue` with your logo SVG
5. Choose your brand color (see Color Customization below)

---

## Setup Steps (For Agents)

When creating a new project from this template, execute the following steps in order:

### Step 1: Ask the user for core information

- **App name** (e.g., "Acme App")
- **Tagline** (e.g., "Project management, simplified")
- **Primary color** (e.g., "sky", "blue", "violet", "rose", "amber", "emerald")
- **Brief description** of what the app does

### Step 2: Apply the brand color

Change the primary color in **two files**:

#### File 1: `app/app.config.ts`

```typescript
export default defineAppConfig({
  ui: {
    colors: {
      primary: 'sky',    // ← Replace 'green' with chosen color
      neutral: 'slate'
    }
  }
})
```

Available Nuxt UI color names: `red`, `orange`, `amber`, `yellow`, `lime`, `green`, `emerald`, `teal`, `cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`.

#### File 2: `app/assets/css/main.css`

Replace the `--color-green-*` palette with the chosen color's Tailwind palette. For example, if the user chose `sky`:

```css
@theme static {
  --font-sans: 'Outfit', sans-serif;

  --color-sky-50: #F0F9FF;
  --color-sky-100: #E0F2FE;
  --color-sky-200: #BAE6FD;
  --color-sky-300: #7DD3FC;
  --color-sky-400: #38BDF8;
  --color-sky-500: #0EA5E9;
  --color-sky-600: #0284C7;
  --color-sky-700: #0369A1;
  --color-sky-800: #075985;
  --color-sky-900: #0C4A6E;
  --color-sky-950: #082F49;
}
```

Reference for all Tailwind color palettes: https://tailwindcss.com/docs/colors

### Step 3: Update landing config

Edit `app/landing.config.ts` — replace every placeholder string:

| Field | What to replace |
|---|---|
| `appName` | Your application name |
| `tagline` | Short tagline for branding |
| `hero.title` | Main headline visitors see first |
| `hero.description` | 1-2 sentence value proposition |
| `hero.primaryCta` | Main button (label + destination URL) |
| `valueProposition.*` | Problem/solution pitch |
| `features.items[]` | 3-6 features with icons, titles, descriptions |
| `testimonials.items[]` | 2-4 real testimonials |
| `appDownload.appStoreUrl` | App Store link (set to `''` to hide) |
| `appDownload.googlePlayUrl` | Google Play link (set to `''` to hide) |
| `faq.items[]` | 3-6 common questions and answers |
| `finalCta.*` | Conversion call-to-action text |
| `footer.copyright` | Copyright notice |
| `footer.socialLinks[]` | Social media links and icons |
| `seo.*` | Page title, description, OG image path |
| `auth.loginUrl` | Keycloak login redirect |
| `auth.registerUrl` | Keycloak register redirect |

### Step 4: Replace visual assets

| Asset | Location | Size/Format |
|---|---|---|
| Logo SVG | `app/components/AppLogo.vue` | Inline SVG component |
| App Store badge | `public/images/badge-app-store.svg` | 135x40 SVG |
| Google Play badge | `public/images/badge-google-play.svg` | 135x40 SVG |
| OG Image | `public/og-image.png` | 1200x630 PNG |
| Favicon | `public/favicon.ico` | 32x32 ICO |

### Step 5: Optional font change

If the user wants a different font, change **two places**:

1. `app/assets/css/main.css` → `--font-sans: 'YourFont', sans-serif;`
2. `app/app.vue` → Google Fonts `<link>` URL in `useHead`

---

## Configuration Reference

### `landing.config.ts` — Full Field Guide

```
LandingConfig
├── appName: string              — App display name
├── tagline: string              — Short branding tagline
├── description: string          — Brief app description
├── navLinks[]                   — Header anchor navigation
│   ├── label: string            — Link text
│   └── to: string               — Anchor (#features) or URL
├── hero
│   ├── headline: string         — Small text above title
│   ├── title: string            — Main headline
│   ├── description: string      — Sub-headline text
│   ├── primaryCta               — Main CTA button
│   │   ├── label: string
│   │   └── to: string
│   └── secondaryCta             — Secondary button
│       ├── label: string
│       ├── to: string
│       └── icon?: string        — Iconify icon name
├── valueProposition
│   ├── headline: string
│   ├── title: string
│   ├── description: string
│   └── points[]                 — 3 value points
│       ├── icon: string
│       └── text: string
├── features
│   ├── headline: string
│   ├── title: string
│   ├── description: string
│   └── items[]                  — 3-6 feature cards
│       ├── icon: string
│       ├── title: string
│       └── description: string
├── testimonials
│   ├── headline: string
│   ├── title: string
│   └── items[]                  — 2-4 testimonials
│       ├── name: string
│       ├── role: string
│       ├── avatar: string       — Image URL
│       └── quote: string
├── appDownload
│   ├── headline: string
│   ├── title: string
│   ├── description: string
│   ├── appStoreUrl: string      — Empty '' to hide
│   └── googlePlayUrl: string    — Empty '' to hide
├── faq
│   ├── headline: string
│   ├── title: string
│   └── items[]                  — 3-6 questions
│       ├── label: string        — Question text
│       └── content: string      — Answer text
├── finalCta
│   ├── title: string
│   ├── description: string
│   ├── primaryCta
│   │   ├── label: string
│   │   └── to: string
│   └── secondaryCta?            — Optional secondary button
│       ├── label: string
│       ├── to: string
│       └── icon?: string
├── footer
│   ├── copyright: string
│   ├── links[]                  — Legal/info links
│   │   ├── label: string
│   │   └── to: string
│   └── socialLinks[]            — Social media icons
│       ├── icon: string         — Iconify icon (i-simple-icons-*)
│       ├── to: string           — Profile URL
│       └── label: string        — Aria label
├── seo
│   ├── title: string
│   ├── description: string
│   └── ogImage: string          — Path to OG image
└── auth
    ├── loginUrl: string         — Keycloak login
    ├── registerUrl: string      — Keycloak register
    └── accountUrl: string       — Keycloak account management
```

---

## Removing Sections

To hide a landing page section, remove the corresponding `<UPageSection>` block from `app/pages/index.vue`. The config values can remain — they simply won't be rendered.

## Adding Sections

1. Add a new config object in `landing.config.ts` (with matching interface)
2. Add a new `<UPageSection>` in `app/pages/index.vue` reading from the config
3. If the section needs a navigation link, add it to `navLinks[]`

---

## Icon Reference

Icons use Iconify names with the `i-` prefix. Browse available icons:

- **Lucide** (recommended for UI): `i-lucide-*` — https://lucide.dev/icons/
- **Simple Icons** (for brands): `i-simple-icons-*` — https://simpleicons.org/
- **Heroicons**: `i-heroicons-*`
