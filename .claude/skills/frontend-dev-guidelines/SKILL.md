---
name: frontend-dev-guidelines
description: Vue 3 frontend development guidelines with TypeScript, Composition API, TanStack Query, Vue Router, Tailwind CSS, and ShadCN Vue components. Use when creating components, pages, features, fetching data, styling with Tailwind, routing, or working with .vue files, composition API patterns, and composable stores. Covers component design, state management with singleton composables, data fetching, performance optimization, and best practices for Vue 3 with Vite.
---

# Vue 3 Frontend Development Guidelines

## Purpose

Comprehensive guide for modern Vue 3 development, emphasizing Composition API, singleton composable stores, data fetching with TanStack Query, Tailwind CSS styling, and proper component design.

## When to Use This Skill

- Creating new Vue 3 components (`.vue` files)
- Building pages and features
- Fetching data with TanStack Query
- Setting up routing with Vue Router
- Working with singleton composable stores
- Styling with Tailwind CSS and ShadCN Vue
- Performance optimization
- Organizing frontend code with Composition API
- TypeScript best practices in Vue 3

---

## Quick Start

### New Component Checklist

Creating a Vue 3 component? Follow this checklist:

- [ ] Use `<script setup lang="ts">` syntax
- [ ] Define props with `withDefaults(defineProps<Props>(), { defaults })`
- [ ] Define emits with `defineEmits<{ eventName: [args] }>()`
- [ ] Use composables for shared logic and state
- [ ] Style with Tailwind CSS utility classes in `<template>`
- [ ] Import ShadCN Vue components from `@/components/ui`
- [ ] Use `computed()` for derived state
- [ ] Keep component logic minimal
- [ ] Add TypeScript interfaces for all props
- [ ] Lazy load with `defineAsyncComponent()` for heavy components

### New Page Checklist

Creating a new page? Follow this pattern:

- [ ] Create `.vue` page component with `<script setup lang="ts">`
- [ ] Create page-specific composables for local state if needed
- [ ] Create page-specific components in dedicated folder
- [ ] Create API service file if new feature
- [ ] Add types and DTOs for data models
- [ ] Set up route in Vue Router with lazy loading
- [ ] Add navigation guards if authentication required
- [ ] Handle loading states with conditional rendering

---

## Import Aliases Quick Reference

| Alias | Resolves To | Example |
|-------|-------------|---------|
| `@/` | `src/` | `import { userApi } from '@/api/user'` |

*Note: Additional aliases may be configured in `vite.config.ts`*

---

## Common Imports Cheatsheet

```typescript
// Vue 3 Composition API
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { Ref, ComputedRef } from 'vue'

// Vue Router
import { useRouter, useRoute } from 'vue-router'
import type { RouteLocationNormalized } from 'vue-router'

// TanStack Query (Vue 3)
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'

// ShadCN Vue Components
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

// Local Composables
import { useAuthState } from '@/composables/useAuthState'

// API & Types
import { userApi } from '@/api/user'
import type { User, UserDTO } from '@/model/User'
```

---

## Topic Guides

### 🎨 Component Patterns

**Modern Vue 3 components use:**
- `<script setup lang="ts">` for Composition API
- `withDefaults(defineProps<Props>(), {})` for typed props with defaults
- `defineEmits<Events>()` for type-safe events
- `defineAsyncComponent()` for code splitting
- Single File Components (`.vue`)

**Key Concepts:**
- Lazy load heavy components (DataGrid, charts, editors)
- Use conditional rendering (`v-if`, `v-show`) for loading states
- Component structure: `<template>` → `<style scoped>` → `<script setup>`
- Extract logic into composables for reusability
- Never put arrays/objects directly in template - use computed/constants

**[📖 Complete Guide: resources/component-patterns.md](resources/component-patterns.md)**

---

### 📊 Data Fetching

**PRIMARY PATTERN: useQuery**
- Cache-first strategy
- Automatic refetching and caching
- Type-safe with generics
- Returns reactive refs in Vue
- Handle loading with `isLoading` ref

**API Service Layer:**
- Create `api/{feature}Api.ts` files
- Use `axios` or `fetch`
- Centralized methods per feature
- Export typed functions

**[📖 Complete Guide: resources/data-fetching.md](resources/data-fetching.md)**

---

### 📁 File Organization

**Component Organization:**
- Keep page-specific components together
- Reusable components in shared location
- Co-locate composables with their usage when possible

**API Layer:**
- One API file per feature/domain
- Export typed functions
- Handle errors consistently

**[📖 Complete Guide: resources/file-organization.md](resources/file-organization.md)**

---

### 🎨 Styling

**Tailwind CSS + ShadCN Vue:**
- Use Tailwind utility classes directly in `<template>`
- ShadCN Vue provides pre-built accessible components
- Use `<style scoped>` for component-specific styles
- No CSS-in-JS needed

**Example:**
```vue
<template>
  <div class="p-4 flex flex-col gap-4">
    <Card class="p-6">
      <h2 class="text-2xl font-bold mb-4">Title</h2>
      <Button variant="default">Action</Button>
    </Card>
  </div>
</template>
```

**[📖 Complete Guide: resources/styling-guide.md](resources/styling-guide.md)**

---

### 🛣️ Routing

**Vue Router Patterns:**
- Define routes in router configuration
- Lazy load page components with dynamic imports
- Use route meta fields for authentication
- Access with `useRouter()` and `useRoute()` composables

**Example:**
```typescript
const routes = [
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/pages/ProfilePage.vue'),
    meta: { requiresAuth: true }
  }
]
```

**[📖 Complete Guide: resources/routing-guide.md](resources/routing-guide.md)**

---

### ⏳ Loading & Error States

**Loading State Pattern:**

```vue
<template>
  <div v-if="isLoading" class="flex justify-center p-8">
    <LoadingSpinner />
  </div>

  <div v-else-if="error" class="text-red-500">
    Error: {{ error.message }}
  </div>

  <div v-else>
    <!-- Content with data -->
    {{ data }}
  </div>
</template>
```

**Error Handling:**
- Use TanStack Query's `isError` and `error` refs
- Show user-friendly error messages
- Use toast notifications for feedback
- Handle errors at appropriate level (component vs global)

**[📖 Complete Guide: resources/loading-and-error-states.md](resources/loading-and-error-states.md)**

---

### ⚡ Performance

**Optimization Patterns:**
- `computed()`: Cached derived state with automatic dependency tracking
- `watch()` / `watchEffect()`: React to state changes
- Lazy load heavy components with `defineAsyncComponent()`
- Debounce user input (300-500ms) for search/filters
- Use `v-show` vs `v-if` appropriately (frequently toggled vs rarely shown)

**[📖 Complete Guide: resources/performance.md](resources/performance.md)**

---

### 📘 TypeScript

**Standards:**
- Strict mode enabled, avoid `any` type
- Explicit return types on functions
- Use `type` keyword for imports: `import type { User } from '@/model/User'`
- Define interfaces for props, emits, and composables
- Use generics for reusable composables

**[📖 Complete Guide: resources/typescript-standards.md](resources/typescript-standards.md)**

---

### 🔧 Common Patterns

**Covered Topics:**
- Form handling with validation
- Singleton composable stores for global state
- Authentication patterns with composables
- Mutation patterns with cache invalidation
- Modal/Dialog management

**[📖 Complete Guide: resources/common-patterns.md](resources/common-patterns.md)**

---

### 📚 Complete Examples

**Full working examples:**
- Modern Vue 3 component with all patterns
- Complete page with data fetching
- API service layer implementation
- Route with lazy loading
- Form with validation
- Singleton composable store

**[📖 Complete Guide: resources/complete-examples.md](resources/complete-examples.md)**

---

## Navigation Guide

| Need to... | Read this resource |
|------------|-------------------|
| Create a component | [component-patterns.md](resources/component-patterns.md) |
| Fetch data | [data-fetching.md](resources/data-fetching.md) |
| Organize files/folders | [file-organization.md](resources/file-organization.md) |
| Style components | [styling-guide.md](resources/styling-guide.md) |
| Set up routing | [routing-guide.md](resources/routing-guide.md) |
| Handle loading/errors | [loading-and-error-states.md](resources/loading-and-error-states.md) |
| Optimize performance | [performance.md](resources/performance.md) |
| TypeScript types | [typescript-standards.md](resources/typescript-standards.md) |
| Forms/Auth/Patterns | [common-patterns.md](resources/common-patterns.md) |
| See full examples | [complete-examples.md](resources/complete-examples.md) |

---

## Core Principles

1. **Single File Components**: All components are `.vue` files with `<script setup lang="ts">`
2. **Composition API**: Use composables for shared logic and state
3. **TanStack Query**: Primary pattern for data fetching and caching
4. **Singleton Composables**: For global state (auth, user, config)
5. **Tailwind for Styling**: Utility-first CSS, no CSS-in-JS
6. **Lazy Loading**: Dynamic imports for heavy components and routes
7. **Type Safety**: TypeScript strict mode, explicit types
8. **Conditional Rendering**: Use `v-if`/`v-else` for loading and error states

---

## Modern Vue 3 Component Template (Quick Copy)

```vue
<template>
  <div class="p-4">
    <div v-if="isLoading" class="flex justify-center p-8">
      <span>Loading...</span>
    </div>

    <div v-else-if="error" class="text-red-500">
      Error loading data
    </div>

    <Card v-else class="p-6">
      <CardContent>
        <h2 class="text-xl font-bold mb-4">{{ displayValue }}</h2>
        <Button @click="handleAction">
          Action
        </Button>
      </CardContent>
    </Card>
  </div>
</template>

<style scoped>
/* Optional scoped styles if needed */
</style>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { featureApi } from '@/api/feature'
import type { FeatureData } from '@/model/Feature'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

interface Props {
  id: number
  mode?: 'view' | 'edit'
}

interface Emits {
  action: []
  update: [value: string]
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'view'
})

const emit = defineEmits<Emits>()

// Local state
const localState = ref<string>('')

// Data fetching
const { data, isLoading, error } = useQuery({
  queryKey: ['feature', () => props.id],
  queryFn: () => featureApi.getFeature(props.id)
})

// Computed values
const displayValue = computed(() => {
  return data.value ? data.value.name.toUpperCase() : ''
})

// Event handlers
const handleAction = () => {
  localState.value = 'updated'
  emit('action')
}
</script>
```

For complete examples, see [resources/complete-examples.md](resources/complete-examples.md)

---

## Related Skills

- **backend-dev-guidelines**: Backend API patterns (Kotlin/Spring Boot)
- **route-tester**: Testing API routes

---

**Skill Status**: Vue 3 focused with progressive resource loading
