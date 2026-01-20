# Component Patterns

Modern Vue 3 component architecture using Single File Components (SFC) with Composition API, emphasizing type safety, clean component design, and function syntax.

---

## Script Setup Pattern (PREFERRED)

### Why `<script setup>`

All Vue 3 components should use `<script setup lang="ts">` for:
- Cleaner, more concise syntax
- Better performance (less overhead)
- Automatic TypeScript inference
- Direct use of imports in template
- Compile-time optimizations

### Basic Pattern

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  /** User ID to display */
  userId: number
  /** Optional callback when action occurs */
  onAction?: () => void
}

const props = defineProps<Props>()

// Local state
const count = ref(0)

// Functions use function syntax
function handleClick() {
  count.value++
  props.onAction?.()
}
</script>

<template>
  <div>
    <p>User: {{ props.userId }}</p>
    <p>Count: {{ count }}</p>
    <button @click="handleClick">Click me</button>
  </div>
</template>
```

**Key Points:**
- Props interface defined with JSDoc comments
- Use `defineProps<Props>()` for type safety
- Local state with `ref()` or `reactive()`
- Functions use `function` keyword, not arrow functions

---

## Props with Defaults

### Using withDefaults

```vue
<script setup lang="ts">
interface Props {
  userId: number
  mode?: 'view' | 'edit'
  title?: string
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'view',
  title: 'Default Title'
})
</script>

<template>
  <div>
    <h2>{{ props.title }}</h2>
    <p>Mode: {{ props.mode }}</p>
  </div>
</template>
```

**When to use `withDefaults`:**
- Props with default values
- Optional props that need fallback
- Better than `??` in template

---

## Emits Pattern

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
</script>

<template>
  <div>
    <input :value="modelValue" @input="handleInput" />
    <button @click="handleSubmit">Submit</button>
  </div>
</template>
```

**Key Points:**
- Define all events in `Emits` interface
- Type event parameters
- Emit events with `emit()`
- Use function syntax for handlers

---

## Component Structure Template

### Recommended Order

```vue
<script setup lang="ts">
/**
 * Component description
 * What it does, when to use it
 */

// 1. IMPORTS
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuery } from '@tanstack/vue-query'
import type { Ref, ComputedRef } from 'vue'

// API imports
import { myFeatureApi } from '@/api/myFeature'
import type { MyData } from '@/model/MyData'

// Component imports
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'

// Composables
import { useAuthState } from '@/composables/useAuthState'

// 2. PROPS & EMITS
interface Props {
  /** The ID of the entity to display */
  entityId: number
  /** Optional callback when action completes */
  onComplete?: () => void
  /** Display mode */
  mode?: 'view' | 'edit'
}

interface Emits {
  (e: 'complete'): void
  (e: 'update', value: string): void
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'view'
})

const emit = defineEmits<Emits>()

// 3. COMPOSABLES & ROUTER
const router = useRouter()
const { user } = useAuthState()

// 4. DATA FETCHING
const { data, isLoading, error } = useQuery({
  queryKey: ['myEntity', () => props.entityId],
  queryFn: () => myFeatureApi.getEntity(props.entityId)
})

// 5. LOCAL STATE
const selectedItem = ref<string | null>(null)
const isEditing = ref(props.mode === 'edit')

// 6. COMPUTED VALUES
const filteredData = computed(() => {
  if (!data.value) return []
  return data.value.filter(item => item.active)
})

// 7. WATCHERS
watch(() => props.mode, (newMode) => {
  isEditing.value = newMode === 'edit'
})

// 8. LIFECYCLE HOOKS
onMounted(() => {
  console.log('Component mounted')
})

// 9. FUNCTIONS (use function syntax)
function handleItemSelect(itemId: string) {
  selectedItem.value = itemId
}

async function handleSave() {
  try {
    await myFeatureApi.updateEntity(props.entityId, {})
    emit('complete')
  } catch (error) {
    console.error('Failed to update:', error)
  }
}
</script>

<template>
  <div class="p-4">
    <div v-if="isLoading" class="flex justify-center p-8">
      <span>Loading...</span>
    </div>

    <div v-else-if="error" class="text-red-500">
      Error: {{ error.message }}
    </div>

    <Card v-else class="p-6">
      <h2 class="text-xl font-bold mb-4">My Component</h2>

      <div class="space-y-4">
        <div v-for="item in filteredData" :key="item.id">
          {{ item.name }}
        </div>
      </div>

      <Button @click="handleSave" class="mt-4">
        Save
      </Button>
    </Card>
  </div>
</template>

<style scoped>
/* Optional scoped styles */
</style>
```

---

## Component Separation

### When to Split Components

**Split into multiple components when:**
- Component exceeds 200-300 lines
- Multiple distinct responsibilities
- Reusable sections
- Complex nested template

**Example:**

```vue
<!-- ❌ AVOID - Monolithic component -->
<script setup lang="ts">
// 500+ lines
// Search logic
// Filter logic
// Grid logic
// Action panel logic
</script>

<template>
  <!-- Very long template -->
</template>

<!-- ✅ PREFER - Modular components -->
<!-- ParentContainer.vue -->
<script setup lang="ts">
import SearchAndFilter from './SearchAndFilter.vue'
import DataGrid from './DataGrid.vue'
import ActionPanel from './ActionPanel.vue'

function handleFilter(filters: Filters) {
  // Handle filtering
}

function handleAction(action: Action) {
  // Handle action
}
</script>

<template>
  <div>
    <SearchAndFilter @filter="handleFilter" />
    <DataGrid :data="filteredData" />
    <ActionPanel @action="handleAction" />
  </div>
</template>
```

### When to Keep Together

**Keep in same file when:**
- Component < 200 lines
- Tightly coupled logic
- Not reusable elsewhere
- Simple presentation component

---

## Component Communication

### Props Down, Events Up

```vue
<!-- Parent.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import ChildComponent from './ChildComponent.vue'

const selectedId = ref<string | null>(null)

function handleSelect(id: string) {
  selectedId.value = id
}
</script>

<template>
  <div>
    <ChildComponent
      :data="data"
      @select="handleSelect"
    />
    <p>Selected: {{ selectedId }}</p>
  </div>
</template>

<!-- Child.vue -->
<script setup lang="ts">
interface Props {
  data: Data[]
}

interface Emits {
  (e: 'select', id: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
</script>

<template>
  <div>
    <button
      v-for="item in data"
      :key="item.id"
      @click="emit('select', item.id)"
    >
      {{ item.name }}
    </button>
  </div>
</template>
```

### v-model Pattern

```vue
<!-- Parent.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import CustomInput from './CustomInput.vue'

const value = ref('')
</script>

<template>
  <CustomInput v-model="value" />
  <p>Value: {{ value }}</p>
</template>

<!-- CustomInput.vue -->
<script setup lang="ts">
interface Props {
  modelValue: string
}

interface Emits {
  (e: 'update:modelValue', value: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
}
</script>

<template>
  <input
    :value="modelValue"
    @input="handleInput"
    class="border p-2 rounded"
  />
</template>
```

---

## Slots Pattern

### Basic Slots

```vue
<!-- Card.vue -->
<script setup lang="ts">
interface Props {
  title?: string
}

const props = defineProps<Props>()
</script>

<template>
  <div class="border rounded p-4">
    <h3 v-if="title" class="font-bold mb-2">
      {{ title }}
    </h3>
    <slot />
  </div>
</template>

<!-- Usage -->
<Card title="My Card">
  <p>Content goes here</p>
</Card>
```

### Named Slots

```vue
<!-- Layout.vue -->
<script setup lang="ts">
</script>

<template>
  <div class="layout">
    <header class="header">
      <slot name="header" />
    </header>

    <main class="main">
      <slot />
    </main>

    <footer class="footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<!-- Usage -->
<Layout>
  <template #header>
    <h1>Header Content</h1>
  </template>

  <p>Main content</p>

  <template #footer>
    <p>Footer Content</p>
  </template>
</Layout>
```

### Scoped Slots

```vue
<!-- DataList.vue -->
<script setup lang="ts">
interface Props {
  items: Item[]
}

const props = defineProps<Props>()
</script>

<template>
  <div>
    <div v-for="item in items" :key="item.id">
      <slot :item="item" :index="item.id">
        <!-- Fallback content -->
        {{ item.name }}
      </slot>
    </div>
  </div>
</template>

<!-- Usage -->
<DataList :items="items">
  <template #default="{ item, index }">
    <strong>{{ index }}:</strong> {{ item.name }}
  </template>
</DataList>
```

---

## Composable Pattern

### Extract Reusable Logic

```typescript
// composables/useCounter.ts
import { ref, computed, readonly } from 'vue'

export function useCounter(initialValue = 0) {
  const count = ref(initialValue)

  const double = computed(() => count.value * 2)

  function increment() {
    count.value++
  }

  function decrement() {
    count.value--
  }

  function reset() {
    count.value = initialValue
  }

  return {
    count: readonly(count),
    double: readonly(double),
    increment,
    decrement,
    reset
  }
}
```

**Usage:**
```vue
<script setup lang="ts">
import { useCounter } from '@/composables/useCounter'

const { count, double, increment, decrement, reset } = useCounter(10)
</script>

<template>
  <div>
    <p>Count: {{ count }}</p>
    <p>Double: {{ double }}</p>
    <button @click="increment">+</button>
    <button @click="decrement">-</button>
    <button @click="reset">Reset</button>
  </div>
</template>
```

---

## Function Syntax Guidelines

### Prefer Function Declarations

```typescript
// ✅ PREFERRED - Function declaration
function handleClick() {
  console.log('Clicked')
}

function async fetchData() {
  return await api.getData()
}

// ❌ AVOID - Arrow function const
const handleClick = () => {
  console.log('Clicked')
}

const fetchData = async () => {
  return await api.getData()
}
```

**Why function declarations:**
- More readable
- Better for debugging (named functions)
- Consistent style
- Standard JavaScript practice

**Exception:** Arrow functions are OK for:
- Short inline callbacks: `.map(item => item.id)`
- Functional programming: `.filter(x => x > 0)`
- Preserving `this` context (rare in Composition API)

---

## Summary

**Modern Vue 3 Component Recipe:**
1. Use `<script setup lang="ts">` for all components
2. `defineProps<Props>()` with TypeScript interfaces
3. `withDefaults()` for props with default values
4. `defineEmits<Emits>()` for type-safe events
5. Use `function` syntax for event handlers and methods
6. Extract logic into composables
7. Props down, events up
8. Use `v-model` for two-way binding
9. Slots for flexible content
10. Router handles lazy loading automatically

**See Also:**
- [data-fetching.md](data-fetching.md) - useQuery patterns
- [loading-and-error-states.md](loading-and-error-states.md) - Loading state handling
- [complete-examples.md](complete-examples.md) - Full working examples
