# Complete Examples

Full working examples combining all Vue 3 patterns: Composition API, TanStack Query, Vue Router, Tailwind CSS, ShadCN Vue, and TypeScript.

---

## Example 1: Complete User Profile Component

Combines: TanStack Query, mutations, computed values, event handling, conditional rendering, Tailwind CSS

```vue
<!-- components/UserProfile.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { userApi } from '@/api/userApi'
import { useToast } from '@/composables/useToast'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardContent, CardFooter } from '@/components/ui/card'
import type { User } from '@/model/User'

/**
 * Props for UserProfile component
 */
interface Props {
  /** User ID to display */
  userId: string

  /** Optional callback when profile is updated */
  onUpdate?: (user: User) => void
}

const props = defineProps<Props>()

const queryClient = useQueryClient()
const { showSuccess, showError } = useToast()

// Data fetching with TanStack Query
const { data: user, isLoading, error } = useQuery({
  queryKey: ['user', () => props.userId],
  queryFn: () => userApi.getUser(props.userId),
  staleTime: 5 * 60 * 1000
})

// Update mutation
const updateMutation = useMutation({
  mutationFn: (updates: Partial<User>) =>
    userApi.updateUser(props.userId, updates),

  onSuccess: (updatedUser) => {
    queryClient.invalidateQueries({ queryKey: ['user', props.userId] })
    showSuccess('Profile updated')
    props.onUpdate?.(updatedUser)
  },

  onError: () => {
    showError('Failed to update profile')
  }
})

// Computed value
const fullName = computed(() => {
  if (!user.value) return ''
  return `${user.value.firstName} ${user.value.lastName}`
})

const initials = computed(() => {
  if (!user.value) return ''
  return `${user.value.firstName[0]}${user.value.lastName[0]}`
})

// Event handlers with function syntax
function handleEdit() {
  // Edit logic here
  console.log('Edit clicked')
}

function handleSave() {
  if (!user.value) return

  updateMutation.mutate({
    firstName: user.value.firstName,
    lastName: user.value.lastName
  })
}
</script>

<template>
  <Card class="max-w-2xl mx-auto">
    <!-- Loading state -->
    <div v-if="isLoading" class="p-6">
      <div class="animate-pulse space-y-4">
        <div class="h-16 w-16 bg-gray-200 rounded-full" />
        <div class="h-4 bg-gray-200 rounded w-3/4" />
        <div class="h-4 bg-gray-200 rounded w-1/2" />
      </div>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="p-6">
      <div class="text-red-600">
        Error loading profile: {{ error.message }}
      </div>
    </div>

    <!-- Success state -->
    <template v-else-if="user">
      <CardHeader>
        <div class="flex items-center gap-4">
          <Avatar class="h-16 w-16">
            <span class="text-xl">{{ initials }}</span>
          </Avatar>
          <div>
            <h2 class="text-2xl font-bold">{{ fullName }}</h2>
            <p class="text-gray-600">{{ user.email }}</p>
          </div>
        </div>
      </CardHeader>

      <CardContent class="space-y-2">
        <div>
          <span class="font-medium">Username:</span> {{ user.username }}
        </div>
        <div>
          <span class="font-medium">Roles:</span> {{ user.roles.join(', ') }}
        </div>
      </CardContent>

      <CardFooter class="flex gap-2">
        <Button @click="handleEdit" variant="outline">
          Edit Profile
        </Button>
        <Button
          @click="handleSave"
          :disabled="updateMutation.isPending.value"
        >
          {{ updateMutation.isPending.value ? 'Saving...' : 'Save Changes' }}
        </Button>
      </CardFooter>
    </template>
  </Card>
</template>
```

---

## Example 2: Complete Feature Structure

Vue 3 feature organization pattern:

```
features/
  users/
    api/
      userApi.ts              # API service layer
    components/
      UserProfile.vue         # Main component (from Example 1)
      UserList.vue            # List component
      UserCard.vue            # Card component
      modals/
        DeleteUserModal.vue   # Modal component
    composables/
      useUser.ts              # Query composable
      useUserMutations.ts     # Mutation composables
      useUserPermissions.ts   # Feature-specific logic
    model/
      User.ts                 # TypeScript interfaces
      mappers/
        userMapper.ts         # DTO to model mapping
    utils/
      validation.ts           # Validation utilities
    index.ts                  # Public API exports
```

### API Service (api/userApi.ts)

```typescript
import axios from 'axios'
import type { User, UserDTO, CreateUserInput, UpdateUserInput } from '../model/User'
import { userMapper } from '../model/mappers/userMapper'

const API_BASE = '/api'

export const userApi = {
  /**
   * Get user by ID
   */
  async getUser(userId: string): Promise<User> {
    const response = await axios.get<UserDTO>(`${API_BASE}/users/${userId}`)
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
  async createUser(input: CreateUserInput): Promise<User> {
    const dto = userMapper.toDTO(input)
    const response = await axios.post<UserDTO>(`${API_BASE}/users`, dto)
    return userMapper.toModel(response.data)
  },

  /**
   * Update existing user
   */
  async updateUser(userId: string, input: UpdateUserInput): Promise<User> {
    const dto = userMapper.toDTO(input)
    const response = await axios.put<UserDTO>(`${API_BASE}/users/${userId}`, dto)
    return userMapper.toModel(response.data)
  },

  /**
   * Delete user
   */
  async deleteUser(userId: string): Promise<void> {
    await axios.delete(`${API_BASE}/users/${userId}`)
  }
}
```

### Query Composable (composables/useUser.ts)

```typescript
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '../api/userApi'
import type { User } from '../model/User'

/**
 * Fetch single user by ID
 */
export function useUser(userId: string) {
  return useQuery<User, Error>({
    queryKey: ['user', userId],
    queryFn: () => userApi.getUser(userId),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000
  })
}

/**
 * Fetch all users
 */
export function useUsers() {
  return useQuery<User[], Error>({
    queryKey: ['users'],
    queryFn: () => userApi.getUsers(),
    staleTime: 1 * 60 * 1000
  })
}
```

### Types (model/User.ts)

```typescript
// Domain model (what your app uses)
export interface User {
  id: string
  username: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
  createdAt: Date
  updatedAt: Date
}

// DTO (what API returns)
export interface UserDTO {
  id: string
  username: string
  email: string
  first_name: string
  last_name: string
  roles: string[]
  created_at: string
  updated_at: string
}

// Input types
export interface CreateUserInput {
  username: string
  email: string
  firstName: string
  lastName: string
  password: string
}

export interface UpdateUserInput {
  firstName?: string
  lastName?: string
  email?: string
}
```

### Mapper (model/mappers/userMapper.ts)

```typescript
import type { User, UserDTO } from '../User'

export const userMapper = {
  toModel(dto: UserDTO): User {
    return {
      id: dto.id,
      username: dto.username,
      email: dto.email,
      firstName: dto.first_name,
      lastName: dto.last_name,
      roles: dto.roles,
      createdAt: new Date(dto.created_at),
      updatedAt: new Date(dto.updated_at)
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

### Public Exports (index.ts)

```typescript
// Export components
export { default as UserProfile } from './components/UserProfile.vue'
export { default as UserList } from './components/UserList.vue'

// Export composables
export { useUser, useUsers } from './composables/useUser'
export { useUserMutations } from './composables/useUserMutations'

// Export API
export { userApi } from './api/userApi'

// Export types
export type { User, CreateUserInput, UpdateUserInput } from './model/User'
```

---

## Example 3: Complete Route with Vue Router

```typescript
// router/index.ts

import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/users/:userId',
    name: 'user-profile',
    // Vue Router handles lazy loading automatically
    component: () => import('@/pages/UserProfilePage.vue'),
    meta: {
      requiresAuth: true,
      breadcrumb: 'User Profile'
    }
  },
  {
    path: '/users',
    name: 'users',
    component: () => import('@/pages/UsersPage.vue'),
    meta: {
      requiresAuth: true,
      breadcrumb: 'Users'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    // Check authentication
    const isAuthenticated = checkAuth()
    if (!isAuthenticated) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
```

```vue
<!-- pages/UserProfilePage.vue -->
<script setup lang="ts">
import { useRoute } from 'vue-router'
import UserProfile from '@/features/users/components/UserProfile.vue'

const route = useRoute()
const userId = route.params.userId as string

function handleProfileUpdate() {
  console.log('Profile updated')
}
</script>

<template>
  <div class="container mx-auto py-8">
    <UserProfile
      :user-id="userId"
      :on-update="handleProfileUpdate"
    />
  </div>
</template>
```

---

## Example 4: List with Search and Filtering

```vue
<!-- components/UserList.vue -->
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { useDebounceFn } from '@vueuse/core'
import { userApi } from '@/api/userApi'
import { Input } from '@/components/ui/input'
import { Card } from '@/components/ui/card'
import type { User } from '@/model/User'

const searchTerm = ref('')
const debouncedSearch = ref('')

// Debounce search input
const updateSearch = useDebounceFn((value: string) => {
  debouncedSearch.value = value
}, 300)

// Watch searchTerm and update debounced value
function handleSearchInput(event: Event) {
  const target = event.target as HTMLInputElement
  searchTerm.value = target.value
  updateSearch(target.value)
}

// Fetch users
const { data: users, isLoading, error } = useQuery({
  queryKey: ['users'],
  queryFn: () => userApi.getUsers()
})

// Computed filtered users
const filteredUsers = computed(() => {
  if (!users.value || !debouncedSearch.value) {
    return users.value || []
  }

  const search = debouncedSearch.value.toLowerCase()
  return users.value.filter(user =>
    user.username.toLowerCase().includes(search) ||
    user.email.toLowerCase().includes(search) ||
    user.firstName.toLowerCase().includes(search) ||
    user.lastName.toLowerCase().includes(search)
  )
})

const resultCount = computed(() => filteredUsers.value.length)
</script>

<template>
  <div class="space-y-4">
    <Input
      :value="searchTerm"
      @input="handleSearchInput"
      placeholder="Search users..."
      class="max-w-md"
    />

    <!-- Loading state -->
    <div v-if="isLoading" class="space-y-2">
      <div v-for="i in 5" :key="i" class="animate-pulse">
        <div class="h-20 bg-gray-200 rounded" />
      </div>
    </div>

    <!-- Error state -->
    <div v-else-if="error" class="text-red-600">
      Error loading users: {{ error.message }}
    </div>

    <!-- Results -->
    <template v-else>
      <div class="text-sm text-gray-600">
        Found {{ resultCount }} user{{ resultCount === 1 ? '' : 's' }}
      </div>

      <div class="space-y-2">
        <Card
          v-for="user in filteredUsers"
          :key="user.id"
          class="p-4"
        >
          <div class="flex items-center justify-between">
            <div>
              <div class="font-medium">
                {{ user.firstName }} {{ user.lastName }}
              </div>
              <div class="text-sm text-gray-600">
                {{ user.email }}
              </div>
            </div>
            <div class="text-sm text-gray-500">
              @{{ user.username }}
            </div>
          </div>
        </Card>
      </div>
    </template>
  </div>
</template>
```

---

## Example 5: Form with Validation

```vue
<!-- components/CreateUserForm.vue -->
<script setup lang="ts">
import { reactive, computed } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { z } from 'zod'
import { userApi } from '@/api/userApi'
import { useToast } from '@/composables/useToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardContent, CardFooter } from '@/components/ui/card'
import type { CreateUserInput } from '@/model/User'

/**
 * Props for CreateUserForm
 */
interface Props {
  /** Callback on successful creation */
  onSuccess?: () => void
}

const props = defineProps<Props>()

const queryClient = useQueryClient()
const { showSuccess, showError } = useToast()

// Form validation schema
const userSchema = z.object({
  username: z.string().min(3).max(50),
  email: z.string().email(),
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  password: z.string().min(8)
})

// Form state
const formData = reactive<CreateUserInput>({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: ''
})

// Validation errors
const errors = reactive<Record<string, string>>({})

// Create mutation
const createMutation = useMutation({
  mutationFn: (data: CreateUserInput) => userApi.createUser(data),

  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['users'] })
    showSuccess('User created successfully')
    resetForm()
    props.onSuccess?.()
  },

  onError: () => {
    showError('Failed to create user')
  }
})

// Validation function
function validateForm(): boolean {
  // Clear previous errors
  Object.keys(errors).forEach(key => delete errors[key])

  const result = userSchema.safeParse(formData)

  if (!result.success) {
    result.error.errors.forEach(error => {
      if (error.path[0]) {
        errors[error.path[0] as string] = error.message
      }
    })
    return false
  }

  return true
}

// Form handlers
function handleSubmit(event: Event) {
  event.preventDefault()

  if (validateForm()) {
    createMutation.mutate(formData)
  }
}

function resetForm() {
  formData.username = ''
  formData.email = ''
  formData.firstName = ''
  formData.lastName = ''
  formData.password = ''
  Object.keys(errors).forEach(key => delete errors[key])
}

const isSubmitting = computed(() => createMutation.isPending.value)
</script>

<template>
  <Card class="max-w-md">
    <CardHeader>
      <h2 class="text-2xl font-bold">Create User</h2>
    </CardHeader>

    <form @submit="handleSubmit">
      <CardContent class="space-y-4">
        <div>
          <Label for="username">Username</Label>
          <Input
            id="username"
            v-model="formData.username"
            :class="{ 'border-red-500': errors.username }"
          />
          <p v-if="errors.username" class="text-sm text-red-600 mt-1">
            {{ errors.username }}
          </p>
        </div>

        <div>
          <Label for="email">Email</Label>
          <Input
            id="email"
            v-model="formData.email"
            type="email"
            :class="{ 'border-red-500': errors.email }"
          />
          <p v-if="errors.email" class="text-sm text-red-600 mt-1">
            {{ errors.email }}
          </p>
        </div>

        <div>
          <Label for="firstName">First Name</Label>
          <Input
            id="firstName"
            v-model="formData.firstName"
            :class="{ 'border-red-500': errors.firstName }"
          />
          <p v-if="errors.firstName" class="text-sm text-red-600 mt-1">
            {{ errors.firstName }}
          </p>
        </div>

        <div>
          <Label for="lastName">Last Name</Label>
          <Input
            id="lastName"
            v-model="formData.lastName"
            :class="{ 'border-red-500': errors.lastName }"
          />
          <p v-if="errors.lastName" class="text-sm text-red-600 mt-1">
            {{ errors.lastName }}
          </p>
        </div>

        <div>
          <Label for="password">Password</Label>
          <Input
            id="password"
            v-model="formData.password"
            type="password"
            :class="{ 'border-red-500': errors.password }"
          />
          <p v-if="errors.password" class="text-sm text-red-600 mt-1">
            {{ errors.password }}
          </p>
        </div>
      </CardContent>

      <CardFooter class="flex gap-2">
        <Button type="button" variant="outline" @click="resetForm">
          Reset
        </Button>
        <Button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Creating...' : 'Create User' }}
        </Button>
      </CardFooter>
    </form>
  </Card>
</template>
```

---

## Example 6: Modal Dialog with Form

```vue
<!-- components/AddUserModal.vue -->
<script setup lang="ts">
import { reactive, watch } from 'vue'
import { z } from 'zod'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

/**
 * Props for AddUserModal
 */
interface Props {
  /** Whether modal is open */
  open: boolean

  /** Callback on submit */
  onSubmit: (data: FormData) => Promise<void>
}

/**
 * Emits
 */
interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'close'): void
}

interface FormData {
  name: string
  email: string
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Invalid email address')
})

const formData = reactive<FormData>({
  name: '',
  email: ''
})

const errors = reactive<Record<string, string>>({})

// Reset form when modal closes
watch(() => props.open, (isOpen) => {
  if (!isOpen) {
    resetForm()
  }
})

function validateForm(): boolean {
  Object.keys(errors).forEach(key => delete errors[key])

  const result = formSchema.safeParse(formData)

  if (!result.success) {
    result.error.errors.forEach(error => {
      if (error.path[0]) {
        errors[error.path[0] as string] = error.message
      }
    })
    return false
  }

  return true
}

async function handleSubmit() {
  if (!validateForm()) return

  try {
    await props.onSubmit({ ...formData })
    handleClose()
  } catch (error) {
    console.error('Submit failed:', error)
  }
}

function handleClose() {
  emit('update:open', false)
  emit('close')
}

function resetForm() {
  formData.name = ''
  formData.email = ''
  Object.keys(errors).forEach(key => delete errors[key])
}
</script>

<template>
  <Dialog :open="open" @update:open="emit('update:open', $event)">
    <DialogContent>
      <DialogHeader>
        <DialogTitle>Add User</DialogTitle>
      </DialogHeader>

      <div class="space-y-4">
        <div>
          <Label for="name">Name</Label>
          <Input
            id="name"
            v-model="formData.name"
            :class="{ 'border-red-500': errors.name }"
          />
          <p v-if="errors.name" class="text-sm text-red-600 mt-1">
            {{ errors.name }}
          </p>
        </div>

        <div>
          <Label for="email">Email</Label>
          <Input
            id="email"
            v-model="formData.email"
            type="email"
            :class="{ 'border-red-500': errors.email }"
          />
          <p v-if="errors.email" class="text-sm text-red-600 mt-1">
            {{ errors.email }}
          </p>
        </div>
      </div>

      <DialogFooter>
        <DialogClose as-child>
          <Button variant="outline">Cancel</Button>
        </DialogClose>
        <Button @click="handleSubmit">Add User</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
```

**Usage:**
```vue
<script setup lang="ts">
import { ref } from 'vue'
import AddUserModal from './AddUserModal.vue'

const isModalOpen = ref(false)

async function handleAddUser(data: { name: string; email: string }) {
  // Handle user creation
  console.log('Adding user:', data)
}
</script>

<template>
  <div>
    <Button @click="isModalOpen = true">Add User</Button>

    <AddUserModal
      v-model:open="isModalOpen"
      :on-submit="handleAddUser"
      @close="console.log('Modal closed')"
    />
  </div>
</template>
```

---

## Example 7: Cache-First Strategy

```typescript
// composables/usePost.ts

import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { postApi } from '@/api/postApi'
import type { Post } from '@/model/Post'

/**
 * Smart post hook with cache-first strategy
 * Reuses data from list cache when available
 */
export function usePost(projectId: number, postId: number) {
  const queryClient = useQueryClient()

  return useQuery<Post, Error>({
    queryKey: ['post', projectId, postId],
    queryFn: async () => {
      // Strategy 1: Check list cache first (avoids API call)
      const listCache = queryClient.getQueryData<{ items: Post[] }>([
        'posts',
        projectId,
        'list'
      ])

      if (listCache?.items) {
        const cached = listCache.items.find(
          (item) => item.id === postId
        )

        if (cached) {
          return cached  // Return from cache - no API call!
        }
      }

      // Strategy 2: Not in cache, fetch from API
      return postApi.getPost(projectId, postId)
    },
    staleTime: 5 * 60 * 1000,       // Fresh for 5 minutes
    gcTime: 10 * 60 * 1000,          // Cache for 10 minutes
    refetchOnWindowFocus: false      // Don't refetch on focus
  })
}
```

**Why this pattern:**
- Checks list cache before API call
- Instant data if user navigated from list
- Falls back to API if not cached
- Configurable cache times
- Better performance and UX

---

## Example 8: Parallel Data Fetching

```vue
<!-- pages/Dashboard.vue -->
<script setup lang="ts">
import { useQueries } from '@tanstack/vue-query'
import { userApi } from '@/api/userApi'
import { statsApi } from '@/api/statsApi'
import { activityApi } from '@/api/activityApi'
import { Card, CardHeader, CardContent } from '@/components/ui/card'

// Fetch all data in parallel
const queries = useQueries({
  queries: [
    {
      queryKey: ['stats'],
      queryFn: () => statsApi.getStats()
    },
    {
      queryKey: ['users', 'active'],
      queryFn: () => userApi.getActiveUsers()
    },
    {
      queryKey: ['activity', 'recent'],
      queryFn: () => activityApi.getRecent()
    }
  ]
})

const [statsQuery, usersQuery, activityQuery] = queries

const isLoading = computed(() =>
  queries.some(q => q.isLoading.value)
)

const hasError = computed(() =>
  queries.some(q => q.isError.value)
)
</script>

<template>
  <div class="p-8">
    <!-- Loading state -->
    <div v-if="isLoading" class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card v-for="i in 3" :key="i" class="animate-pulse">
        <CardContent class="h-32 bg-gray-200" />
      </Card>
    </div>

    <!-- Error state -->
    <div v-else-if="hasError" class="text-red-600">
      Error loading dashboard data
    </div>

    <!-- Success state -->
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card>
        <CardHeader>
          <h3 class="text-lg font-semibold">Stats</h3>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">
            {{ statsQuery.data.value?.total ?? 0 }}
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <h3 class="text-lg font-semibold">Active Users</h3>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">
            {{ usersQuery.data.value?.length ?? 0 }}
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <h3 class="text-lg font-semibold">Recent Activity</h3>
        </CardHeader>
        <CardContent>
          <p class="text-2xl font-bold">
            {{ activityQuery.data.value?.length ?? 0 }}
          </p>
        </CardContent>
      </Card>
    </div>
  </div>
</template>
```

---

## Example 9: Optimistic Update

```typescript
// composables/useUserMutations.ts

import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { userApi } from '@/api/userApi'
import type { User } from '@/model/User'

export function useToggleUserStatus() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (userId: string) => userApi.toggleStatus(userId),

    // Optimistic update
    onMutate: async (userId) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: ['users'] })

      // Snapshot previous value
      const previousUsers = queryClient.getQueryData<User[]>(['users'])

      // Optimistically update UI
      queryClient.setQueryData<User[]>(['users'], (old) => {
        if (!old) return []
        return old.map(user =>
          user.id === userId
            ? { ...user, active: !user.active }
            : user
        )
      })

      return { previousUsers }
    },

    // Rollback on error
    onError: (err, userId, context) => {
      if (context?.previousUsers) {
        queryClient.setQueryData(['users'], context.previousUsers)
      }
    },

    // Refetch after mutation
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
    }
  })
}
```

**Usage in component:**
```vue
<script setup lang="ts">
import { useToggleUserStatus } from '@/features/users/composables/useUserMutations'

const toggleStatus = useToggleUserStatus()

function handleToggle(userId: string) {
  toggleStatus.mutate(userId)
}
</script>

<template>
  <Button @click="handleToggle(user.id)">
    Toggle Status
  </Button>
</template>
```

---

## Example 10: Complete Page Component

```vue
<!-- pages/UsersPage.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/features/users/api/userApi'
import { useUserMutations } from '@/features/users/composables/useUserMutations'
import UserList from '@/features/users/components/UserList.vue'
import CreateUserForm from '@/features/users/components/CreateUserForm.vue'
import { Button } from '@/components/ui/button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'

const activeTab = ref('list')
const isCreating = ref(false)

// Fetch users
const { data: users, isLoading, error } = useQuery({
  queryKey: ['users'],
  queryFn: () => userApi.getUsers()
})

// Create mutation
const { createUser } = useUserMutations()

function handleCreateSuccess() {
  isCreating.value = false
  activeTab.value = 'list'
}
</script>

<template>
  <div class="container mx-auto py-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-3xl font-bold">Users Management</h1>
      <Button @click="isCreating = true">
        Add User
      </Button>
    </div>

    <Tabs v-model="activeTab">
      <TabsList>
        <TabsTrigger value="list">User List</TabsTrigger>
        <TabsTrigger value="create" :disabled="!isCreating">
          Create User
        </TabsTrigger>
      </TabsList>

      <TabsContent value="list">
        <UserList />
      </TabsContent>

      <TabsContent value="create">
        <CreateUserForm :on-success="handleCreateSuccess" />
      </TabsContent>
    </Tabs>
  </div>
</template>
```

---

## Summary

**Key Patterns Demonstrated:**

1. **Component Structure**: Single File Components with `<script setup>`, `<template>`, `<style scoped>`
2. **Feature Organization**: Organized by domain (api/, components/, composables/, model/)
3. **Routing**: Vue Router with lazy loading
4. **Data Fetching**: TanStack Query with reactive refs
5. **Forms**: Reactive forms with Zod validation
6. **Error Handling**: useToast composable + conditional rendering
7. **Performance**: computed(), debouncing, cache-first strategy
8. **Styling**: Tailwind CSS + ShadCN Vue components
9. **State Management**: Singleton composables for global state
10. **TypeScript**: Full type safety with explicit return types

**Best Practices:**
- Function declarations over arrow functions
- v-if/v-else for conditional rendering
- withDefaults for prop defaults
- Component composition for variants
- DTO to Model mapping pattern
- Optimistic updates for better UX
- Parallel queries for performance

**See other resources for detailed explanations of each pattern.**
