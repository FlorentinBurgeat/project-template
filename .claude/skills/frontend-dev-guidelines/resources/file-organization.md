# File Organization Principles

General organization principles for maintainable Vue 3 code. Project-specific structure is defined in your project's documentation.

---

## File Naming Conventions

### Vue Components

**Pattern**: PascalCase with `.vue` extension

```
✅ CORRECT:
UserProfile.vue
DataTable.vue
AppHeader.vue
LoginForm.vue

❌ AVOID:
userProfile.vue       // camelCase
user-profile.vue      // kebab-case
USERPROFILE.vue      // All caps
```

### Composables

**Pattern**: camelCase with `use` prefix, `.ts` extension

```
✅ CORRECT:
useAuth.ts
useCounter.ts
useDebounce.ts
useLocalStorage.ts

❌ AVOID:
UseAuth.ts           // PascalCase
auth.ts              // Missing 'use' prefix
use-auth.ts          // kebab-case
```

### API Services

**Pattern**: camelCase with `Api` suffix, `.ts` extension

```
✅ CORRECT:
userApi.ts
postApi.ts
authApi.ts
productApi.ts

❌ AVOID:
UserApi.ts           // PascalCase
user.ts              // Missing 'Api' suffix
user-api.ts          // kebab-case
```

### Type Files

**Pattern**: PascalCase, `.ts` extension

```
✅ CORRECT:
User.ts
Post.ts
ApiResponse.ts
FormData.ts

❌ AVOID:
user.ts              // camelCase
user-type.ts         // kebab-case
IUser.ts             // Hungarian notation (not needed in TypeScript)
```

### Utilities/Helpers

**Pattern**: camelCase, `.ts` extension

```
✅ CORRECT:
validation.ts
formatting.ts
constants.ts
dateUtils.ts

❌ AVOID:
Validation.ts        // PascalCase
validation-utils.ts  // kebab-case
```

---

## Component Organization

### Single File Components (SFC)

Every Vue component should be a `.vue` file with three sections:

```vue
<script setup lang="ts">
// Component logic
import { ref } from 'vue'

const count = ref(0)
</script>

<template>
  <!-- Component template -->
  <div>{{ count }}</div>
</template>

<style scoped>
/* Component styles (optional) */
</style>
```

### When to Split Components

**Create separate components when:**
- Component exceeds 200-300 lines
- Reusable section appears 2+ times
- Distinct responsibility/concern
- Complex nested template

**Keep together when:**
- Component < 200 lines
- Tightly coupled logic
- Not reusable elsewhere
- Simple presentation

---

## Composable Organization

### What Goes in a Composable

```typescript
// ✅ GOOD - Reusable logic
export function useCounter(initialValue = 0) {
  const count = ref(initialValue)

  function increment() {
    count.value++
  }

  return { count, increment }
}

// ✅ GOOD - Stateful logic
export function useAuth() {
  const user = ref<User | null>(null)
  const isAuthenticated = computed(() => !!user.value)

  async function login(credentials: Credentials) {
    // Login logic
  }

  return { user, isAuthenticated, login }
}

// ❌ AVOID - Simple utilities (use regular functions)
export function useAdd(a: number, b: number) {
  return a + b  // Just use a regular function
}
```

### Composable vs Regular Function

**Use composable when:**
- Manages reactive state
- Uses Vue lifecycle (onMounted, watch, etc.)
- Returns reactive refs/computed
- Reusable across components

**Use regular function when:**
- Pure computation (no state)
- Simple utility
- No Vue-specific features
- Just transforms data

---

## API Service Organization

### API Service Pattern

```typescript
// api/userApi.ts
import axios from 'axios'
import type { User, UserDTO } from '@/model/User'

const API_BASE = '/api'

export const userApi = {
  /**
   * Get user by ID
   */
  async getUser(id: number): Promise<User> {
    const response = await axios.get<UserDTO>(`${API_BASE}/users/${id}`)
    return userMapper.toModel(response.data)
  },

  /**
   * Get all users
   */
  async getUsers(): Promise<User[]> {
    const response = await axios.get<UserDTO[]>(`${API_BASE}/users`)
    return response.data.map(userMapper.toModel)
  },

  /**
   * Create new user
   */
  async createUser(userData: Partial<User>): Promise<User> {
    const dto = userMapper.toDTO(userData)
    const response = await axios.post<UserDTO>(`${API_BASE}/users`, dto)
    return userMapper.toModel(response.data)
  }
}
```

**Key principles:**
- One API file per domain/feature
- Export object with methods
- JSDoc comments
- Use mappers for DTO transformation
- Explicit return types

---

## Import Organization

### Recommended Import Order

```vue
<script setup lang="ts">
// 1. Vue core
import { ref, computed, watch, onMounted } from 'vue'

// 2. Vue ecosystem
import { useRouter, useRoute } from 'vue-router'

// 3. Third-party libraries
import { useQuery, useMutation } from '@tanstack/vue-query'
import { useDebounce } from '@vueuse/core'

// 4. Type imports (grouped together)
import type { User } from '@/model/User'
import type { Post } from '@/model/Post'
import type { Ref, ComputedRef } from 'vue'

// 5. Project imports (organized by category)
import { userApi } from '@/api/user'
import { useAuth } from '@/composables/useAuth'
import { Button } from '@/components/ui/button'

// 6. Relative imports (same directory/feature)
import SubComponent from './SubComponent.vue'
import { useFeature } from '../composables/useFeature'
</script>
```

**Key points:**
- Group by source (Vue → ecosystem → third-party → project)
- Type imports together with `import type`
- Blank lines between groups
- Alphabetical within groups

---

## Type Organization

### Type Files

```typescript
// model/User.ts

// Domain model (what your app uses)
export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  createdAt: Date
}

// DTO (what API returns)
export interface UserDTO {
  id: number
  first_name: string
  last_name: string
  email: string
  created_at: string
}

// Input types (what API accepts)
export interface UserCreateInput {
  firstName: string
  lastName: string
  email: string
  password: string
}

export interface UserUpdateInput {
  firstName?: string
  lastName?: string
  email?: string
}
```

### Mapper Pattern

```typescript
// model/mappers/userMapper.ts
import type { User, UserDTO } from '../User'

export const userMapper = {
  toModel(dto: UserDTO): User {
    return {
      id: dto.id,
      firstName: dto.first_name,
      lastName: dto.last_name,
      email: dto.email,
      createdAt: new Date(dto.created_at)
    }
  },

  toDTO(user: Partial<User>): Partial<UserDTO> {
    return {
      first_name: user.firstName,
      last_name: user.lastName,
      email: user.email
    }
  }
}
```

---

## Co-location Principle

### Keep Related Files Together

Instead of organizing by file type:
```
❌ AVOID:
components/
  UserList.vue
  UserDetail.vue
  PostList.vue
  PostDetail.vue
composables/
  useUser.ts
  usePost.ts
api/
  userApi.ts
  postApi.ts
```

Organize by feature/domain:
```
✅ PREFER:
users/
  components/
    UserList.vue
    UserDetail.vue
  composables/
    useUser.ts
  api/
    userApi.ts

posts/
  components/
    PostList.vue
    PostDetail.vue
  composables/
    usePost.ts
  api/
    postApi.ts
```

**Benefits:**
- Easy to find related code
- Clear feature boundaries
- Easy to move/remove features
- Better code organization

---

## Public API Pattern

### Feature Index File

Create `index.ts` to export public API:

```typescript
// users/index.ts

// Export components
export { default as UserList } from './components/UserList.vue'
export { default as UserDetail } from './components/UserDetail.vue'

// Export composables
export { useUser } from './composables/useUser'

// Export API
export { userApi } from './api/userApi'

// Export types
export type { User, UserCreateInput } from './types'
```

**Usage:**
```vue
<script setup lang="ts">
// ✅ Clean import
import { UserList, useUser } from '@/features/users'

// ❌ Deep import (works but less clean)
import UserList from '@/features/users/components/UserList.vue'
</script>
```

---

## When to Create What

### Create New Component When:
- Logic exceeds 200-300 lines
- Template is complex/nested
- Section is reused 2+ times
- Distinct responsibility
- Can be tested independently

### Create Composable When:
- Reusable reactive logic
- Uses Vue lifecycle hooks
- Manages stateful logic
- Shared across 2+ components

### Create API Service When:
- Feature has API endpoints
- Multiple API calls for same domain
- Need centralized error handling
- Want to mock for testing

### Create Type File When:
- Types used in 3+ places
- Complex domain models
- API DTOs need mapping
- Shared types across features

---

## Summary

**Naming Conventions:**
- Components: PascalCase + `.vue`
- Composables: camelCase + `use` prefix + `.ts`
- API Services: camelCase + `Api` suffix + `.ts`
- Types: PascalCase + `.ts`
- Utilities: camelCase + `.ts`

**Organization Principles:**
1. **Co-location**: Keep related files together
2. **Feature-based**: Organize by domain, not file type
3. **Public API**: Export from index.ts for clean imports
4. **Separation of Concerns**: Components, logic, types separate
5. **Reusability**: Share when used 2-3+ times

**File Extensions:**
- `.vue` for components
- `.ts` for composables, APIs, types, utilities

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [data-fetching.md](data-fetching.md) - API patterns
- [typescript-standards.md](typescript-standards.md) - Type organization
