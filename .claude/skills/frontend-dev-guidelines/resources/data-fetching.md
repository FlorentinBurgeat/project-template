# Data Fetching Patterns

Modern data fetching using TanStack Query (Vue) with cache-first strategies and centralized API services.

---

## PRIMARY PATTERN: useQuery

### Why useQuery?

For **all data fetching**, use `useQuery` from `@tanstack/vue-query`:

**Benefits:**
- Automatic caching and cache invalidation
- Background refetching
- Optimistic updates
- Type-safe with generics
- Returns reactive refs in Vue
- Reduces boilerplate code

### Basic Pattern

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { myFeatureApi } from '@/api/myFeature'
import type { MyEntity } from '@/model/MyEntity'

interface Props {
  id: number
}

const props = defineProps<Props>()

// useQuery returns reactive refs
const { data, isLoading, error, isError } = useQuery({
  queryKey: ['myEntity', () => props.id],
  queryFn: () => myFeatureApi.getEntity(props.id)
})
</script>

<template>
  <div>
    <div v-if="isLoading">Loading...</div>
    <div v-else-if="isError">Error: {{ error.message }}</div>
    <div v-else>
      <h2>{{ data.name }}</h2>
      <p>{{ data.description }}</p>
    </div>
  </div>
</template>
```

**Key Points:**
- All return values are reactive refs (use `.value` in script)
- Data is `undefined` until loaded (check with `v-if` or optional chaining)
- Query key array with reactive dependencies

---

## Query Keys with Reactive Dependencies

### Using Function in Query Key

When query depends on reactive props or refs, wrap them in a function:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { postApi } from '@/api/post'

interface Props {
  userId: number
}

const props = defineProps<Props>()
const filter = ref('all')

// Reactive dependencies in query key
const { data } = useQuery({
  queryKey: ['posts', () => props.userId, () => filter.value],
  queryFn: () => postApi.getPosts(props.userId, filter.value)
})

function updateFilter(newFilter: string) {
  filter.value = newFilter // Query will automatically refetch
}
</script>

<template>
  <div>
    <button @click="updateFilter('active')">Active</button>
    <button @click="updateFilter('completed')">Completed</button>

    <div v-for="post in data" :key="post.id">
      {{ post.title }}
    </div>
  </div>
</template>
```

---

## Cache-First Strategy

### Checking Cache Before API Call

**Smart caching** reduces API calls by checking React Query cache first:

```typescript
// composables/usePost.ts
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { postApi } from '@/api/post'
import type { Post } from '@/model/Post'

export function usePost(postId: Ref<number>) {
  const queryClient = useQueryClient()

  return useQuery({
    queryKey: ['post', postId],
    queryFn: async () => {
      // Strategy 1: Try to get from list cache first
      const cachedListData = queryClient.getQueryData<{ posts: Post[] }>([
        'posts',
        'list'
      ])

      if (cachedListData?.posts) {
        const cachedPost = cachedListData.posts.find(
          (post) => post.id === postId.value
        )

        if (cachedPost) {
          return cachedPost  // Return from cache!
        }
      }

      // Strategy 2: Not in cache, fetch from API
      return postApi.getPost(postId.value)
    },
    staleTime: 5 * 60 * 1000,      // Consider fresh for 5 minutes
    gcTime: 10 * 60 * 1000,         // Keep in cache for 10 minutes
    refetchOnWindowFocus: false,    // Don't refetch on focus
  })
}
```

---

## API Service Layer

### Structure

Create dedicated API files per feature/domain:

```
src/
  api/
    auth.ts          # Authentication endpoints
    user.ts          # User management
    post.ts          # Posts feature
    comment.ts       # Comments feature
```

### API File Pattern

```typescript
// api/user.ts
import axios from 'axios'
import type { User, UserDTO } from '@/model/User'
import { userMapper } from '@/model/mappers/userMapper'

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
  },

  /**
   * Update user
   */
  async updateUser(id: number, updates: Partial<User>): Promise<User> {
    const dto = userMapper.toDTO(updates)
    const response = await axios.put<UserDTO>(`${API_BASE}/users/${id}`, dto)
    return userMapper.toModel(response.data)
  },

  /**
   * Delete user
   */
  async deleteUser(id: number): Promise<void> {
    await axios.delete(`${API_BASE}/users/${id}`)
  }
}
```

**Key Points:**
- Export object with methods
- Use axios or fetch
- Transform DTOs to domain models with mappers
- Add JSDoc comments
- Explicit return types

---

## Mutations (Create, Update, Delete)

### useMutation Pattern

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import type { User } from '@/model/User'

const queryClient = useQueryClient()

// Create mutation
const createUserMutation = useMutation({
  mutationFn: (userData: Partial<User>) => userApi.createUser(userData),
  onSuccess: (newUser) => {
    // Invalidate and refetch
    queryClient.invalidateQueries({ queryKey: ['users'] })
    console.log('User created:', newUser)
  },
  onError: (error) => {
    console.error('Failed to create user:', error)
  }
})

// Update mutation
const updateUserMutation = useMutation({
  mutationFn: ({ id, updates }: { id: number; updates: Partial<User> }) =>
    userApi.updateUser(id, updates),
  onSuccess: (updatedUser) => {
    // Invalidate specific query
    queryClient.invalidateQueries({ queryKey: ['user', updatedUser.id] })
    queryClient.invalidateQueries({ queryKey: ['users'] })
  }
})

// Delete mutation
const deleteUserMutation = useMutation({
  mutationFn: (id: number) => userApi.deleteUser(id),
  onSuccess: (_, deletedId) => {
    queryClient.invalidateQueries({ queryKey: ['users'] })
    // Remove from cache
    queryClient.removeQueries({ queryKey: ['user', deletedId] })
  }
})

function handleCreate() {
  createUserMutation.mutate({
    name: 'John Doe',
    email: 'john@example.com'
  })
}

function handleUpdate(userId: number) {
  updateUserMutation.mutate({
    id: userId,
    updates: { name: 'Jane Doe' }
  })
}

function handleDelete(userId: number) {
  deleteUserMutation.mutate(userId)
}
</script>

<template>
  <div>
    <button
      @click="handleCreate"
      :disabled="createUserMutation.isPending.value"
    >
      {{ createUserMutation.isPending.value ? 'Creating...' : 'Create User' }}
    </button>

    <p v-if="createUserMutation.isError.value" class="text-red-500">
      Error: {{ createUserMutation.error.value.message }}
    </p>

    <p v-if="createUserMutation.isSuccess.value" class="text-green-500">
      User created successfully!
    </p>
  </div>
</template>
```

---

## Optimistic Updates

### Update UI Before Server Response

```typescript
// composables/useUpdatePost.ts
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { postApi } from '@/api/post'
import type { Post } from '@/model/Post'

export function useUpdatePost() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, updates }: { id: number; updates: Partial<Post> }) =>
      postApi.updatePost(id, updates),

    // Before mutation
    onMutate: async ({ id, updates }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['post', id] })

      // Snapshot previous value
      const previousPost = queryClient.getQueryData<Post>(['post', id])

      // Optimistically update cache
      queryClient.setQueryData<Post>(['post', id], (old) => {
        if (!old) return old
        return { ...old, ...updates }
      })

      // Return context with previous value
      return { previousPost }
    },

    // On error, rollback
    onError: (error, { id }, context) => {
      if (context?.previousPost) {
        queryClient.setQueryData(['post', id], context.previousPost)
      }
    },

    // Always refetch after error or success
    onSettled: (data, error, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['post', id] })
    }
  })
}
```

**Usage:**
```vue
<script setup lang="ts">
import { useUpdatePost } from '@/composables/useUpdatePost'

const updatePost = useUpdatePost()

function handleUpdate(postId: number) {
  updatePost.mutate({
    id: postId,
    updates: { title: 'New Title' }
  })
}
</script>

<template>
  <button @click="handleUpdate(1)">
    Update Post
  </button>
</template>
```

---

## Dependent Queries

### Serial Query Execution

```vue
<script setup lang="ts">
import { computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import { projectApi } from '@/api/project'

interface Props {
  email: string
}

const props = defineProps<Props>()

// Get the user
const { data: user, isLoading: isLoadingUser } = useQuery({
  queryKey: ['user', () => props.email],
  queryFn: () => userApi.getUserByEmail(props.email)
})

// Compute dependencies
const userId = computed(() => user.value?.id)
const isEnabled = computed(() => !!user.value?.id)

// Then get the user's projects (only when user is loaded)
const { data: projects, isLoading: isLoadingProjects } = useQuery({
  queryKey: ['projects', userId],
  queryFn: () => projectApi.getProjectsByUser(userId.value!),
  enabled: isEnabled // Don't run until user is loaded
})

const isLoading = computed(() => isLoadingUser.value || isLoadingProjects.value)
</script>

<template>
  <div>
    <div v-if="isLoading">Loading...</div>
    <div v-else-if="projects">
      <h2>Projects for {{ user.name }}</h2>
      <ul>
        <li v-for="project in projects" :key="project.id">
          {{ project.name }}
        </li>
      </ul>
    </div>
  </div>
</template>
```

---

## Query Configuration

### Common Options

```typescript
useQuery({
  queryKey: ['posts'],
  queryFn: () => postApi.getPosts(),

  // Caching
  staleTime: 5 * 60 * 1000,      // 5 minutes (data stays fresh)
  gcTime: 10 * 60 * 1000,         // 10 minutes (garbage collection)

  // Refetching
  refetchOnMount: true,            // Refetch when component mounts
  refetchOnWindowFocus: false,     // Don't refetch on window focus
  refetchOnReconnect: true,        // Refetch when internet reconnects
  refetchInterval: false,          // No polling by default

  // Retry
  retry: 3,                        // Retry failed requests 3 times
  retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),

  // Enabled/disabled
  enabled: computed(() => someCondition.value),

  // Callbacks
  onSuccess: (data) => {
    console.log('Data loaded:', data)
  },
  onError: (error) => {
    console.error('Error loading data:', error)
  }
})
```

---

## Composable Pattern for Queries

### Reusable Query Composables

```typescript
// composables/useUserQuery.ts
import { computed, type Ref } from 'vue'
import { useQuery, type UseQueryOptions } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import type { User } from '@/model/User'

export function useUserQuery(
  userId: Ref<number> | number,
  options?: Omit<UseQueryOptions<User>, 'queryKey' | 'queryFn'>
) {
  const idRef = computed(() =>
    typeof userId === 'number' ? userId : userId.value
  )

  return useQuery({
    queryKey: ['user', idRef],
    queryFn: () => userApi.getUser(idRef.value),
    ...options
  })
}
```

**Usage:**
```vue
<script setup lang="ts">
import { useUserQuery } from '@/composables/useUserQuery'

interface Props {
  userId: number
}

const props = defineProps<Props>()

const { data: user, isLoading, error } = useUserQuery(() => props.userId)
</script>

<template>
  <div v-if="isLoading">Loading...</div>
  <div v-else-if="error">Error loading user</div>
  <div v-else>
    <h2>{{ user.name }}</h2>
    <p>{{ user.email }}</p>
  </div>
</template>
```

---

## Summary

**Data Fetching Best Practices:**
1. Use `useQuery` for all GET operations
2. Use `useMutation` for CREATE, UPDATE, DELETE
3. Create API service layer per feature
4. Transform DTOs with mappers
5. Use query keys with reactive dependencies
6. Cache-first strategy where possible
7. Invalidate queries after mutations
8. Composables for reusable queries
9. Use function syntax for all functions
10. Handle loading and error states in template

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [loading-and-error-states.md](loading-and-error-states.md) - Error handling patterns
- [common-patterns.md](common-patterns.md) - Mutation patterns
