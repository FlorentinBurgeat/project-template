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

### Navigation

```vue
<script setup lang="ts">
const router = useRouter()
const route = useRoute()

// Programmatic navigation
const goToUser = (id: number) => {
  navigateTo(`/users/${id}`)
}

// With query params
navigateTo({
  path: '/users',
  query: { page: 1 }
})

// Get current route params
const userId = route.params.id
</script>

<template>
  <!-- Declarative navigation -->
  <NuxtLink to="/about">About</NuxtLink>
  <NuxtLink :to="`/users/${userId}`">User Profile</NuxtLink>
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

---

## Data Fetching

### useFetch - Recommended

**Primary method for fetching data from API endpoints:**

```vue
<script setup lang="ts">
// Basic usage
const { data, pending, error, refresh } = await useFetch('/api/users')

// With options
const { data } = await useFetch('/api/users', {
  method: 'GET',
  query: { page: 1, limit: 10 },
  headers: {
    'Authorization': 'Bearer token'
  },
  // Transform response
  transform: (data) => data.map(u => ({
    ...u,
    fullName: `${u.firstName} ${u.lastName}`
  })),
  // Pick specific fields
  pick: ['id', 'name', 'email'],
  // Lazy load (don't block navigation)
  lazy: false,
  // Server-only fetch
  server: true
})

// Reactive parameters
const page = ref(1)
const { data } = await useFetch('/api/users', {
  query: { page }
})

// Manual refresh
await refresh()
</script>

<template>
  <div>
    <div v-if="pending">Loading...</div>
    <div v-else-if="error">Error: {{ error.message }}</div>
    <div v-else>
      <div v-for="user in data" :key="user.id">
        {{ user.name }}
      </div>
    </div>
  </div>
</template>
```

### useAsyncData - Custom Logic

**For complex data processing:**

```vue
<script setup lang="ts">
const { data } = await useAsyncData('users', async () => {
  const response = await $fetch('/api/users')
  return response.filter(user => user.active)
})

// With key for deduplication
const { data } = await useAsyncData(`user-${id}`, () =>
  $fetch(`/api/users/${id}`)
)
</script>
```

---

## State Management

### useState - Global State

**Built-in composable for shared state (replaces Pinia/Vuex):**

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

### useSeoMeta - Primary Method

**Type-safe SEO meta tag management:**

```vue
<script setup lang="ts">
useSeoMeta({
  title: 'My Page Title',
  description: 'Page description for search engines',
  ogTitle: 'My Page Title',
  ogDescription: 'Page description for social sharing',
  ogImage: 'https://example.com/image.png',
  ogUrl: 'https://example.com/page',
  twitterCard: 'summary_large_image',
  twitterTitle: 'My Page Title',
  twitterDescription: 'Page description for Twitter',
  twitterImage: 'https://example.com/image.png',
})

// Reactive meta tags
const title = ref('Dynamic Title')
useSeoMeta({
  title,
  description: () => `Description for ${title.value}`,
})
</script>
```

### useHead - Advanced Head Management

```vue
<script setup lang="ts">
useHead({
  title: 'My Page',
  titleTemplate: '%s | My Site',
  meta: [
    { name: 'description', content: 'Page description' },
    { name: 'keywords', content: 'nuxt, vue, ssr' }
  ],
  link: [
    { rel: 'canonical', href: 'https://example.com/page' }
  ],
  htmlAttrs: {
    lang: 'en'
  }
})
</script>
```

### definePageMeta - Page Configuration

```vue
<script setup lang="ts">
definePageMeta({
  title: 'User Profile',
  layout: 'default',
  middleware: ['auth']
})
</script>
```

---

## Server API Routes

### API Routes (`server/api/`)

**Automatically prefixed with `/api/`:**

```typescript
// server/api/users.ts → /api/users
export default defineEventHandler(async (event) => {
  // GET all users
  return { users: [] }
})

// server/api/users/[id].ts → /api/users/:id
export default defineEventHandler(async (event) => {
  const id = getRouterParam(event, 'id')

  if (event.method === 'GET') {
    return { user: { id } }
  }

  if (event.method === 'PUT') {
    const body = await readBody(event)
    return { updated: true, data: body }
  }

  throw createError({
    statusCode: 405,
    message: 'Method not allowed'
  })
})

// server/api/auth/login.ts → /api/auth/login
export default defineEventHandler(async (event) => {
  const body = await readBody(event)

  // Validate credentials
  const user = await validateCredentials(body)

  if (!user) {
    throw createError({
      statusCode: 401,
      message: 'Invalid credentials'
    })
  }

  // Set auth cookie
  setCookie(event, 'auth_token', user.token, {
    httpOnly: true,
    secure: true,
    maxAge: 60 * 60 * 24 * 7 // 7 days
  })

  return { user }
})
```

### Server Middleware (`server/middleware/`)

**Runs on every request:**

```typescript
// server/middleware/auth.ts
export default defineEventHandler((event) => {
  const token = getCookie(event, 'auth_token')

  if (!token && !event.path.startsWith('/api/public')) {
    throw createError({
      statusCode: 401,
      message: 'Unauthorized'
    })
  }
})

// server/middleware/log.ts
export default defineEventHandler((event) => {
  console.log(`${event.method} ${event.path}`)
})
```

---

## Layouts

### Default Layout

```vue
<!-- layouts/default.vue -->
<template>
  <div>
    <header>
      <nav>
        <NuxtLink to="/">Home</NuxtLink>
        <NuxtLink to="/about">About</NuxtLink>
      </nav>
    </header>
    <main>
      <slot /> <!-- Page content -->
    </main>
    <footer>
      <p>&copy; 2026 My App</p>
    </footer>
  </div>
</template>
```

### Custom Layouts

```vue
<!-- layouts/admin.vue -->
<template>
  <div class="admin-layout">
    <aside>Admin Sidebar</aside>
    <main>
      <slot />
    </main>
  </div>
</template>

<!-- pages/admin/dashboard.vue -->
<script setup lang="ts">
definePageMeta({
  layout: 'admin'
})
</script>
```

---

## Configuration

### nuxt.config.ts

```typescript
export default defineNuxtConfig({
  // Compatibility date
  compatibilityDate: '2025-01-15',

  // Modules
  modules: [
    '@nuxt/eslint',
    '@nuxt/ui',
    '@nuxt/a11y',
    '@nuxt/hints',
    '@nuxt/image',
    '@nuxt/test-utils'
  ],

  // Dev tools
  devtools: {
    enabled: true
  },

  // Global CSS
  css: ['~/assets/css/main.css'],

  // Route rules
  routeRules: {
    '/': { prerender: true },
    '/api/**': { cors: true }
  },

  // Runtime config (env variables)
  runtimeConfig: {
    // Private (server-only)
    apiSecret: process.env.API_SECRET,

    // Public (client + server)
    public: {
      apiBase: process.env.API_BASE_URL || 'http://localhost:3000'
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

---

## Auto-Imports

Nuxt automatically imports:
- **Components** from `components/`
- **Composables** from `composables/`
- **Utils** from `utils/`
- **Vue APIs** (ref, computed, watch, etc.)
- **Nuxt APIs** (useFetch, useState, navigateTo, etc.)

**No explicit imports needed:**
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

## Best Practices

### Performance

✅ **DO**: Use SSR for SEO-critical pages
```typescript
// Enabled by default
const { data } = await useFetch('/api/data')
```

✅ **DO**: Use lazy loading for non-critical data
```typescript
const { data } = await useFetch('/api/data', { lazy: true })
```

✅ **DO**: Prefetch on hover
```vue
<NuxtLink to="/page" prefetch>Link</NuxtLink>
```

❌ **DON'T**: Disable SSR for SEO pages
```typescript
// Bad for SEO
const { data } = await useFetch('/api/data', { server: false })
```

### TypeScript

✅ **DO**: Type your composables
```typescript
interface User {
  id: number
  name: string
}

export const useUser = () => {
  const user = useState<User | null>('user', () => null)
  return { user }
}
```

✅ **DO**: Type API responses
```typescript
const { data } = await useFetch<User[]>('/api/users')
```

### SEO

✅ **DO**: Use `useSeoMeta` for all SEO meta tags
✅ **DO**: Set unique titles and descriptions per page
✅ **DO**: Include Open Graph and Twitter Card tags
✅ **DO**: Use semantic HTML

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
  error: {
    statusCode: number
    message: string
  }
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

## Skills Reference

### Available Skills

- **nuxt-dev-guidelines** - Nuxt 4 comprehensive development guide with SSR, data fetching, SEO, and best practices
- **tailwindcss** - Tailwind CSS v4 utility-first styling (via Nuxt UI)

---

**Version**: 2.0
**Last Updated**: January 2026
**Framework**: Nuxt 4
