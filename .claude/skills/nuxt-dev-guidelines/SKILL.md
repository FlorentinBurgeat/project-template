---
name: nuxt-dev-guidelines
description: Nuxt 4 frontend development guidelines with TypeScript, Composition API, file-based routing, auto-imports, server-side rendering, data fetching with useFetch and useAsyncData, SEO and meta tag management with useHead and useSeoMeta, Nitro server engine, API routes, Nuxt UI component integration with MCP nuxt-ui-remote, and best practices for building performant full-stack web applications.
---

# Nuxt 4 Development Guidelines

## Purpose

Comprehensive guide for building full-stack web applications with Nuxt 4, covering framework conventions, directory structure, composables, SSR, API routes, SEO optimization, and production best practices.

## When to Use This Skill

Use when:
- Creating Nuxt 4 applications or pages
- Working with .vue files in Nuxt projects
- Building server API routes with Nitro
- Implementing SSR or static generation
- Managing SEO and meta tags
- Setting up data fetching with composables
- Configuring Nuxt modules and plugins
- Working with file-based routing
- Creating middleware or layouts

---

## Core Philosophy

### Convention Over Configuration

Nuxt uses an opinionated directory structure to automate repetitive tasks:
- **File-based routing** from `pages/` directory
- **Auto-imports** for components, composables, and utilities
- **Automatic code splitting** for optimal performance
- **Zero-config TypeScript** with auto-generated types
- **Built-in SSR** out of the box

### Full-Stack Framework

Nuxt provides both frontend and backend functionality:
- **Vue 3** for reactive UI with Composition API
- **Nitro** for server engine and API routes
- **h3** for HTTP handling
- **Multiple rendering modes** (SSR, SSG, SPA, hybrid)

---

## Directory Structure

### Essential Directories

```
project/
├── app/                      # Nuxt 4 app directory (optional)
│   ├── pages/               # File-based routing
│   ├── components/          # Auto-imported components
│   ├── composables/         # Auto-imported composition functions
│   ├── layouts/             # Page layouts
│   ├── middleware/          # Route middleware
│   ├── plugins/             # Vue plugins
│   ├── utils/               # Auto-imported utility functions
│   ├── assets/              # Build-time assets
│   ├── app.config.ts        # App-level configuration
│   └── app.vue              # Root component
├── server/                  # Nitro server directory
│   ├── api/                # API routes (/api/*)
│   ├── routes/             # Server routes (no /api prefix)
│   ├── middleware/         # Server middleware
│   ├── plugins/            # Server plugins
│   └── utils/              # Server-only utilities
├── public/                  # Static assets
├── nuxt.config.ts          # Nuxt configuration
└── package.json
```

### Key Directory Behaviors

**`pages/`** - File-based routing:
```
pages/
├── index.vue              → /
├── about.vue              → /about
├── users/
│   ├── index.vue         → /users
│   ├── [id].vue          → /users/:id (dynamic)
│   └── profile.vue       → /users/profile
└── [...slug].vue          → Catch-all route
```

**`components/`** - Auto-imported components:
```
components/
├── Button.vue             → <Button />
├── ui/
│   └── Card.vue          → <UiCard />
└── UserProfile.vue        → <UserProfile />
```

**`composables/`** - Auto-imported composables:
```
composables/
├── useAuth.ts            → useAuth()
└── useUser.ts            → useUser()
```

---

## Data Fetching

Primary composables: `useFetch` and `useAsyncData`.

### Quick Reference

```vue
<script setup lang="ts">
// Basic fetch
const { data, pending, error, refresh } = await useFetch('/api/users')

// With options
const { data } = await useFetch('/api/users', {
  method: 'GET',
  query: { page: 1 },
  transform: (data) => data.map(u => u.name),
  lazy: false,
  server: true
})

// Custom async logic
const { data } = await useAsyncData('users', async () => {
  const response = await $fetch('/api/users')
  return response.filter(user => user.active)
})
</script>

<template>
  <div v-if="pending">Loading...</div>
  <div v-else-if="error">Error: {{ error.message }}</div>
  <div v-else>{{ data }}</div>
</template>
```

**For detailed examples and all options**, see [references/data-fetching.md](references/data-fetching.md).

---

## State Management

### useState - Global State

Built-in composable for shared state (replaces Pinia/Vuex):

```typescript
// composables/useAuth.ts
export const useAuth = () => {
  const user = useState<User | null>('user', () => null)
  const isAuthenticated = computed(() => !!user.value)

  const login = async (credentials: LoginDTO) => {
    const response = await $fetch('/api/auth/login', {
      method: 'POST',
      body: credentials
    })
    user.value = response.user
  }

  const logout = () => {
    user.value = null
  }

  return {
    user: readonly(user),
    isAuthenticated,
    login,
    logout
  }
}

// Use in any component
<script setup lang="ts">
const { user, login, logout } = useAuth()
</script>
```

### callOnce - Run Code Once

```typescript
<script setup lang="ts">
// Runs once across SSR and client
await callOnce(async () => {
  await initializeApp()
})
</script>
```

---

## SEO and Meta Tags

Use `useSeoMeta` for most SEO needs, `useHead` for advanced customization.

### Quick Reference

```vue
<script setup lang="ts">
// Recommended: useSeoMeta for SEO
useSeoMeta({
  title: 'My Page',
  description: 'Page description',
  ogTitle: 'My Page',
  ogDescription: 'Page description',
  ogImage: 'https://example.com/image.png',
  twitterCard: 'summary_large_image',
})

// Advanced: useHead for complex head management
useHead({
  title: 'My Page',
  titleTemplate: '%s | My Site',
  meta: [
    { name: 'keywords', content: 'nuxt, vue' }
  ],
  link: [
    { rel: 'canonical', href: 'https://example.com/page' }
  ]
})

// Page-level configuration
definePageMeta({
  title: 'User Profile',
  layout: 'default',
  middleware: ['auth']
})
</script>
```

**For complete SEO examples and all meta tag options**, see [references/seo-meta.md](references/seo-meta.md).

---

## Server API Routes

Routes in `server/api/` are automatically prefixed with `/api/`.

### Quick Reference

```typescript
// server/api/users.ts → /api/users
export default defineEventHandler(async (event) => {
  return { users: [] }
})

// server/api/users/[id].ts → /api/users/:id
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')

  if (event.method === 'GET') {
    return { user: { id } }
  }

  if (event.method === 'POST') {
    const body = await readBody(event)
    return { created: true, data: body }
  }

  throw createError({
    statusCode: 405,
    message: 'Method not allowed'
  })
})
```

### Common Nitro Utilities

```typescript
// Request handling
const body = await readBody(event)
const query = getQuery(event)
const id = getRouterParam(event, 'id')
const auth = getHeader(event, 'authorization')

// Response handling
setHeader(event, 'x-custom', 'value')
throw createError({ statusCode: 404, message: 'Not found' })

// Cookies
const token = getCookie(event, 'token')
setCookie(event, 'token', 'value', { httpOnly: true })
```

**For complete API examples, middleware patterns, and all Nitro utilities**, see [references/server-api.md](references/server-api.md).

---

## Routing and Navigation

File-based routing from `pages/` directory.

### Quick Reference

```vue
<script setup lang="ts">
// Programmatic navigation
const router = useRouter()
const route = useRoute()

navigateTo('/users/123')
navigateTo({ path: '/users', query: { page: 1 } })
router.back()

// Get current route info
const userId = route.params.id
const page = route.query.page
</script>

<template>
  <!-- Declarative navigation -->
  <NuxtLink to="/about">About</NuxtLink>
  <NuxtLink :to="`/users/${userId}`">User</NuxtLink>
</template>
```

### Route Middleware

```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware((to, from) => {
  const user = useUser()

  if (!user.value && to.path !== '/login') {
    return navigateTo('/login')
  }
})

// Use in page
<script setup lang="ts">
definePageMeta({
  middleware: ['auth']
})
</script>
```

**For layouts, dynamic routes, and advanced navigation patterns**, see [references/routing-navigation.md](references/routing-navigation.md).

---

## Configuration

### nuxt.config.ts - Primary Config

```typescript
export default defineNuxtConfig({
  compatibilityDate: '2025-01-15',

  modules: [
    '@nuxt/eslint',
    '@nuxt/ui'
  ],

  runtimeConfig: {
    // Private (server-only)
    apiSecret: process.env.API_SECRET,

    // Public (client + server)
    public: {
      apiBase: process.env.API_BASE_URL
    }
  },

  routeRules: {
    '/': { prerender: true },
    '/api/**': { cors: true }
  },

  typescript: {
    strict: true,
    typeCheck: true
  }
})
```

### app.config.ts - Reactive App Config

```typescript
export default defineAppConfig({
  ui: {
    primary: 'green',
    gray: 'slate'
  },
  theme: {
    darkMode: true
  }
})

// Access in components
const appConfig = useAppConfig()
```

**For complete configuration options, environment variables, and route rules**, see [references/configuration.md](references/configuration.md).

---

## Auto-Imports

Nuxt automatically imports:
- **Components** from `components/`
- **Composables** from `composables/`
- **Utils** from `utils/`
- **Vue APIs** (ref, computed, watch, etc.)
- **Nuxt APIs** (useFetch, useState, navigateTo, etc.)

No explicit imports needed:
```vue
<script setup lang="ts">
// All auto-imported, no import statements
const count = ref(0)
const doubled = computed(() => count.value * 2)
const { data } = await useFetch('/api/users')
const router = useRouter()
</script>
```

---

## Component Structure

### Reusable Components (`components/`)

- No business logic
- Props-driven
- Emit events for parent handling
- Fully reusable across pages

### Page Components (`pages/`)

- Business logic
- Data fetching
- State management
- Page-specific functionality

### Nuxt UI Components

This project uses **Nuxt UI** (@nuxt/ui):
- Pre-built accessible components
- Built on Tailwind CSS
- Fully typed with TypeScript
- Dark mode support

#### Basic Usage

```vue
<template>
  <UButton color="primary" @click="handleClick">
    Click Me
  </UButton>

  <UCard>
    <template #header>
      <h3>Card Title</h3>
    </template>
    <p>Card content</p>
  </UCard>
</template>
```

#### Using MCP nuxt-ui-remote for Documentation

The **nuxt-ui-remote** MCP server provides access to official Nuxt UI documentation and component metadata. Use these tools when you need detailed component information beyond basic usage.

**When to Use MCP Tools:**
- Learning how to use a new Nuxt UI component
- Finding all available props, slots, and events for a component
- Searching for components by category or use case
- Looking for example implementations
- Understanding component theming and customization

**Available MCP Tools:**

| Tool | Purpose | Example |
|------|---------|---------|
| `list-components` | Browse all available Nuxt UI components | Get complete component catalog |
| `get-component` | Fetch full component documentation | `{ componentName: "Button" }` |
| `get-component-metadata` | Get detailed props/slots/events | `{ componentName: "Button" }` |
| `search-components-by-category` | Find components by category | `{ category: "forms" }` |
| `list-examples` | Browse UI example implementations | Get all available examples |
| `get-example` | Fetch specific example code | `{ exampleName: "ButtonExample" }` |
| `get-documentation-page` | Fetch documentation pages | `{ path: "/docs/components/button" }` |

**Usage Examples:**

```typescript
// Get complete Button component documentation
mcp__nuxt-ui-remote__get-component({
  componentName: "Button",
  sections: ["usage", "api", "examples"] // Optional: filter sections
})

// Get detailed component metadata (props, slots, events)
mcp__nuxt-ui-remote__get-component-metadata({
  componentName: "Button"
})

// Search for all form components
mcp__nuxt-ui-remote__search-components-by-category({
  category: "forms"
})

// List all available components
mcp__nuxt-ui-remote__list-components()
```

**Best Practices:**
- Use `get-component-metadata` when you need exact prop types and available slots
- Use `get-component` with specific sections to reduce response size
- Use `search-components-by-category` to discover components for specific use cases
- For simple usage (buttons, cards), basic documentation may be sufficient

---

## Error Handling

### Client Errors

```vue
<script setup lang="ts">
const { data, error } = await useFetch('/api/data')

if (error.value) {
  console.error('Fetch failed:', error.value)
}
</script>
```

### Server Errors

```typescript
// server/api/users.ts
export default defineEventHandler(async (event) => {
  try {
    return await fetchUsers()
  } catch (error) {
    throw createError({
      statusCode: 500,
      message: 'Failed to fetch users',
      cause: error
    })
  }
})
```

### Error Page

```vue
<!-- error.vue in root -->
<script setup lang="ts">
const props = defineProps<{
  error: { statusCode: number; message: string }
}>()

const handleClear = () => clearError({ redirect: '/' })
</script>

<template>
  <div>
    <h1>{{ error.statusCode }}</h1>
    <p>{{ error.message }}</p>
    <button @click="handleClear">Go Home</button>
  </div>
</template>
```

---

## Best Practices

### Performance

✅ **DO**: Use SSR for SEO-critical pages (enabled by default)
✅ **DO**: Use lazy loading for non-critical data: `lazy: true`
✅ **DO**: Prefetch on hover: `<NuxtLink prefetch>`
✅ **DO**: Use static generation for content pages

❌ **DON'T**: Disable SSR for SEO pages
❌ **DON'T**: Fetch client-side only for above-the-fold content

### TypeScript

✅ **DO**: Type your composables and API responses
```typescript
interface User {
  id: number
  name: string
}

const { data } = await useFetch<User[]>('/api/users')
```

### SEO

✅ **DO**: Use `useSeoMeta` for all SEO meta tags
✅ **DO**: Set unique titles and descriptions per page
✅ **DO**: Include Open Graph and Twitter Card tags
✅ **DO**: Use semantic HTML

---

## Common Patterns

### Protected Routes

```typescript
// middleware/auth.global.ts
export default defineNuxtRouteMiddleware((to) => {
  const user = useUser()

  if (!user.value && to.path.startsWith('/dashboard')) {
    return navigateTo('/login')
  }
})
```

### API Proxy

```typescript
// server/api/proxy/[...path].ts
export default defineEventHandler(async (event) => {
  const path = event.path.replace('/api/proxy', '')
  const config = useRuntimeConfig()

  return $fetch(`${config.backendUrl}${path}`, {
    method: event.method,
    headers: getHeaders(event),
    body: event.method !== 'GET' ? await readBody(event) : undefined
  })
})
```

---

## Quick Reference

### Essential Composables

| Composable | Purpose |
|------------|---------|
| `useFetch` | Fetch data from API |
| `useAsyncData` | Custom async operations |
| `useState` | Shared state |
| `useSeoMeta` | SEO meta tags |
| `useHead` | Advanced head management |
| `useRoute` | Current route info |
| `useRouter` | Programmatic navigation |
| `useCookie` | Cookie management |
| `callOnce` | Run once (SSR + client) |

### Nitro Helpers

| Helper | Purpose |
|--------|---------|
| `defineEventHandler` | Create API handler |
| `readBody` | Parse request body |
| `getQuery` | Get query params |
| `getCookie` | Get cookie value |
| `createError` | Throw HTTP error |

---

## Reference Files

For detailed examples and comprehensive guides:

- **[data-fetching.md](references/data-fetching.md)** - Complete useFetch/useAsyncData guide with all options
- **[seo-meta.md](references/seo-meta.md)** - SEO optimization, Open Graph, Twitter Cards
- **[server-api.md](references/server-api.md)** - API routes, middleware, Nitro utilities, CRUD examples
- **[routing-navigation.md](references/routing-navigation.md)** - File-based routing, middleware, layouts, navigation guards
- **[configuration.md](references/configuration.md)** - nuxt.config.ts, app.config.ts, environment variables, route rules

---

**Skill Version**: 2.1
**Last Updated**: January 26, 2026
**Framework Version**: Nuxt 4.x
**MCP Integration**: nuxt-ui-remote for component documentation
**Line Count**: ~680 lines (includes MCP nuxt-ui-remote documentation)
