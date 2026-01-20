# Common Patterns

Frequently used patterns for forms, authentication, modals, and state management in Vue 3 applications.

---

## Authentication with Singleton Composable

### useAuthState Pattern

```typescript
// composables/useAuthState.ts
import { ref, computed, readonly } from 'vue'
import type { Ref } from 'vue'
import { authApi } from '@/api/auth'

interface User {
  id: number
  email: string
  username: string
  roles: string[]
}

// Singleton state (shared across all components)
const user = ref<User | null>(null)
const isLoading = ref(false)

export function useAuthState() {
  const isAuthenticated = computed(() => !!user.value)

  async function login(credentials: { email: string; password: string }) {
    isLoading.value = true
    try {
      const response = await authApi.login(credentials)
      user.value = response.user
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function logout() {
    try {
      await authApi.logout()
      user.value = null
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  async function fetchCurrentUser() {
    isLoading.value = true
    try {
      const response = await authApi.getCurrentUser()
      user.value = response.user
    } catch (error) {
      user.value = null
    } finally {
      isLoading.value = false
    }
  }

  function hasRole(role: string): boolean {
    return user.value?.roles.includes(role) ?? false
  }

  return {
    user: readonly(user),
    isAuthenticated,
    isLoading: readonly(isLoading),
    login,
    logout,
    fetchCurrentUser,
    hasRole
  }
}
```

**Usage:**
```vue
<script setup lang="ts">
import { useAuthState } from '@/composables/useAuthState'

const { user, isAuthenticated, hasRole, logout } = useAuthState()

function handleLogout() {
  logout()
}
</script>

<template>
  <div v-if="isAuthenticated">
    <p>Logged in as: {{ user.email }}</p>
    <p>Username: {{ user.username }}</p>
    <p>Roles: {{ user.roles.join(', ') }}</p>

    <button v-if="hasRole('admin')" class="btn">
      Admin Panel
    </button>

    <button @click="handleLogout">Logout</button>
  </div>

  <div v-else>
    <p>Not logged in</p>
  </div>
</template>
```

**NEVER make direct API calls for auth** - always use `useAuthState` composable.

---

## Form Patterns

### Basic Form with Validation

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useToast } from '@/composables/useToast'
import { userApi } from '@/api/user'

interface FormData {
  username: string
  email: string
  age: number
}

const { showSuccess, showError } = useToast()

const formData = ref<FormData>({
  username: '',
  email: '',
  age: 18
})

const errors = ref<Partial<Record<keyof FormData, string>>>({})
const isSubmitting = ref(false)

// Validation
function validateForm(): boolean {
  errors.value = {}

  if (formData.value.username.length < 3) {
    errors.value.username = 'Username must be at least 3 characters'
  }

  if (!formData.value.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) {
    errors.value.email = 'Invalid email address'
  }

  if (formData.value.age < 18) {
    errors.value.age = 'Must be 18 or older'
  }

  return Object.keys(errors.value).length === 0
}

async function handleSubmit() {
  if (!validateForm()) {
    return
  }

  isSubmitting.value = true

  try {
    await userApi.createUser(formData.value)
    showSuccess('Form submitted successfully')
    // Reset form
    formData.value = { username: '', email: '', age: 18 }
  } catch (error) {
    showError('Failed to submit form')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <form @submit.prevent="handleSubmit" class="space-y-4">
    <div>
      <label for="username" class="block text-sm font-medium mb-1">
        Username
      </label>
      <input
        id="username"
        v-model="formData.username"
        type="text"
        class="w-full p-2 border rounded"
        :class="{ 'border-red-500': errors.username }"
      />
      <p v-if="errors.username" class="text-red-500 text-sm mt-1">
        {{ errors.username }}
      </p>
    </div>

    <div>
      <label for="email" class="block text-sm font-medium mb-1">
        Email
      </label>
      <input
        id="email"
        v-model="formData.email"
        type="email"
        class="w-full p-2 border rounded"
        :class="{ 'border-red-500': errors.email }"
      />
      <p v-if="errors.email" class="text-red-500 text-sm mt-1">
        {{ errors.email }}
      </p>
    </div>

    <div>
      <label for="age" class="block text-sm font-medium mb-1">
        Age
      </label>
      <input
        id="age"
        v-model.number="formData.age"
        type="number"
        class="w-full p-2 border rounded"
        :class="{ 'border-red-500': errors.age }"
      />
      <p v-if="errors.age" class="text-red-500 text-sm mt-1">
        {{ errors.age }}
      </p>
    </div>

    <button
      type="submit"
      :disabled="isSubmitting"
      class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 disabled:opacity-50"
    >
      {{ isSubmitting ? 'Submitting...' : 'Submit' }}
    </button>
  </form>
</template>
```

### Form Validation Composable

```typescript
// composables/useFormValidation.ts
import { ref, type Ref } from 'vue'

type ValidationRules<T> = {
  [K in keyof T]?: Array<(value: T[K]) => string | null>
}

export function useFormValidation<T extends Record<string, any>>(
  formData: Ref<T>,
  rules: ValidationRules<T>
) {
  const errors = ref<Partial<Record<keyof T, string>>>({}) as Ref<Partial<Record<keyof T, string>>>

  function validate(): boolean {
    errors.value = {}

    for (const field in rules) {
      const fieldRules = rules[field]
      if (!fieldRules) continue

      for (const rule of fieldRules) {
        const error = rule(formData.value[field])
        if (error) {
          errors.value[field] = error
          break
        }
      }
    }

    return Object.keys(errors.value).length === 0
  }

  function clearErrors() {
    errors.value = {}
  }

  return {
    errors,
    validate,
    clearErrors
  }
}
```

**Usage:**
```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useFormValidation } from '@/composables/useFormValidation'

const formData = ref({
  email: '',
  password: ''
})

const { errors, validate } = useFormValidation(formData, {
  email: [
    (value) => !value ? 'Email is required' : null,
    (value) => !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) ? 'Invalid email' : null
  ],
  password: [
    (value) => !value ? 'Password is required' : null,
    (value) => value.length < 8 ? 'Password must be at least 8 characters' : null
  ]
})

function handleSubmit() {
  if (validate()) {
    // Submit form
  }
}
</script>
```

---

## Modal/Dialog Pattern

### Modal Component

```vue
<!-- components/Modal.vue -->
<script setup lang="ts">
interface Props {
  open: boolean
  title?: string
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl'
}

interface Emits {
  (e: 'close'): void
  (e: 'confirm'): void
}

const props = withDefaults(defineProps<Props>(), {
  maxWidth: 'md'
})

const emit = defineEmits<Emits>()

const widthClasses = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-xl'
}

function handleClose() {
  emit('close')
}

function handleConfirm() {
  emit('confirm')
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-50 flex items-center justify-center"
    >
      <!-- Backdrop -->
      <div
        class="absolute inset-0 bg-black bg-opacity-50"
        @click="handleClose"
      ></div>

      <!-- Modal -->
      <div
        class="relative bg-white rounded-lg shadow-xl w-full mx-4"
        :class="widthClasses[maxWidth]"
      >
        <!-- Header -->
        <div class="flex items-center justify-between p-4 border-b">
          <h2 class="text-xl font-semibold">{{ title }}</h2>
          <button
            @click="handleClose"
            class="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>

        <!-- Content -->
        <div class="p-4">
          <slot />
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-2 p-4 border-t">
          <button
            @click="handleClose"
            class="px-4 py-2 text-gray-700 bg-gray-200 rounded hover:bg-gray-300"
          >
            Cancel
          </button>
          <button
            @click="handleConfirm"
            class="px-4 py-2 text-white bg-blue-500 rounded hover:bg-blue-600"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
```

**Usage:**
```vue
<script setup lang="ts">
import { ref } from 'vue'
import Modal from '@/components/Modal.vue'

const isModalOpen = ref(false)

function openModal() {
  isModalOpen.value = true
}

function closeModal() {
  isModalOpen.value = false
}

function handleConfirm() {
  console.log('Confirmed')
  closeModal()
}
</script>

<template>
  <div>
    <button @click="openModal">Open Modal</button>

    <Modal
      :open="isModalOpen"
      title="Confirm Action"
      @close="closeModal"
      @confirm="handleConfirm"
    >
      <p>Are you sure you want to perform this action?</p>
    </Modal>
  </div>
</template>
```

---

## Mutation Patterns with TanStack Query

### Update with Cache Invalidation

```vue
<script setup lang="ts">
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { useToast } from '@/composables/useToast'
import { userApi } from '@/api/user'
import type { User } from '@/model/User'

const queryClient = useQueryClient()
const { showSuccess, showError } = useToast()

const updateUserMutation = useMutation({
  mutationFn: ({ id, data }: { id: number; data: Partial<User> }) =>
    userApi.updateUser(id, data),

  onSuccess: (result, variables) => {
    // Invalidate affected queries
    queryClient.invalidateQueries({ queryKey: ['user', variables.id] })
    queryClient.invalidateQueries({ queryKey: ['users'] })

    showSuccess('User updated successfully')
  },

  onError: (error) => {
    showError('Failed to update user')
    console.error('Update error:', error)
  }
})

function handleSave(userId: number, updates: Partial<User>) {
  updateUserMutation.mutate({ id: userId, data: updates })
}
</script>

<template>
  <button
    @click="handleSave(123, { name: 'New Name' })"
    :disabled="updateUserMutation.isPending.value"
  >
    {{ updateUserMutation.isPending.value ? 'Saving...' : 'Save' }}
  </button>
</template>
```

### Delete with Optimistic Update

```vue
<script setup lang="ts">
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { useToast } from '@/composables/useToast'
import { userApi } from '@/api/user'
import type { User } from '@/model/User'

const queryClient = useQueryClient()
const { showSuccess, showError } = useToast()

const deleteUserMutation = useMutation({
  mutationFn: (userId: number) => userApi.deleteUser(userId),

  onMutate: async (userId) => {
    // Cancel outgoing queries
    await queryClient.cancelQueries({ queryKey: ['users'] })

    // Snapshot previous value
    const previousUsers = queryClient.getQueryData<User[]>(['users'])

    // Optimistically update
    if (previousUsers) {
      queryClient.setQueryData<User[]>(
        ['users'],
        previousUsers.filter(user => user.id !== userId)
      )
    }

    return { previousUsers }
  },

  onError: (error, userId, context) => {
    // Rollback on error
    if (context?.previousUsers) {
      queryClient.setQueryData(['users'], context.previousUsers)
    }
    showError('Failed to delete user')
  },

  onSuccess: () => {
    showSuccess('User deleted successfully')
  },

  onSettled: () => {
    // Always refetch to ensure consistency
    queryClient.invalidateQueries({ queryKey: ['users'] })
  }
})

function handleDelete(userId: number) {
  if (confirm('Are you sure you want to delete this user?')) {
    deleteUserMutation.mutate(userId)
  }
}
</script>
```

---

## State Management Patterns

### Singleton Composable for Global State

```typescript
// composables/useAppState.ts
import { ref, computed, readonly } from 'vue'

// Singleton state (shared across all components)
const sidebarOpen = ref(true)
const theme = ref<'light' | 'dark'>('light')

export function useAppState() {
  const isDarkMode = computed(() => theme.value === 'dark')

  function toggleSidebar() {
    sidebarOpen.value = !sidebarOpen.value
  }

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
  }

  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme
  }

  return {
    sidebarOpen: readonly(sidebarOpen),
    theme: readonly(theme),
    isDarkMode,
    toggleSidebar,
    toggleTheme,
    setTheme
  }
}
```

**Usage:**
```vue
<script setup lang="ts">
import { useAppState } from '@/composables/useAppState'

const { sidebarOpen, theme, isDarkMode, toggleSidebar, toggleTheme } = useAppState()
</script>

<template>
  <div :class="{ 'dark-mode': isDarkMode }">
    <button @click="toggleSidebar">
      {{ sidebarOpen ? 'Close' : 'Open' }} Sidebar
    </button>

    <button @click="toggleTheme">
      Switch to {{ isDarkMode ? 'Light' : 'Dark' }} Mode
    </button>
  </div>
</template>
```

### Local Component State

Use `ref` for **local UI state only**:
- Form inputs
- Modal open/closed
- Selected tab
- Temporary UI flags

```vue
<script setup lang="ts">
import { ref } from 'vue'

// Local UI state
const selectedTab = ref(0)
const isModalOpen = ref(false)
const searchQuery = ref('')

function selectTab(index: number) {
  selectedTab.value = index
}

function openModal() {
  isModalOpen.value = true
}
</script>

<template>
  <div>
    <div class="tabs">
      <button
        v-for="(tab, index) in ['Tab 1', 'Tab 2', 'Tab 3']"
        :key="index"
        @click="selectTab(index)"
        :class="{ 'active': selectedTab === index }"
      >
        {{ tab }}
      </button>
    </div>

    <input v-model="searchQuery" placeholder="Search..." />

    <button @click="openModal">Open Modal</button>
  </div>
</template>
```

---

## Debounced Search Pattern

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useDebounce } from '@vueuse/core'
import { useQuery } from '@tanstack/vue-query'
import { searchApi } from '@/api/search'

const searchTerm = ref('')
const debouncedSearchTerm = useDebounce(searchTerm, 300)

const { data: results, isLoading } = useQuery({
  queryKey: ['search', debouncedSearchTerm],
  queryFn: () => searchApi.search(debouncedSearchTerm.value),
  enabled: computed(() => debouncedSearchTerm.value.length > 2)
})
</script>

<template>
  <div>
    <input
      v-model="searchTerm"
      placeholder="Type to search..."
      class="w-full p-2 border rounded"
    />

    <div v-if="isLoading" class="mt-4">
      Searching...
    </div>

    <div v-else-if="results" class="mt-4 space-y-2">
      <div
        v-for="result in results"
        :key="result.id"
        class="p-2 border rounded"
      >
        {{ result.title }}
      </div>
    </div>

    <div v-else-if="searchTerm.length > 0 && searchTerm.length <= 2" class="mt-4 text-gray-500">
      Type at least 3 characters to search
    </div>
  </div>
</template>
```

---

## Summary

**Common Patterns:**
- ✅ **Singleton Composable** for global state (auth, app settings)
- ✅ **ref/reactive** for local UI state
- ✅ **Form validation** with composable pattern
- ✅ **Modal/Dialog** with Teleport and props
- ✅ **TanStack Query** mutations with cache invalidation
- ✅ **Optimistic updates** for better UX
- ✅ **Debounced search** with VueUse
- ✅ **Toast notifications** for user feedback

**State Management Guidelines:**
- **TanStack Query**: Server state (API data)
- **Singleton Composables**: Global client state (auth, theme)
- **ref/reactive**: Local component state (UI flags)

**See Also:**
- [data-fetching.md](data-fetching.md) - TanStack Query patterns
- [component-patterns.md](component-patterns.md) - Component structure
- [loading-and-error-states.md](loading-and-error-states.md) - Error handling
- [complete-examples.md](complete-examples.md) - Full examples
