# Routing and Navigation Reference

## File-Based Routing

Nuxt automatically generates routes from the `pages/` (or `app/pages/`) directory structure.

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

### Dynamic Route Parameters

```vue
<!-- pages/users/[id].vue -->
<script setup lang="ts">
const route = useRoute()
const userId = route.params.id // Get the :id parameter
</script>
```

### Catch-All Routes

```vue
<!-- pages/blog/[...slug].vue -->
<script setup lang="ts">
const route = useRoute()
// /blog/2024/01/my-post → slug = ['2024', '01', 'my-post']
const slug = route.params.slug
</script>
```

## Navigation

### Declarative Navigation

```vue
<template>
  <!-- Basic link -->
  <NuxtLink to="/about">About</NuxtLink>

  <!-- Dynamic link -->
  <NuxtLink :to="`/users/${userId}`">User Profile</NuxtLink>

  <!-- With query params -->
  <NuxtLink to="/search?q=nuxt">Search</NuxtLink>

  <!-- Named route -->
  <NuxtLink :to="{ name: 'user-id', params: { id: 123 } }">
    User 123
  </NuxtLink>

  <!-- With prefetch -->
  <NuxtLink to="/page" prefetch>Link with prefetch</NuxtLink>

  <!-- External link -->
  <NuxtLink to="https://nuxt.com" external>Nuxt Docs</NuxtLink>
</template>
```

### Programmatic Navigation

```vue
<script setup lang="ts">
const router = useRouter()
const route = useRoute()

// Navigate to path
const goToUser = (id: number) => {
  navigateTo(`/users/${id}`)
}

// With query params
navigateTo({
  path: '/users',
  query: { page: 1 }
})

// Named routes
navigateTo({ name: 'user-id', params: { id: 123 } })

// Navigate back
router.back()

// Navigate forward
router.forward()

// Replace (no history entry)
navigateTo('/new-path', { replace: true })

// External redirect
navigateTo('https://nuxt.com', { external: true })

// Get current route info
const currentPath = route.path
const queryParams = route.query
const routeParams = route.params
</script>
```

## Route Middleware

### Global Middleware

```typescript
// middleware/auth.global.ts
export default defineNuxtRouteMiddleware((to, from) => {
  const user = useUser()

  if (!user.value && to.path.startsWith('/dashboard')) {
    return navigateTo('/login')
  }
})
```

### Named Middleware

```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware((to, from) => {
  const user = useUser()

  if (!user.value) {
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

### Inline Middleware

```vue
<script setup lang="ts">
definePageMeta({
  middleware: (to, from) => {
    if (to.params.id === '1') {
      return abortNavigation()
    }
  }
})
</script>
```

### Multiple Middleware

```vue
<script setup lang="ts">
definePageMeta({
  middleware: ['auth', 'admin', 'subscription']
})
</script>
```

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
      <slot /> <!-- Page content renders here -->
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
    <aside>
      <nav>Admin Navigation</nav>
    </aside>
    <main>
      <slot />
    </main>
  </div>
</template>
```

### Using Layouts in Pages

```vue
<!-- pages/admin/dashboard.vue -->
<script setup lang="ts">
definePageMeta({
  layout: 'admin'
})
</script>

<template>
  <div>Dashboard content</div>
</template>
```

### No Layout

```vue
<script setup lang="ts">
definePageMeta({
  layout: false
})
</script>
```

### Dynamic Layout

```vue
<script setup lang="ts">
const route = useRoute()

definePageMeta({
  layout: computed(() => route.query.admin ? 'admin' : 'default')
})
</script>
```

## Route Configuration

### definePageMeta Options

```vue
<script setup lang="ts">
definePageMeta({
  // Layout
  layout: 'admin',

  // Middleware
  middleware: ['auth', 'admin'],

  // Keep alive
  keepalive: true,

  // Custom properties (accessible in middleware)
  requiresAuth: true,
  roles: ['admin']
})
</script>
```

### Accessing Page Meta in Middleware

```typescript
// middleware/auth.ts
export default defineNuxtRouteMiddleware((to) => {
  if (to.meta.requiresAuth) {
    const user = useUser()
    if (!user.value) {
      return navigateTo('/login')
    }
  }
})
```

## Error Pages

### Custom Error Page

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

### Throwing Errors in Pages

```vue
<script setup lang="ts">
const { data } = await useFetch('/api/user')

if (!data.value) {
  throw createError({
    statusCode: 404,
    message: 'User not found'
  })
}
</script>
```

## Navigation Guards Pattern

### Protected Routes

```typescript
// middleware/auth.global.ts
export default defineNuxtRouteMiddleware((to) => {
  const publicPages = ['/login', '/register', '/']
  const authRequired = !publicPages.includes(to.path)
  const user = useUser()

  if (authRequired && !user.value) {
    return navigateTo('/login')
  }
})
```

### Role-Based Access

```typescript
// middleware/admin.ts
export default defineNuxtRouteMiddleware(() => {
  const user = useUser()

  if (!user.value?.roles.includes('admin')) {
    throw createError({
      statusCode: 403,
      message: 'Access denied'
    })
  }
})
```
