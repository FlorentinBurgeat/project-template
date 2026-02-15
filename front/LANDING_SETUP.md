# Landing Page Setup Guide

This document is intended for **AI agents** and developers who need to customize the boilerplate landing page for a new business project.

---

## Quick Start

1. Edit text content in `locales/en.json` (and `fr.json` for French)
2. Update structural config in `app/landing.config.ts` (icons, URLs)
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
      primary: "sky", // ← Replace 'green' with chosen color
      neutral: "slate"
    }
  }
})
```

Available Nuxt UI color names: `red`, `orange`, `amber`, `yellow`, `lime`, `green`, `emerald`, `teal`, `cyan`, `sky`, `blue`, `indigo`, `violet`, `purple`, `fuchsia`, `pink`, `rose`.

#### File 2: `app/assets/css/main.css`

Replace the `--color-green-*` palette with the chosen color's Tailwind palette. For example, if the user chose `sky`:

```css
@theme static {
  --font-sans: "Outfit", sans-serif;

  --color-sky-50: #f0f9ff;
  --color-sky-100: #e0f2fe;
  --color-sky-200: #bae6fd;
  --color-sky-300: #7dd3fc;
  --color-sky-400: #38bdf8;
  --color-sky-500: #0ea5e9;
  --color-sky-600: #0284c7;
  --color-sky-700: #0369a1;
  --color-sky-800: #075985;
  --color-sky-900: #0c4a6e;
  --color-sky-950: #082f49;
}
```

Reference for all Tailwind color palettes: https://tailwindcss.com/docs/colors

### Step 3: Update landing content

#### 3a. Edit text content in i18n locale files

Edit `i18n/locales/en.json` — replace every placeholder string:

| Field                              | What to replace                     |
| ---------------------------------- | ----------------------------------- |
| `landing.appName`                  | Your application name               |
| `landing.tagline`                  | Short tagline for branding          |
| `landing.hero.title`               | Main headline visitors see first    |
| `landing.hero.description`         | 1-2 sentence value proposition      |
| `landing.hero.primaryCta`          | Main button label                   |
| `landing.valueProposition.*`       | Problem/solution pitch              |
| `landing.features.items[].title`   | Feature titles and descriptions     |
| `landing.testimonials.items[]`     | Testimonial names, roles, quotes    |
| `landing.faq.items[]`              | Questions and answers               |
| `landing.finalCta.*`               | Conversion call-to-action text      |
| `landing.footer.copyright`         | Copyright notice                    |
| `landing.seo.*`                    | Page title, description             |

If you want to support French, also edit `i18n/locales/fr.json` with French translations.

#### 3b. Update structural config

Edit `app/landing.config.ts` — update the `landingStructure` object:

| Field                          | What to update                      |
| ------------------------------ | ----------------------------------- |
| `featureIcons[]`               | Icon names for features             |
| `valuePropositionIcons[]`      | Icon names for value props          |
| `testimonialAvatars[]`         | Avatar image URLs                   |
| `appDownload.appStoreUrl`      | App Store link (set to `''` to hide)|
| `appDownload.googlePlayUrl`    | Google Play link                    |
| `socialLinks[]`                | Social media URLs and icons         |
| `seo.ogImage`                  | OG image path                       |
| `auth.*`                       | Keycloak URLs                       |

### Step 4: Replace visual assets

| Asset             | Location                              | Size/Format          |
| ----------------- | ------------------------------------- | -------------------- |
| Logo SVG          | `app/components/AppLogo.vue`          | Inline SVG component |
| App Store badge   | `public/images/badge-app-store.svg`   | 135x40 SVG           |
| Google Play badge | `public/images/badge-google-play.svg` | 135x40 SVG           |
| OG Image          | `public/og-image.png`                 | 1200x630 PNG         |
| Favicon           | `public/favicon.ico`                  | 32x32 ICO            |

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

---

## Internationalization (i18n)

The landing page now uses **@nuxtjs/i18n** for multi-language support. All text content is stored in JSON locale files instead of `landing.config.ts`.

### How it Works

1. **Text content** is stored in `i18n/locales/*.json` files (one per language)
2. **Structure, icons, and URLs** are stored in `landing.config.ts` (in `landingStructure`)
3. Components use the `useLandingConfig()` composable which merges structure + translations
4. Users can switch languages using the `<LanguageSwitcher>` component in the header

### File Structure

```
locales/
├── en.json         ← English translations
└── fr.json         ← French translations

i18n.config.ts      ← i18n configuration
landing.config.ts   ← Structural config (icons, URLs, arrays)
```

### Editing Translations

To update landing page text in English, edit `locales/en.json`:

```json
{
  "landing": {
    "appName": "Your App Name",
    "hero": {
      "title": "Your Hero Title",
      "description": "Your description..."
    }
  }
}
```

The structure follows the same hierarchy as the original `landing.config.ts`, but contains only translatable text.

### Adding a New Language

1. Create a new locale file: `locales/de.json` (for German, for example)
2. Copy the structure from `en.json` and translate all values
3. Add the locale to `nuxt.config.ts`:

```typescript
i18n: {
  locales: [
    { code: 'en', name: 'English', file: 'en.json', language: 'en-US' },
    { code: 'fr', name: 'Français', file: 'fr.json', language: 'fr-FR' },
    { code: 'de', name: 'Deutsch', file: 'de.json', language: 'de-DE' }
  ]
}
```

4. Update the `LanguageSwitcher.vue` component to add the flag emoji:

```typescript
function getLocaleIcon(code: string) {
  const icons: Record<string, string> = {
    en: "🇬🇧",
    fr: "🇫🇷",
    de: "🇩🇪"  // Add new locale icon
  }
  return icons[code] || "🌐"
}
```

### Changing Default Language

Edit `nuxt.config.ts`:

```typescript
i18n: {
  defaultLocale: 'en',  // Change to 'fr', 'de', etc.
  // ...
}
```

### Using Translations in Components

**In templates** (recommended):
```vue
<template>
  <h1>{{ $t('landing.hero.title') }}</h1>
</template>
```

**In script setup**:
```vue
<script setup>
const { t } = useI18n()
const title = t('landing.hero.title')
</script>
```

**Using the landing config composable**:
```vue
<script setup>
import { useLandingConfig } from '~/landing.config'

const cfg = useLandingConfig()
// cfg.value.hero.title is now reactive and translated
</script>
```

### Removing i18n

If you only need one language:

1. Remove `@nuxtjs/i18n` from `nuxt.config.ts` modules
2. Move all text from `en.json` back into `landing.config.ts` as hardcoded values
3. Replace `useLandingConfig()` with a simple export of the config object
4. Remove the `<LanguageSwitcher>` component from `app.vue`
5. Run `pnpm remove @nuxtjs/i18n`
