# Loading & Error States

Proper loading and error state handling prevents layout shift and provides better user experience.

---

## ⚠️ CRITICAL RULE: Avoid Layout Shift

### The Problem

```vue
<!-- ❌ AVOID - Component with layout shift -->
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { productApi } from '@/api/product'

interface Props {
  productId: number
}

const props = defineProps<Props>()

const { data, isLoading } = useQuery({
  queryKey: ['product', () => props.productId],
  queryFn: () => productApi.getProduct(props.productId)
})
</script>

<template>
  <!-- WRONG: This causes layout shift and poor UX -->
  <div v-if="isLoading" class="p-4">
    <LoadingSpinner />
  </div>

  <!-- Different container size! -->
  <div v-else class="p-8">
    <ProductCard :product="data" />
  </div>
</template>
```

**Why this is bad:**
1. **Layout Shift**: Content position jumps when loading completes
2. **CLS (Cumulative Layout Shift)**: Poor Core Web Vital score
3. **Jarring UX**: Page structure changes suddenly
4. **Lost Scroll Position**: User loses place on page

### The Solutions

**Option 1: Conditional Content with Same Container (PREFERRED)**

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { productApi } from '@/api/product'

interface Props {
  productId: number
}

const props = defineProps<Props>()

const { data, isLoading, error } = useQuery({
  queryKey: ['product', () => props.productId],
  queryFn: () => productApi.getProduct(props.productId)
})
</script>

<template>
  <!-- ✅ GOOD: Same container, conditional content -->
  <div class="p-8 min-h-[400px]">
    <LoadingSpinner v-if="isLoading" />
    <ErrorMessage v-else-if="error" :error="error" />
    <ProductCard v-else :product="data" />
  </div>
</template>
```

**Option 2: Loading Overlay**

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { productApi } from '@/api/product'
import LoadingOverlay from '@/components/LoadingOverlay.vue'

interface Props {
  productId: number
}

const props = defineProps<Props>()

const { data, isLoading, error } = useQuery({
  queryKey: ['product', () => props.productId],
  queryFn: () => productApi.getProduct(props.productId)
})
</script>

<template>
  <LoadingOverlay :loading="isLoading">
    <div class="p-8">
      <ErrorMessage v-if="error" :error="error" />
      <ProductCard v-else-if="data" :product="data" />
    </div>
  </LoadingOverlay>
</template>
```

---

## TanStack Query Loading States

### Reactive Refs in Vue

TanStack Query returns **reactive refs** in Vue. Access them directly in template, use `.value` in script.

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/api/user'

interface Props {
  userId: number
}

const props = defineProps<Props>()

// All return values are reactive refs
const { data, isLoading, isError, error, refetch } = useQuery({
  queryKey: ['user', () => props.userId],
  queryFn: () => userApi.getUser(props.userId)
})

// Access with .value in script
function handleRetry() {
  console.log('Loading:', isLoading.value)
  refetch()
}
</script>

<template>
  <div class="p-4">
    <!-- No .value needed in template -->
    <div v-if="isLoading" class="flex justify-center">
      <LoadingSpinner />
    </div>

    <div v-else-if="isError" class="text-red-500">
      <p>Error: {{ error.message }}</p>
      <button @click="handleRetry" class="mt-2 px-4 py-2 bg-blue-500 text-white rounded">
        Retry
      </button>
    </div>

    <div v-else-if="data">
      <h2 class="text-xl font-bold">{{ data.name }}</h2>
      <p>{{ data.email }}</p>
    </div>
  </div>
</template>
```

**Key Points:**
- `isLoading` is true while fetching
- `isError` is true if query failed
- `error` contains the error object
- `data` is undefined until loaded
- All values are reactive refs

---

## Loading Patterns

### Pattern 1: v-if / v-else-if / v-else

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { postApi } from '@/api/post'

interface Props {
  postId: number
}

const props = defineProps<Props>()

const { data, isLoading, isError, error } = useQuery({
  queryKey: ['post', () => props.postId],
  queryFn: () => postApi.getPost(props.postId)
})
</script>

<template>
  <div class="container p-6">
    <!-- Loading state -->
    <div v-if="isLoading" class="flex justify-center items-center h-64">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Error state -->
    <div v-else-if="isError" class="text-center p-8">
      <p class="text-red-500 text-lg mb-4">{{ error.message }}</p>
      <button class="px-4 py-2 bg-blue-500 text-white rounded">
        Try Again
      </button>
    </div>

    <!-- Success state -->
    <div v-else class="space-y-4">
      <h1 class="text-3xl font-bold">{{ data.title }}</h1>
      <p class="text-gray-700">{{ data.content }}</p>
    </div>
  </div>
</template>
```

### Pattern 2: Inline Conditional with Same Layout

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { postApi } from '@/api/post'

const { data, isLoading } = useQuery({
  queryKey: ['posts'],
  queryFn: () => postApi.getPosts()
})
</script>

<template>
  <div class="grid grid-cols-3 gap-4">
    <!-- Same layout for loading and content -->
    <div
      v-for="i in 6"
      :key="i"
      class="border rounded p-4 h-64"
    >
      <template v-if="isLoading">
        <!-- Skeleton placeholder -->
        <div class="animate-pulse space-y-4">
          <div class="h-4 bg-gray-200 rounded w-3/4"></div>
          <div class="h-32 bg-gray-200 rounded"></div>
          <div class="h-4 bg-gray-200 rounded"></div>
        </div>
      </template>

      <template v-else-if="data && data[i - 1]">
        <!-- Actual content -->
        <h3 class="font-bold mb-2">{{ data[i - 1].title }}</h3>
        <img :src="data[i - 1].image" class="w-full h-32 object-cover rounded" />
        <p class="mt-2">{{ data[i - 1].excerpt }}</p>
      </template>
    </div>
  </div>
</template>
```

### Pattern 3: Multiple Independent Loading States

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import { postApi } from '@/api/post'
import { commentApi } from '@/api/comment'

interface Props {
  userId: number
}

const props = defineProps<Props>()

// Independent queries
const userQuery = useQuery({
  queryKey: ['user', () => props.userId],
  queryFn: () => userApi.getUser(props.userId)
})

const postsQuery = useQuery({
  queryKey: ['posts', () => props.userId],
  queryFn: () => postApi.getUserPosts(props.userId)
})

const commentsQuery = useQuery({
  queryKey: ['comments', () => props.userId],
  queryFn: () => commentApi.getUserComments(props.userId)
})
</script>

<template>
  <div class="space-y-8">
    <!-- User section -->
    <section class="border rounded p-4 min-h-[200px]">
      <LoadingSpinner v-if="userQuery.isLoading.value" />
      <ErrorMessage v-else-if="userQuery.isError.value" :error="userQuery.error.value" />
      <UserProfile v-else :user="userQuery.data.value" />
    </section>

    <!-- Posts section -->
    <section class="border rounded p-4 min-h-[300px]">
      <LoadingSpinner v-if="postsQuery.isLoading.value" />
      <ErrorMessage v-else-if="postsQuery.isError.value" :error="postsQuery.error.value" />
      <PostsList v-else :posts="postsQuery.data.value" />
    </section>

    <!-- Comments section -->
    <section class="border rounded p-4 min-h-[200px]">
      <LoadingSpinner v-if="commentsQuery.isLoading.value" />
      <ErrorMessage v-else-if="commentsQuery.isError.value" :error="commentsQuery.error.value" />
      <CommentsList v-else :comments="commentsQuery.data.value" />
    </section>
  </div>
</template>
```

**Benefits:**
- Each section loads independently
- User sees partial content sooner
- Better perceived performance

---

## Loading Components

### LoadingSpinner Component

```vue
<!-- components/LoadingSpinner.vue -->
<script setup lang="ts">
interface Props {
  size?: 'sm' | 'md' | 'lg'
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  color: 'blue'
})

const sizeClasses = {
  sm: 'w-6 h-6 border-2',
  md: 'w-10 h-10 border-4',
  lg: 'w-16 h-16 border-4'
}

const colorClasses = {
  blue: 'border-gray-200 border-t-blue-500',
  green: 'border-gray-200 border-t-green-500',
  red: 'border-gray-200 border-t-red-500'
}
</script>

<template>
  <div
    class="loading-spinner rounded-full"
    :class="[sizeClasses[size], colorClasses[color]]"
  />
</template>

<style scoped>
.loading-spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
```

**Usage:**
```vue
<template>
  <div v-if="isLoading" class="flex justify-center p-8">
    <LoadingSpinner size="lg" color="blue" />
  </div>
</template>
```

### LoadingOverlay Component

```vue
<!-- components/LoadingOverlay.vue -->
<script setup lang="ts">
interface Props {
  loading: boolean
}

const props = defineProps<Props>()
</script>

<template>
  <div class="relative">
    <slot />

    <!-- Overlay -->
    <div
      v-if="loading"
      class="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center z-10"
    >
      <LoadingSpinner size="lg" />
    </div>
  </div>
</template>
```

**Usage:**
```vue
<template>
  <LoadingOverlay :loading="isLoading">
    <div class="p-8">
      <Content :data="data" />
    </div>
  </LoadingOverlay>
</template>
```

---

## Skeleton Loading

### Tailwind Skeleton Pattern

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { productApi } from '@/api/product'

const { data, isLoading } = useQuery({
  queryKey: ['products'],
  queryFn: () => productApi.getProducts()
})
</script>

<template>
  <div class="space-y-4">
    <div
      v-for="i in 5"
      :key="i"
      class="border rounded-lg p-6"
    >
      <!-- Skeleton (same layout as content) -->
      <div v-if="isLoading" class="animate-pulse space-y-4">
        <div class="h-6 bg-gray-200 rounded w-1/2"></div>
        <div class="h-40 bg-gray-200 rounded"></div>
        <div class="h-4 bg-gray-200 rounded w-3/4"></div>
        <div class="h-4 bg-gray-200 rounded w-full"></div>
        <div class="flex gap-2">
          <div class="h-10 bg-gray-200 rounded w-24"></div>
          <div class="h-10 bg-gray-200 rounded w-24"></div>
        </div>
      </div>

      <!-- Actual content -->
      <div v-else-if="data && data[i - 1]">
        <h3 class="text-xl font-bold mb-4">{{ data[i - 1].name }}</h3>
        <img :src="data[i - 1].image" class="w-full h-40 object-cover rounded mb-4" />
        <p class="text-gray-700 mb-2">{{ data[i - 1].description }}</p>
        <p class="text-lg font-semibold mb-4">${{ data[i - 1].price }}</p>
        <div class="flex gap-2">
          <button class="px-4 py-2 bg-blue-500 text-white rounded">Buy</button>
          <button class="px-4 py-2 bg-gray-200 text-gray-700 rounded">Details</button>
        </div>
      </div>
    </div>
  </div>
</template>
```

**Key**: Skeleton must have **same layout** as actual content (no shift)

---

## Error Handling

### Toast Notifications

Create a composable for toast notifications:

```typescript
// composables/useToast.ts
import { ref } from 'vue'

interface Toast {
  id: number
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
}

const toasts = ref<Toast[]>([])
let nextId = 0

export function useToast() {
  function showToast(message: string, type: Toast['type'] = 'info') {
    const id = nextId++
    toasts.value.push({ id, message, type })

    // Auto-dismiss after 3 seconds
    setTimeout(() => {
      removeToast(id)
    }, 3000)
  }

  function removeToast(id: number) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
      toasts.value.splice(index, 1)
    }
  }

  function showSuccess(message: string) {
    showToast(message, 'success')
  }

  function showError(message: string) {
    showToast(message, 'error')
  }

  function showWarning(message: string) {
    showToast(message, 'warning')
  }

  function showInfo(message: string) {
    showToast(message, 'info')
  }

  return {
    toasts,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeToast
  }
}
```

**Toast Component:**
```vue
<!-- components/ToastContainer.vue -->
<script setup lang="ts">
import { useToast } from '@/composables/useToast'

const { toasts, removeToast } = useToast()

const typeClasses = {
  success: 'bg-green-500',
  error: 'bg-red-500',
  warning: 'bg-orange-500',
  info: 'bg-blue-500'
}
</script>

<template>
  <div class="fixed top-4 right-4 z-50 space-y-2">
    <div
      v-for="toast in toasts"
      :key="toast.id"
      class="px-6 py-4 rounded-lg text-white shadow-lg min-w-[300px] flex justify-between items-center"
      :class="typeClasses[toast.type]"
    >
      <span>{{ toast.message }}</span>
      <button @click="removeToast(toast.id)" class="ml-4 text-white hover:text-gray-200">
        ✕
      </button>
    </div>
  </div>
</template>
```

**Usage in components:**
```vue
<script setup lang="ts">
import { useToast } from '@/composables/useToast'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { userApi } from '@/api/user'

const { showSuccess, showError } = useToast()
const queryClient = useQueryClient()

const updateMutation = useMutation({
  mutationFn: (updates) => userApi.updateUser(updates),

  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['user'] })
    showSuccess('User updated successfully')
  },

  onError: (error) => {
    showError('Failed to update user')
    console.error('Update error:', error)
  }
})

function handleUpdate() {
  updateMutation.mutate({ name: 'New Name' })
}
</script>

<template>
  <button @click="handleUpdate">Update</button>
</template>
```

### Error Display Component

```vue
<!-- components/ErrorMessage.vue -->
<script setup lang="ts">
interface Props {
  error: Error | null
  retry?: () => void
}

const props = defineProps<Props>()
</script>

<template>
  <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
    <p class="text-red-600 text-lg font-semibold mb-2">Something went wrong</p>
    <p class="text-red-500 mb-4">{{ error.message }}</p>
    <button
      v-if="retry"
      @click="retry"
      class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
    >
      Try Again
    </button>
  </div>
</template>
```

**Usage:**
```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import ErrorMessage from '@/components/ErrorMessage.vue'

const { data, isLoading, isError, error, refetch } = useQuery({
  queryKey: ['users'],
  queryFn: () => userApi.getUsers()
})
</script>

<template>
  <div>
    <LoadingSpinner v-if="isLoading" />
    <ErrorMessage v-else-if="isError" :error="error" :retry="refetch" />
    <UserList v-else :users="data" />
  </div>
</template>
```

---

## Complete Examples

### Example 1: Basic Loading & Error Handling

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { postApi } from '@/api/post'
import LoadingSpinner from '@/components/LoadingSpinner.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
import type { Post } from '@/model/Post'

interface Props {
  postId: number
}

const props = defineProps<Props>()

const { data, isLoading, isError, error, refetch } = useQuery({
  queryKey: ['post', () => props.postId],
  queryFn: () => postApi.getPost(props.postId)
})
</script>

<template>
  <div class="max-w-4xl mx-auto p-6">
    <div v-if="isLoading" class="flex justify-center items-center h-64">
      <LoadingSpinner size="lg" />
    </div>

    <ErrorMessage v-else-if="isError" :error="error" :retry="refetch" />

    <article v-else class="space-y-6">
      <h1 class="text-4xl font-bold">{{ data.title }}</h1>
      <div class="text-gray-500">
        By {{ data.author }} • {{ data.date }}
      </div>
      <div class="prose">
        {{ data.content }}
      </div>
    </article>
  </div>
</template>
```

### Example 2: Mutation with Toast Feedback

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { useToast } from '@/composables/useToast'
import { commentApi } from '@/api/comment'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

interface Props {
  postId: number
}

const props = defineProps<Props>()

const queryClient = useQueryClient()
const { showSuccess, showError } = useToast()

const commentText = ref('')

const addCommentMutation = useMutation({
  mutationFn: (text: string) => commentApi.addComment(props.postId, text),

  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['comments', props.postId] })
    showSuccess('Comment added successfully')
    commentText.value = ''
  },

  onError: (error) => {
    showError('Failed to add comment')
    console.error('Comment error:', error)
  }
})

function handleSubmit() {
  if (!commentText.value.trim()) {
    showWarning('Comment cannot be empty')
    return
  }

  addCommentMutation.mutate(commentText.value)
}
</script>

<template>
  <div class="space-y-4">
    <Input
      v-model="commentText"
      placeholder="Write a comment..."
      :disabled="addCommentMutation.isPending.value"
    />

    <Button
      @click="handleSubmit"
      :disabled="addCommentMutation.isPending.value"
    >
      {{ addCommentMutation.isPending.value ? 'Adding...' : 'Add Comment' }}
    </Button>
  </div>
</template>
```

### Example 3: Multiple Loading States with Skeleton

```vue
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { dashboardApi } from '@/api/dashboard'

const statsQuery = useQuery({
  queryKey: ['stats'],
  queryFn: () => dashboardApi.getStats()
})

const recentPostsQuery = useQuery({
  queryKey: ['recentPosts'],
  queryFn: () => dashboardApi.getRecentPosts()
})

const activityQuery = useQuery({
  queryKey: ['activity'],
  queryFn: () => dashboardApi.getActivity()
})
</script>

<template>
  <div class="space-y-8 p-6">
    <!-- Stats Section -->
    <section class="grid grid-cols-3 gap-4">
      <div
        v-for="i in 3"
        :key="i"
        class="border rounded-lg p-6"
      >
        <div v-if="statsQuery.isLoading.value" class="animate-pulse space-y-2">
          <div class="h-4 bg-gray-200 rounded w-1/2"></div>
          <div class="h-8 bg-gray-200 rounded w-3/4"></div>
        </div>

        <div v-else-if="statsQuery.data.value">
          <p class="text-gray-500 text-sm">{{ statsQuery.data.value[i - 1].label }}</p>
          <p class="text-3xl font-bold">{{ statsQuery.data.value[i - 1].value }}</p>
        </div>
      </div>
    </section>

    <!-- Recent Posts Section -->
    <section class="border rounded-lg p-6 min-h-[300px]">
      <h2 class="text-2xl font-bold mb-4">Recent Posts</h2>

      <LoadingSpinner v-if="recentPostsQuery.isLoading.value" />
      <ErrorMessage v-else-if="recentPostsQuery.isError.value" :error="recentPostsQuery.error.value" />

      <div v-else class="space-y-4">
        <div
          v-for="post in recentPostsQuery.data.value"
          :key="post.id"
          class="border-b pb-4"
        >
          <h3 class="font-semibold">{{ post.title }}</h3>
          <p class="text-gray-600 text-sm">{{ post.excerpt }}</p>
        </div>
      </div>
    </section>

    <!-- Activity Section -->
    <section class="border rounded-lg p-6 min-h-[200px]">
      <h2 class="text-2xl font-bold mb-4">Recent Activity</h2>

      <LoadingSpinner v-if="activityQuery.isLoading.value" />
      <ErrorMessage v-else-if="activityQuery.isError.value" :error="activityQuery.error.value" />

      <ul v-else class="space-y-2">
        <li
          v-for="activity in activityQuery.data.value"
          :key="activity.id"
          class="flex items-center gap-2"
        >
          <span class="text-gray-500 text-sm">{{ activity.timestamp }}</span>
          <span>{{ activity.action }}</span>
        </li>
      </ul>
    </section>
  </div>
</template>
```

---

## Anti-Patterns

### ❌ What NOT to Do

```vue
<!-- ❌ NEVER - Different container sizes -->
<template>
  <div v-if="isLoading" class="h-20">
    <LoadingSpinner />
  </div>

  <div v-else class="h-96">  <!-- Different height! -->
    <Content :data="data" />
  </div>
</template>

<!-- ❌ NEVER - Conditional rendering without error handling -->
<template>
  <div v-if="isLoading">Loading...</div>
  <div v-else>{{ data.name }}</div>  <!-- What if error? -->
</template>

<!-- ❌ NEVER - Accessing data without checking if loaded -->
<template>
  <div>
    <h1>{{ data.title }}</h1>  <!-- data might be undefined! -->
  </div>
</template>
```

### ✅ What TO Do

```vue
<!-- ✅ BEST - Same container, all states handled -->
<template>
  <div class="min-h-[400px] p-6">
    <LoadingSpinner v-if="isLoading" />
    <ErrorMessage v-else-if="isError" :error="error" />
    <Content v-else-if="data" :data="data" />
  </div>
</template>

<!-- ✅ GOOD - Skeleton with same layout -->
<template>
  <div class="grid grid-cols-3 gap-4">
    <div v-for="i in 6" :key="i" class="border rounded p-4 h-64">
      <div v-if="isLoading" class="animate-pulse space-y-4">
        <div class="h-4 bg-gray-200 rounded"></div>
        <div class="h-32 bg-gray-200 rounded"></div>
      </div>
      <Card v-else-if="data && data[i - 1]" :item="data[i - 1]" />
    </div>
  </div>
</template>

<!-- ✅ GOOD - LoadingOverlay -->
<template>
  <LoadingOverlay :loading="isLoading">
    <div class="p-6">
      <ErrorMessage v-if="isError" :error="error" />
      <Content v-else-if="data" :data="data" />
    </div>
  </LoadingOverlay>
</template>
```

---

## Summary

**Loading States:**
- ✅ **PREFERRED**: v-if/v-else with same container size
- ✅ **GOOD**: LoadingOverlay for complex layouts
- ✅ **GOOD**: Skeleton loaders with matching layout
- ❌ **NEVER**: Different container sizes causing layout shift
- ❌ **NEVER**: Missing error state handling

**TanStack Query in Vue:**
- All return values are reactive refs
- Use directly in template (no `.value`)
- Access with `.value` in script
- `isLoading`, `isError`, `error`, `data` are reactive

**Error Handling:**
- ✅ Toast notifications for user feedback
- ✅ Error display components with retry option
- ✅ onError callbacks in queries/mutations
- ✅ Always handle all states: loading, error, success

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [data-fetching.md](data-fetching.md) - useQuery patterns
- [styling-guide.md](styling-guide.md) - Loading component styling
