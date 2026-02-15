# CLAUDE.md - Frontend

## Architecture Overview

The frontend is built with **Nuxt 4**, leveraging file-based routing, auto-imports, server-side rendering, and the Nuxt UI component library. The architecture follows Nuxt conventions for maximum developer productivity and performance.

---

## Tech Stack

- **Framework**: Nuxt 4 (Vue 3 Composition API)
- **Routing**: Nuxt file-based routing (built-in)
- **State Management**: useState composable (built-in, no Pinia)
- **Data Fetching**: useFetch & useAsyncData (built-in)
- **UI Library**: Nuxt UI (@nuxt/ui)
- **Styling**: Tailwind CSS (via Nuxt UI)
- **Build Tool**: Vite (via Nuxt)
- **Server Engine**: Nitro (built-in)
- **TypeScript**: Zero-config with auto-generated types

---

## Directory Structure

### Nuxt 4 App Directory

```
/app
  /assets           - Build-time assets (images, styles)
    /css
      main.css      - Global styles
  /components       - Auto-imported Vue components
    /ui             - Nuxt UI components
    TemplateMenu.vue
  /composables      - Auto-imported composition functions
    useAuth.ts
  /layouts          - Page layouts
    default.vue
  /middleware       - Route middleware
    auth.ts
  /pages            - File-based routing
    index.vue       → /
    about.vue       → /about
    /users
      index.vue     → /users
      [id].vue      → /users/:id
  /plugins          - Vue plugins
  /utils            - Auto-imported utility functions
  app.config.ts     - App-level configuration (reactive)
  app.vue           - Root component
```

### Server Directory (Backend/API)

```
/server
  /api              - API routes (prefixed with /api)
    users.ts        → /api/users
    /auth
      login.ts      → /api/auth/login
  /routes           - Server routes (no /api prefix)
    health.ts       → /health
  /middleware       - Server middleware (runs on all requests)
    log.ts
  /plugins          - Server plugins
  /utils            - Server-only utilities
```

---

## Important Note

**For detailed Nuxt 4 technical documentation** (navigation, middleware, data fetching, state management, SEO, server API routes, layouts, auto-imports, error handling), see the **nuxt-dev-guidelines** skill.

This file focuses on project-specific architecture and configuration.

---

## Routing: File-Based

Nuxt automatically generates routes from the `app/pages/` directory structure.

### Route Examples

```
pages/
├── index.vue                 → /
├── about.vue                 → /about
├── users/
│   ├── index.vue            → /users
│   ├── [id].vue             → /users/:id (dynamic)
│   └── profile.vue          → /users/profile
├── settings/
│   └── [...slug].vue        → /settings/* (catch-all)
└── admin-[role].vue         → /admin-:role
```

**For navigation patterns, route middleware, and layouts**, see the **nuxt-dev-guidelines** skill (`references/routing-navigation.md`).

---

## Components

### Auto-Import Convention

All components in `app/components/` are automatically imported and available globally.

```
components/
├── Button.vue              → <Button />
├── ui/
│   ├── Card.vue           → <UiCard />
│   └── Modal.vue          → <UiModal />
└── UserProfile.vue         → <UserProfile />
```

### Component Structure

**Reusable Components** (`components/`):

- No business logic
- Props-driven
- Emit events for parent handling
- Fully reusable

**Page Components** (`pages/`):

- Business logic
- Data fetching
- State management
- Page-specific functionality

### Nuxt UI Components

This project uses **Nuxt UI** for components:

- Pre-built accessible components
- Built on Tailwind CSS
- Fully typed with TypeScript
- Dark mode support

**Example:**

```vue
<template>
  <UButton color="primary" @click="handleClick"> Click Me </UButton>

  <UCard>
    <template #header>
      <h3>Card Title</h3>
    </template>
    <p>Card content</p>
  </UCard>
</template>
```

---

## Data Fetching

**For complete data fetching patterns**, see the **nuxt-dev-guidelines** skill (`references/data-fetching.md`).

**Quick reference:**

- Use `useFetch('/api/endpoint')` for API calls
- Use `useAsyncData('key', async () => {})` for custom async logic
- Both return `{ data, pending, error, refresh }`

---

## State Management

**For state management patterns**, see the **nuxt-dev-guidelines** skill.

**Quick reference:**

- Use `useState('key', () => initialValue)` for global state (replaces Pinia/Vuex)
- Use `callOnce(async () => {})` for one-time initialization across SSR and client

---

## SEO and Meta Tags

**For complete SEO patterns and examples**, see the **nuxt-dev-guidelines** skill (`references/seo-meta.md`).

**Quick reference:**

- Use `useSeoMeta()` for SEO meta tags (title, description, OG, Twitter Card)
- Use `useHead()` for advanced head management (scripts, links, htmlAttrs)
- Use `definePageMeta()` for page-level configuration (layout, middleware)

---

## Server API Routes

**For server API routes, middleware, and Nitro utilities**, see the **nuxt-dev-guidelines** skill (`references/server-api.md`).

**Quick reference:**

- Routes in `server/api/` → automatically prefixed with `/api/`
- Routes in `server/routes/` → no `/api/` prefix
- Middleware in `server/middleware/` → runs on every request
- Use `defineEventHandler()`, `readBody()`, `getCookie()`, `createError()`

---

## Configuration

### nuxt.config.ts

```typescript
export default defineNuxtConfig({
  // Compatibility date
  compatibilityDate: "2025-01-15",

  // Modules
  modules: [
    "@nuxt/eslint",
    "@nuxt/ui",
    "@nuxt/a11y",
    "@nuxt/hints",
    "@nuxt/image",
    "@nuxt/test-utils"
  ],

  // Dev tools
  devtools: {
    enabled: true
  },

  // Global CSS
  css: ["~/assets/css/main.css"],

  // Route rules
  routeRules: {
    "/": { prerender: true },
    "/api/**": { cors: true }
  },

  // Runtime config (env variables)
  runtimeConfig: {
    // Private (server-only)
    apiSecret: process.env.API_SECRET,

    // Public (client + server)
    public: {
      apiBase: process.env.API_BASE_URL || "http://localhost:3000"
    }
  },

  // TypeScript
  typescript: {
    strict: true,
    typeCheck: true
  }
})
```

### app.config.ts

**Reactive app-level configuration:**

```typescript
export default defineAppConfig({
  ui: {
    primary: "green",
    gray: "slate"
  },
  theme: {
    darkMode: true
  }
})

// Access in components
const appConfig = useAppConfig()
```

---

## Best Practices

### Performance

✅ **DO**: Use SSR for SEO-critical pages

```typescript
// Enabled by default
const { data } = await useFetch("/api/data")
```

✅ **DO**: Use lazy loading for non-critical data

```typescript
const { data } = await useFetch("/api/data", { lazy: true })
```

✅ **DO**: Prefetch on hover

```vue
<NuxtLink to="/page" prefetch>Link</NuxtLink>
```

❌ **DON'T**: Disable SSR for SEO pages

```typescript
// Bad for SEO
const { data } = await useFetch("/api/data", { server: false })
```

### TypeScript

✅ **DO**: Type your composables

```typescript
interface User {
  id: number
  name: string
}

export const useUser = () => {
  const user = useState<User | null>("user", () => null)
  return { user }
}
```

✅ **DO**: Type API responses

```typescript
const { data } = await useFetch<User[]>("/api/users")
```

### SEO

✅ **DO**: Use `useSeoMeta` for all SEO meta tags
✅ **DO**: Set unique titles and descriptions per page
✅ **DO**: Include Open Graph and Twitter Card tags
✅ **DO**: Use semantic HTML

---

## Skills Reference

### Available Skills

- **nuxt-dev-guidelines** - Nuxt 4 comprehensive development guide with SSR, data fetching, SEO, and best practices
- **frontend-dev-guidelines** - Vue 3 frontend development with TypeScript, Composition API, TanStack Query, Vue Router, Tailwind CSS, and ShadCN components
- **tailwindcss** - Tailwind CSS v4 utility-first styling (via Nuxt UI)

---

**Version**: 2.0
**Last Updated**: January 2026
**Framework**: Nuxt 4
