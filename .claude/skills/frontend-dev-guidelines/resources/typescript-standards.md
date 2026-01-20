# TypeScript Standards

TypeScript best practices for type safety and maintainability in Vue 3 frontend code.

---

## Strict Mode

### Configuration

TypeScript strict mode is **enabled** in the project:

```json
// tsconfig.json
{
    "compilerOptions": {
        "strict": true,
        "noImplicitAny": true,
        "strictNullChecks": true
    }
}
```

**This means:**
- No implicit `any` types
- Null/undefined must be handled explicitly
- Type safety enforced

---

## No `any` Type

### The Rule

```typescript
// ❌ NEVER use any
function handleData(data: any) {
    return data.something
}

// ✅ Use specific types
interface MyData {
    something: string
}

function handleData(data: MyData) {
    return data.something
}

// ✅ Or use unknown for truly unknown data
function handleUnknown(data: unknown) {
    if (typeof data === 'object' && data !== null && 'something' in data) {
        return (data as MyData).something
    }
}
```

**If you truly don't know the type:**
- Use `unknown` (forces type checking)
- Use type guards to narrow
- Document why type is unknown

---

## Explicit Return Types

### Function Return Types

```typescript
// ✅ CORRECT - Explicit return type
function getUser(id: number): Promise<User> {
    return apiClient.get(`/users/${id}`)
}

function calculateTotal(items: Item[]): number {
    return items.reduce((sum, item) => sum + item.price, 0)
}

// ❌ AVOID - Implicit return type (less clear)
function getUser(id: number) {
    return apiClient.get(`/users/${id}`)
}
```

### Composable Return Types

```typescript
// composables/useCounter.ts
import { ref, computed } from 'vue'

export function useCounter(initialValue = 0): {
  count: Readonly<Ref<number>>
  double: ComputedRef<number>
  increment: () => void
  decrement: () => void
} {
  const count = ref(initialValue)

  const double = computed(() => count.value * 2)

  function increment() {
    count.value++
  }

  function decrement() {
    count.value--
  }

  return {
    count: readonly(count),
    double,
    increment,
    decrement
  }
}
```

---

## Type Imports

### Use 'type' Keyword

```typescript
// ✅ CORRECT - Explicitly mark as type import
import type { User } from '@/model/User'
import type { Post } from '@/model/Post'
import type { Ref, ComputedRef } from 'vue'
import type { RouteLocationNormalized } from 'vue-router'

// ❌ AVOID - Mixed value and type imports
import { User } from '@/model/User'  // Unclear if type or value
```

**Benefits:**
- Clearly separates types from values
- Better tree-shaking
- Prevents circular dependencies
- TypeScript compiler optimization

---

## Component Prop Interfaces

### Interface Pattern

```vue
<script setup lang="ts">
/**
 * Props for MyComponent
 */
interface Props {
  /** The user ID to display */
  userId: number

  /** Optional callback when action completes */
  onComplete?: () => void

  /** Display mode for the component */
  mode?: 'view' | 'edit'

  /** Additional CSS classes */
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'view',
  className: ''
})
</script>

<template>
  <div :class="className">
    <!-- Component content -->
  </div>
</template>
```

**Key Points:**
- Separate interface for props
- JSDoc comments for each prop
- Optional props use `?`
- Use `withDefaults` for default values

### Props with Slots

```vue
<script setup lang="ts">
interface Props {
  title: string
  subtitle?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div>
    <h2>{{ title }}</h2>
    <p v-if="subtitle">{{ subtitle }}</p>
    <slot />  <!-- Default slot -->
    <slot name="footer" />  <!-- Named slot -->
  </div>
</template>
```

---

## Emits Typing

### Type-Safe Events

```vue
<script setup lang="ts">
interface Props {
  modelValue: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
  (e: 'submit'): void
  (e: 'delete', id: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}

function handleSubmit() {
  emit('submit')
}

function handleDelete(itemId: number) {
  emit('delete', itemId)
}
</script>

<template>
  <div>
    <input :value="modelValue" @input="handleInput" />
    <button @click="handleSubmit">Submit</button>
    <button @click="handleDelete(123)">Delete</button>
  </div>
</template>
```

---

## Utility Types

### Partial<T>

```typescript
// Make all properties optional
type UserUpdate = Partial<User>

function updateUser(id: number, updates: Partial<User>) {
    // updates can have any subset of User properties
}
```

### Pick<T, K>

```typescript
// Select specific properties
type UserPreview = Pick<User, 'id' | 'name' | 'email'>

const preview: UserPreview = {
    id: 1,
    name: 'John',
    email: 'john@example.com',
    // Other User properties not allowed
}
```

### Omit<T, K>

```typescript
// Exclude specific properties
type UserWithoutPassword = Omit<User, 'password' | 'passwordHash'>

const publicUser: UserWithoutPassword = {
    id: 1,
    name: 'John',
    email: 'john@example.com',
    // password and passwordHash not allowed
}
```

### Required<T>

```typescript
// Make all properties required
type RequiredConfig = Required<Config>  // All optional props become required
```

### Record<K, V>

```typescript
// Type-safe object/map
const userMap: Record<string, User> = {
    'user1': { id: 1, name: 'John' },
    'user2': { id: 2, name: 'Jane' },
}

// For variant classes
const variantClasses: Record<string, string> = {
    primary: 'bg-blue-500 text-white',
    secondary: 'bg-gray-200 text-gray-900',
    danger: 'bg-red-500 text-white'
}
```

---

## Type Guards

### Basic Type Guards

```typescript
function isUser(data: unknown): data is User {
    return (
        typeof data === 'object' &&
        data !== null &&
        'id' in data &&
        'name' in data
    )
}

// Usage
if (isUser(response)) {
    console.log(response.name)  // TypeScript knows it's User
}
```

### Discriminated Unions

```typescript
type LoadingState =
    | { status: 'idle' }
    | { status: 'loading' }
    | { status: 'success'; data: Data }
    | { status: 'error'; error: Error }

// In component
const state = ref<LoadingState>({ status: 'idle' })

// TypeScript narrows type based on status
if (state.value.status === 'success') {
    console.log(state.value.data)  // data available here
}

if (state.value.status === 'error') {
    console.error(state.value.error)  // error available here
}
```

---

## Generic Types

### Generic Functions

```typescript
function getById<T extends { id: number }>(items: T[], id: number): T | undefined {
    return items.find(item => item.id === id)
}

// Usage with type inference
const users: User[] = [...]
const user = getById(users, 123)  // Type: User | undefined
```

### Generic Components

```vue
<!-- GenericList.vue -->
<script setup lang="ts" generic="T">
interface Props {
  items: T[]
}

const props = defineProps<Props>()
</script>

<template>
  <div>
    <div v-for="(item, index) in items" :key="index">
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<!-- Usage -->
<GenericList :items="users">
  <template #default="{ item }">
    <UserCard :user="item" />
  </template>
</GenericList>
```

### Generic Composables

```typescript
// composables/useLocalStorage.ts
import { ref, watch, type Ref } from 'vue'

export function useLocalStorage<T>(
  key: string,
  defaultValue: T
): Ref<T> {
  const storedValue = localStorage.getItem(key)
  const value = ref<T>(
    storedValue ? JSON.parse(storedValue) : defaultValue
  ) as Ref<T>

  watch(value, (newValue) => {
    localStorage.setItem(key, JSON.stringify(newValue))
  }, { deep: true })

  return value
}

// Usage
const userPreferences = useLocalStorage<UserPreferences>('preferences', {
  theme: 'light',
  language: 'en'
})
```

---

## Type Assertions (Use Sparingly)

### When to Use

```typescript
// ✅ OK - When you know more than TypeScript
const element = document.getElementById('my-element') as HTMLInputElement
const value = element.value

// ✅ OK - API response that you've validated
const response = await api.getData()
const user = response.data as User  // You know the shape

// ✅ OK - Event target
function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  console.log(target.value)
}
```

### When NOT to Use

```typescript
// ❌ AVOID - Circumventing type safety
const data = getData() as any  // WRONG - defeats TypeScript

// ❌ AVOID - Unsafe assertion
const value = unknownValue as string  // Might not actually be string
```

---

## Null/Undefined Handling

### Optional Chaining

```typescript
// ✅ CORRECT
const name = user.value?.profile?.name

// Equivalent to:
const name = user.value && user.value.profile && user.value.profile.name
```

### Nullish Coalescing

```typescript
// ✅ CORRECT
const displayName = user.value?.name ?? 'Anonymous'

// Only uses default if null or undefined
// (Different from || which triggers on '', 0, false)
```

### Non-Null Assertion (Use Carefully)

```typescript
// ✅ OK - When you're certain value exists
const data = queryClient.getQueryData<Data>(['data'])!

// ⚠️ CAREFUL - Only use when you KNOW it's not null
// Better to check explicitly:
const data = queryClient.getQueryData<Data>(['data'])
if (data) {
    // Use data
}
```

---

## Ref Types

### Typing Refs

```vue
<script setup lang="ts">
import { ref, type Ref } from 'vue'

// ✅ CORRECT - Explicit type
const count = ref<number>(0)
const user = ref<User | null>(null)
const items = ref<Item[]>([])

// ✅ Type inference works too
const message = ref('hello')  // Type: Ref<string>

// ✅ Complex types
interface FormData {
  username: string
  email: string
}

const formData = ref<FormData>({
  username: '',
  email: ''
})

// Access with .value in script
console.log(count.value)
console.log(user.value?.name)
</script>

<template>
  <!-- No .value needed in template -->
  <div>{{ count }}</div>
  <div>{{ user?.name }}</div>
</template>
```

### Reactive Types

```vue
<script setup lang="ts">
import { reactive } from 'vue'

interface User {
  id: number
  name: string
  email: string
}

// ✅ CORRECT - Type inference
const user = reactive<User>({
  id: 1,
  name: 'John',
  email: 'john@example.com'
})

// Access without .value
console.log(user.name)
</script>

<template>
  <div>{{ user.name }}</div>
</template>
```

---

## Complete Example

```vue
<!-- UserProfile.vue -->
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { userApi } from '@/api/user'
import type { User } from '@/model/User'
import type { ComputedRef } from 'vue'

/**
 * Props for UserProfile component
 */
interface Props {
  /** User ID to display */
  userId: number

  /** Optional callback when profile is loaded */
  onLoad?: (user: User) => void

  /** Display mode */
  mode?: 'compact' | 'full'
}

/**
 * Emitted events
 */
interface Emits {
  (e: 'edit', userId: number): void
  (e: 'delete', userId: number): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'full'
})

const emit = defineEmits<Emits>()

// Data fetching with TanStack Query
const { data: user, isLoading, error } = useQuery({
  queryKey: ['user', () => props.userId],
  queryFn: () => userApi.getUser(props.userId)
})

// Computed value with explicit type
const displayName: ComputedRef<string> = computed(() => {
  if (!user.value) return ''
  return `${user.value.firstName} ${user.value.lastName}`
})

// Watch for successful load
watch(user, (newUser) => {
  if (newUser && props.onLoad) {
    props.onLoad(newUser)
  }
})

// Event handlers with function syntax
function handleEdit() {
  emit('edit', props.userId)
}

function handleDelete() {
  emit('delete', props.userId)
}

// Lifecycle
onMounted(() => {
  console.log('UserProfile mounted for user:', props.userId)
})
</script>

<template>
  <div class="user-profile">
    <div v-if="isLoading" class="loading">
      Loading...
    </div>

    <div v-else-if="error" class="error">
      Error: {{ error.message }}
    </div>

    <div v-else-if="user" class="content">
      <h2>{{ displayName }}</h2>
      <p>{{ user.email }}</p>

      <div v-if="mode === 'full'" class="actions">
        <button @click="handleEdit">Edit</button>
        <button @click="handleDelete">Delete</button>
      </div>
    </div>
  </div>
</template>
```

---

## Summary

**TypeScript Checklist:**
- ✅ Strict mode enabled
- ✅ No `any` type (use `unknown` if needed)
- ✅ Explicit return types on functions
- ✅ Use `import type` for type imports
- ✅ JSDoc comments on prop interfaces
- ✅ `defineProps<Props>()` for type-safe props
- ✅ `defineEmits<Emits>()` for type-safe events
- ✅ Utility types (Partial, Pick, Omit, Required, Record)
- ✅ Type guards for narrowing
- ✅ Optional chaining and nullish coalescing
- ✅ Explicit `Ref<T>` types when needed
- ❌ Avoid type assertions unless necessary

**See Also:**
- [component-patterns.md](component-patterns.md) - Component typing patterns
- [data-fetching.md](data-fetching.md) - API typing with TanStack Query
- [complete-examples.md](complete-examples.md) - Full typed examples
