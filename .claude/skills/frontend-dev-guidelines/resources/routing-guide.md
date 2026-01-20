# Routing Guide

Vue Router implementation with centralized route configuration and lazy loading patterns.

---

## Vue Router Overview

**Vue Router** provides:
- Centralized route configuration
- Lazy loading for code splitting
- Type-safe routing with TypeScript
- Navigation guards for authentication
- Route meta fields

---

## Router Configuration

### Basic Setup

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/pages/HomePage.vue'),
    meta: {
      title: 'Home',
      requiresAuth: false
    }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: {
      title: 'Profile',
      requiresAuth: true
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

---

## Route Patterns

### Simple Route

```typescript
{
  path: '/about',
  name: 'about',
  component: () => import('@/pages/AboutPage.vue')
}
```

### Dynamic Parameters

```typescript
// Single parameter
{
  path: '/users/:id',
  name: 'user-detail',
  component: () => import('@/pages/UserDetailPage.vue')
}

// Multiple parameters
{
  path: '/posts/:postId/comments/:commentId',
  name: 'comment-detail',
  component: () => import('@/pages/CommentDetailPage.vue')
}

// Optional parameter
{
  path: '/posts/:id?',
  name: 'posts',
  component: () => import('@/pages/PostsPage.vue')
}
```

**Accessing parameters:**
```vue
<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// Access parameter
const userId = route.params.id
</script>
```

### Query Parameters

```typescript
// Route: /search?q=vue&category=tutorials
{
  path: '/search',
  name: 'search',
  component: () => import('@/pages/SearchPage.vue')
}
```

**Accessing query params:**
```vue
<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// Access query parameters
const searchQuery = route.query.q       // 'vue'
const category = route.query.category   // 'tutorials'
</script>
```

---

## Nested Routes

### Parent-Child Routes

```typescript
{
  path: '/dashboard',
  component: () => import('@/pages/DashboardLayout.vue'),
  children: [
    {
      path: '',
      name: 'dashboard-home',
      component: () => import('@/pages/dashboard/DashboardHome.vue')
    },
    {
      path: 'settings',
      name: 'dashboard-settings',
      component: () => import('@/pages/dashboard/DashboardSettings.vue')
    },
    {
      path: 'users',
      name: 'dashboard-users',
      component: () => import('@/pages/dashboard/DashboardUsers.vue')
    }
  ]
}
```

**Parent Layout:**
```vue
<!-- pages/DashboardLayout.vue -->
<script setup lang="ts">
</script>

<template>
  <div class="dashboard-layout">
    <nav class="sidebar">
      <router-link to="/dashboard">Home</router-link>
      <router-link to="/dashboard/settings">Settings</router-link>
      <router-link to="/dashboard/users">Users</router-link>
    </nav>

    <main class="content">
      <!-- Child routes render here -->
      <router-view />
    </main>
  </div>
</template>
```

---

## Navigation

### Programmatic Navigation

```vue
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

// Navigate to route by name
function goToProfile() {
  router.push({ name: 'profile' })
}

// Navigate with parameters
function goToUser(userId: number) {
  router.push({
    name: 'user-detail',
    params: { id: userId }
  })
}

// Navigate with query parameters
function searchPosts(query: string) {
  router.push({
    name: 'search',
    query: { q: query }
  })
}

// Go back
function goBack() {
  router.back()
}

// Replace current route (no history entry)
function replaceRoute() {
  router.replace({ name: 'home' })
}
</script>

<template>
  <div>
    <button @click="goToProfile">Go to Profile</button>
    <button @click="goToUser(123)">View User 123</button>
    <button @click="goBack">Go Back</button>
  </div>
</template>
```

### Declarative Navigation

```vue
<template>
  <!-- Basic link -->
  <router-link to="/about">About</router-link>

  <!-- Named route -->
  <router-link :to="{ name: 'profile' }">Profile</router-link>

  <!-- With parameters -->
  <router-link :to="{ name: 'user-detail', params: { id: 123 } }">
    User 123
  </router-link>

  <!-- With query parameters -->
  <router-link :to="{ name: 'search', query: { q: 'vue' } }">
    Search Vue
  </router-link>

  <!-- Active class styling -->
  <router-link
    to="/dashboard"
    active-class="text-blue-500 font-bold"
  >
    Dashboard
  </router-link>
</template>
```

---

## Route Guards

### Global Before Guard (Authentication)

```typescript
// router/index.ts
import { useAuthState } from '@/composables/useAuthState'

router.beforeEach((to, from, next) => {
  const { isAuthenticated } = useAuthState()

  // Check if route requires authentication
  if (to.meta.requiresAuth && !isAuthenticated.value) {
    // Redirect to login
    next({
      name: 'login',
      query: { redirect: to.fullPath } // Save intended destination
    })
  } else {
    next()
  }
})
```

### Per-Route Guards

```typescript
{
  path: '/admin',
  name: 'admin',
  component: () => import('@/pages/AdminPage.vue'),
  beforeEnter: (to, from, next) => {
    const { user } = useAuthState()

    if (user.value?.role === 'admin') {
      next()
    } else {
      next({ name: 'home' }) // Redirect non-admins
    }
  }
}
```

### Component Guards

```vue
<script setup lang="ts">
import { onBeforeRouteLeave } from 'vue-router'
import { ref } from 'vue'

const hasUnsavedChanges = ref(false)

// Warn before leaving if unsaved changes
onBeforeRouteLeave((to, from, next) => {
  if (hasUnsavedChanges.value) {
    const answer = window.confirm('You have unsaved changes. Leave anyway?')
    next(answer)
  } else {
    next()
  }
})
</script>
```

---

## Route Meta Fields

### Defining Meta

```typescript
{
  path: '/admin',
  name: 'admin',
  component: () => import('@/pages/AdminPage.vue'),
  meta: {
    title: 'Admin Panel',
    requiresAuth: true,
    requiresAdmin: true,
    breadcrumb: 'Administration'
  }
}
```

### Using Meta

```typescript
// Set page title from route meta
router.afterEach((to) => {
  document.title = to.meta.title as string || 'Default Title'
})
```

```vue
<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()

const breadcrumb = computed(() => route.meta.breadcrumb as string)
</script>

<template>
  <div class="breadcrumb">
    <router-link to="/">Home</router-link>
    <span v-if="breadcrumb"> / {{ breadcrumb }}</span>
  </div>
</template>
```

---

## Lazy Loading

### Component Lazy Loading

```typescript
// ✅ PREFERRED - Lazy load page components
{
  path: '/posts',
  name: 'posts',
  component: () => import('@/pages/PostsPage.vue') // Loaded only when route is visited
}

// ❌ AVOID - Eager loading
import PostsPage from '@/pages/PostsPage.vue'
{
  path: '/posts',
  name: 'posts',
  component: PostsPage // Loaded immediately with app
}
```

### Grouping Routes for Chunking

```typescript
// Group related routes into same chunk
{
  path: '/admin/users',
  component: () => import(/* webpackChunkName: "admin" */ '@/pages/admin/UsersPage.vue')
},
{
  path: '/admin/settings',
  component: () => import(/* webpackChunkName: "admin" */ '@/pages/admin/SettingsPage.vue')
}
```

---

## 404 and Error Routes

### Catch-All Route

```typescript
{
  path: '/:pathMatch(.*)*',
  name: 'not-found',
  component: () => import('@/pages/NotFoundPage.vue')
}
```

### Error Handling

```vue
<!-- pages/NotFoundPage.vue -->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen">
    <h1 class="text-4xl font-bold mb-4">404 - Page Not Found</h1>
    <p class="text-gray-600 mb-8">The page you're looking for doesn't exist.</p>
    <button @click="goHome" class="px-4 py-2 bg-blue-500 text-white rounded">
      Go Home
    </button>
  </div>
</template>
```

---

## Scroll Behavior

### Custom Scroll Behavior

```typescript
// router/index.ts
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // Restore saved position (back/forward)
    if (savedPosition) {
      return savedPosition
    }

    // Scroll to anchor
    if (to.hash) {
      return {
        el: to.hash,
        behavior: 'smooth'
      }
    }

    // Scroll to top
    return { top: 0 }
  }
})
```

---

## TypeScript Support

### Route Type Safety

```typescript
// router/types.ts
export interface RouteMeta {
  title?: string
  requiresAuth?: boolean
  requiresAdmin?: boolean
  breadcrumb?: string
}

declare module 'vue-router' {
  interface RouteMeta extends RouteMeta {}
}
```

### Typed Routes

```typescript
// Use route names with autocomplete
router.push({ name: 'user-detail' }) // Autocomplete available

// Type-safe params
interface UserDetailParams {
  id: string
}

function goToUser(params: UserDetailParams) {
  router.push({
    name: 'user-detail',
    params
  })
}
```

---

## Common Patterns

### Redirect After Login

```vue
<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { userApi } from '@/api/user'

const router = useRouter()
const route = useRoute()

async function handleLogin(credentials: Credentials) {
  await userApi.login(credentials)

  // Redirect to intended page or home
  const redirect = route.query.redirect as string || '/'
  router.push(redirect)
}
</script>
```

### Protected Routes

```typescript
// All admin routes require authentication and admin role
{
  path: '/admin',
  component: () => import('@/pages/admin/AdminLayout.vue'),
  meta: {
    requiresAuth: true,
    requiresAdmin: true
  },
  children: [
    {
      path: 'users',
      component: () => import('@/pages/admin/UsersPage.vue')
    },
    {
      path: 'settings',
      component: () => import('@/pages/admin/SettingsPage.vue')
    }
  ]
}
```

---

## Summary

**Vue Router Best Practices:**
1. **Lazy load** all route components with dynamic imports
2. **Use route names** for type-safe navigation
3. **Meta fields** for route-specific data (title, auth, breadcrumbs)
4. **Navigation guards** for authentication and authorization
5. **Nested routes** for layouts with shared UI
6. **Programmatic navigation** with `useRouter()`
7. **Access route data** with `useRoute()`
8. **TypeScript** for type-safe routing

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [loading-and-error-states.md](loading-and-error-states.md) - Handle navigation loading states
