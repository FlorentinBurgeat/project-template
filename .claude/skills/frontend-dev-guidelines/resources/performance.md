# Performance Optimization

Patterns for optimizing Vue 3 component performance, preventing unnecessary computations, and avoiding memory leaks.

---

## computed() for Derived State

### Why computed() is Essential

Vue 3's `computed()` is **cached** and only recalculates when its dependencies change.

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'

interface Item {
  id: number
  name: string
  active: boolean
}

interface Props {
  items: Item[]
}

const props = defineProps<Props>()

const searchTerm = ref('')

// ❌ AVOID - Recalculates on every access
function getFilteredItems() {
  return props.items
    .filter(item => item.name.includes(searchTerm.value))
    .sort((a, b) => a.name.localeCompare(b.name))
}

// ✅ CORRECT - Cached, only recalculates when dependencies change
const filteredItems = computed(() => {
  return props.items
    .filter(item => item.name.toLowerCase().includes(searchTerm.value.toLowerCase()))
    .sort((a, b) => a.name.localeCompare(b.name))
})

// Access with .value in script
console.log(filteredItems.value)
</script>

<template>
  <div>
    <input v-model="searchTerm" placeholder="Search..." />

    <!-- No .value needed in template -->
    <div v-for="item in filteredItems" :key="item.id">
      {{ item.name }}
    </div>
  </div>
</template>
```

**When to use computed():**
- Filtering/sorting arrays
- Complex calculations
- Transforming data structures
- Any derived state based on reactive data
- Formatting values

**When NOT to use computed():**
- Side effects (use watch() instead)
- Async operations (use composables with useQuery)
- Simple property access

---

## Debounced Input

### Using VueUse

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { useDebounceFn, useDebounce } from '@vueuse/core'
import { useQuery } from '@tanstack/vue-query'
import { searchApi } from '@/api/search'

// Method 1: Debounce the value
const searchTerm = ref('')
const debouncedSearchTerm = useDebounce(searchTerm, 300)

const { data, isLoading } = useQuery({
  queryKey: ['search', debouncedSearchTerm],
  queryFn: () => searchApi.search(debouncedSearchTerm.value),
  enabled: computed(() => debouncedSearchTerm.value.length > 0)
})

// Method 2: Debounce the function
const searchQuery = ref('')

const performSearch = useDebounceFn((value: string) => {
  console.log('Searching for:', value)
  // Perform search
}, 300)

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  searchQuery.value = target.value
  performSearch(target.value)
}
</script>

<template>
  <div>
    <!-- Method 1: Debounced value -->
    <input v-model="searchTerm" placeholder="Search..." />

    <!-- Method 2: Debounced function -->
    <input :value="searchQuery" @input="handleInput" placeholder="Search..." />

    <div v-if="isLoading">Searching...</div>
    <SearchResults v-else :results="data" />
  </div>
</template>
```

**Optimal Debounce Timing:**
- **300-500ms**: Search/filtering
- **1000ms**: Auto-save
- **100-200ms**: Real-time validation

### Manual Debounce Implementation

```typescript
// composables/useDebounce.ts
import { ref, watch, type Ref } from 'vue'

export function useDebounce<T>(value: Ref<T>, delay: number): Ref<T> {
  const debouncedValue = ref(value.value) as Ref<T>
  let timeoutId: ReturnType<typeof setTimeout> | null = null

  watch(value, (newValue) => {
    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    timeoutId = setTimeout(() => {
      debouncedValue.value = newValue
    }, delay)
  })

  return debouncedValue
}
```

---

## Memory Leak Prevention

### Cleanup Timeouts/Intervals

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const count = ref(0)

let intervalId: ReturnType<typeof setInterval> | null = null
let timeoutId: ReturnType<typeof setTimeout> | null = null

onMounted(() => {
  // ✅ CORRECT - Setup interval
  intervalId = setInterval(() => {
    count.value++
  }, 1000)

  // ✅ CORRECT - Setup timeout
  timeoutId = setTimeout(() => {
    console.log('Delayed action')
  }, 5000)
})

onUnmounted(() => {
  // ✅ CORRECT - Cleanup on unmount
  if (intervalId) {
    clearInterval(intervalId)
  }

  if (timeoutId) {
    clearTimeout(timeoutId)
  }
})
</script>

<template>
  <div>{{ count }}</div>
</template>
```

### Cleanup Event Listeners

```vue
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const windowWidth = ref(window.innerWidth)

function handleResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  // ✅ CORRECT - Remove listener on unmount
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div>Window width: {{ windowWidth }}px</div>
</template>
```

### Using VueUse for Auto-Cleanup

```vue
<script setup lang="ts">
import { useIntervalFn, useEventListener } from '@vueuse/core'
import { ref } from 'vue'

const count = ref(0)

// ✅ Automatically cleaned up on unmount
const { pause, resume } = useIntervalFn(() => {
  count.value++
}, 1000)

// ✅ Automatically cleaned up on unmount
useEventListener(window, 'resize', () => {
  console.log('Window resized')
})
</script>

<template>
  <div>
    <p>Count: {{ count }}</p>
    <button @click="pause">Pause</button>
    <button @click="resume">Resume</button>
  </div>
</template>
```

**Note**: TanStack Query automatically handles cleanup for fetch operations.

---

## List Rendering Optimization

### Stable Keys with v-for

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Item {
  id: number
  name: string
}

const items = ref<Item[]>([
  { id: 1, name: 'Item 1' },
  { id: 2, name: 'Item 2' },
  { id: 3, name: 'Item 3' }
])
</script>

<template>
  <div>
    <!-- ✅ CORRECT - Stable unique keys -->
    <div v-for="item in items" :key="item.id">
      {{ item.name }}
    </div>

    <!-- ❌ AVOID - Index as key (unstable if list changes) -->
    <div v-for="(item, index) in items" :key="index">
      {{ item.name }}
    </div>

    <!-- ❌ NEVER - No key at all -->
    <div v-for="item in items">
      {{ item.name }}
    </div>
  </div>
</template>
```

**Why stable keys matter:**
- Vue reuses DOM elements efficiently
- Prevents unnecessary re-renders
- Maintains component state correctly
- Essential for transitions/animations

### v-show vs v-if

```vue
<script setup lang="ts">
import { ref } from 'vue'

const isVisible = ref(true)
</script>

<template>
  <div>
    <!-- ✅ Use v-show for frequently toggled elements -->
    <!-- Element stays in DOM, only CSS display changes -->
    <div v-show="isVisible" class="frequently-toggled">
      Content that toggles often
    </div>

    <!-- ✅ Use v-if for rarely shown elements -->
    <!-- Element is added/removed from DOM -->
    <div v-if="isVisible" class="rarely-shown">
      Heavy component that rarely shows
    </div>
  </div>
</template>
```

**v-show (CSS display toggle):**
- ✅ Frequently toggled (tabs, accordions)
- ✅ Simple content
- ❌ Initial render cost even if hidden

**v-if (DOM add/remove):**
- ✅ Rarely toggled
- ✅ Heavy components
- ✅ Conditional logic
- ❌ Higher cost when toggling

---

## watch() Performance

### Debounced Watch

```vue
<script setup lang="ts">
import { ref, watch } from 'vue'
import { useDebounceFn } from '@vueuse/core'

const searchTerm = ref('')
const searchResults = ref([])

// ✅ Debounce the watch callback
const debouncedSearch = useDebounceFn(async (newValue: string) => {
  if (newValue.length > 2) {
    const results = await searchApi.search(newValue)
    searchResults.value = results
  }
}, 300)

watch(searchTerm, (newValue) => {
  debouncedSearch(newValue)
})
</script>

<template>
  <div>
    <input v-model="searchTerm" placeholder="Search..." />
    <div v-for="result in searchResults" :key="result.id">
      {{ result.name }}
    </div>
  </div>
</template>
```

### Watch Specific Properties

```vue
<script setup lang="ts">
import { reactive, watch } from 'vue'

const form = reactive({
  username: '',
  email: '',
  password: '',
  address: {
    street: '',
    city: ''
  }
})

// ❌ AVOID - Watches entire object (deep watch is expensive)
watch(form, (newValue) => {
  console.log('Form changed:', newValue)
}, { deep: true })

// ✅ CORRECT - Watch specific properties
watch(() => form.username, (newUsername) => {
  console.log('Username changed:', newUsername)
})

// ✅ CORRECT - Watch multiple specific properties
watch([() => form.username, () => form.email], ([newUsername, newEmail]) => {
  console.log('Username or email changed:', newUsername, newEmail)
})

// ✅ CORRECT - Watch nested property
watch(() => form.address.city, (newCity) => {
  console.log('City changed:', newCity)
})
</script>
```

---

## Lazy Loading Heavy Dependencies

### Dynamic Imports

```vue
<script setup lang="ts">
import { ref } from 'vue'

const isExporting = ref(false)

// ❌ AVOID - Import heavy libraries at top level
// import jsPDF from 'jspdf'  // Large library loaded immediately
// import * as XLSX from 'xlsx'  // Large library loaded immediately

// ✅ CORRECT - Dynamic import when needed
async function handleExportPDF() {
  isExporting.value = true

  try {
    const { jsPDF } = await import('jspdf')
    const doc = new jsPDF()
    // Use it
    doc.text('Hello world', 10, 10)
    doc.save('document.pdf')
  } catch (error) {
    console.error('Failed to export PDF:', error)
  } finally {
    isExporting.value = false
  }
}

async function handleExportExcel() {
  isExporting.value = true

  try {
    const XLSX = await import('xlsx')
    const worksheet = XLSX.utils.json_to_sheet(data)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Data')
    XLSX.writeFile(workbook, 'export.xlsx')
  } catch (error) {
    console.error('Failed to export Excel:', error)
  } finally {
    isExporting.value = false
  }
}
</script>

<template>
  <div>
    <button @click="handleExportPDF" :disabled="isExporting">
      Export PDF
    </button>
    <button @click="handleExportExcel" :disabled="isExporting">
      Export Excel
    </button>
  </div>
</template>
```

### Lazy Load Components

```vue
<script setup lang="ts">
import { ref, defineAsyncComponent } from 'vue'

// ✅ Lazy load heavy components
const HeavyChart = defineAsyncComponent(() => import('@/components/HeavyChart.vue'))
const DataGrid = defineAsyncComponent(() => import('@/components/DataGrid.vue'))

const showChart = ref(false)
</script>

<template>
  <div>
    <button @click="showChart = true">Show Chart</button>

    <!-- Only loads when shown -->
    <HeavyChart v-if="showChart" :data="chartData" />
  </div>
</template>
```

**Note**: Route-level components are automatically lazy-loaded by Vue Router with dynamic imports.

---

## Large List Performance

### Virtual Scrolling

For lists with thousands of items, use virtual scrolling:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useVirtualList } from '@vueuse/core'

interface Item {
  id: number
  name: string
}

const allItems = ref<Item[]>(
  Array.from({ length: 10000 }, (_, i) => ({
    id: i,
    name: `Item ${i}`
  }))
)

// ✅ Virtual scrolling - only renders visible items
const { list, containerProps, wrapperProps } = useVirtualList(
  allItems,
  {
    itemHeight: 50,  // Height of each item
    overscan: 5       // Extra items to render outside viewport
  }
)
</script>

<template>
  <div v-bind="containerProps" class="h-96 overflow-auto border">
    <div v-bind="wrapperProps">
      <div
        v-for="{ data: item, index } in list"
        :key="item.id"
        class="h-[50px] flex items-center px-4 border-b"
      >
        {{ index }}: {{ item.name }}
      </div>
    </div>
  </div>
</template>
```

**When to use virtual scrolling:**
- Lists with 1000+ items
- Tables with many rows
- Infinite scroll implementations
- Chat message history

---

## Form Performance

### Avoid Watching Entire Form

```vue
<script setup lang="ts">
import { reactive, watch } from 'vue'

const form = reactive({
  username: '',
  email: '',
  password: '',
  bio: ''
})

// ❌ AVOID - Deep watch on entire form (expensive)
watch(form, (newValue) => {
  console.log('Form changed:', newValue)
}, { deep: true })

// ✅ CORRECT - Watch only fields you need
watch(() => form.username, (newUsername) => {
  // Validate username
  console.log('Username changed:', newUsername)
})

watch(() => form.email, (newEmail) => {
  // Validate email
  console.log('Email changed:', newEmail)
})

// ✅ CORRECT - Or watch multiple specific fields
watch([() => form.username, () => form.email], ([username, email]) => {
  console.log('Username or email changed:', username, email)
})
</script>

<template>
  <form>
    <input v-model="form.username" placeholder="Username" />
    <input v-model="form.email" type="email" placeholder="Email" />
    <input v-model="form.password" type="password" placeholder="Password" />
    <textarea v-model="form.bio" placeholder="Bio"></textarea>
  </form>
</template>
```

---

## Avoiding Unnecessary Reactivity

### When to Use shallowRef/shallowReactive

```vue
<script setup lang="ts">
import { ref, shallowRef, reactive, shallowReactive } from 'vue'

// ❌ Deeply reactive (more expensive)
const deepData = ref({
  user: {
    profile: {
      address: {
        city: 'Paris'
      }
    }
  }
})

// ✅ Shallow reactivity (less expensive)
// Only top-level properties are reactive
const shallowData = shallowRef({
  items: [1, 2, 3],
  config: { theme: 'dark' }
})

// When updating, replace entire object
function updateData() {
  // ✅ This triggers reactivity
  shallowData.value = {
    items: [4, 5, 6],
    config: { theme: 'light' }
  }

  // ❌ This won't trigger reactivity
  // shallowData.value.items.push(4)
}

// Use case: Large data structures you replace entirely
const apiResponse = shallowRef<ApiResponse | null>(null)

async function fetchData() {
  const response = await api.getData()
  // Replace entire object
  apiResponse.value = response
}
</script>

<template>
  <div>{{ shallowData.items }}</div>
</template>
```

**When to use shallow reactivity:**
- Large data structures
- Data you replace entirely (not mutate)
- API responses
- Read-only data

---

## Composable Performance

### Memoize Expensive Composables

```typescript
// composables/useExpensiveCalculation.ts
import { computed, type Ref } from 'vue'

export function useExpensiveCalculation(data: Ref<number[]>) {
  // ✅ computed() caches the result
  const result = computed(() => {
    // Expensive operation
    return data.value.reduce((sum, num) => {
      return sum + Math.sqrt(num) * Math.log(num)
    }, 0)
  })

  const formattedResult = computed(() => {
    return result.value.toFixed(2)
  })

  return {
    result,
    formattedResult
  }
}
```

**Usage:**
```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useExpensiveCalculation } from '@/composables/useExpensiveCalculation'

const numbers = ref([1, 2, 3, 4, 5])

// Automatically cached
const { result, formattedResult } = useExpensiveCalculation(numbers)
</script>

<template>
  <div>
    <p>Result: {{ formattedResult }}</p>
  </div>
</template>
```

---

## Complete Example: Optimized Component

```vue
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { useDebounce, useVirtualList } from '@vueuse/core'
import { productApi } from '@/api/product'
import type { Product } from '@/model/Product'

// Lazy load heavy component
const ProductDetails = defineAsyncComponent(() => import('@/components/ProductDetails.vue'))

// Search with debounce
const searchTerm = ref('')
const debouncedSearchTerm = useDebounce(searchTerm, 300)

// Fetch data
const { data: products, isLoading } = useQuery({
  queryKey: ['products', debouncedSearchTerm],
  queryFn: () => productApi.search(debouncedSearchTerm.value)
})

// Computed filtered data (cached)
const filteredProducts = computed(() => {
  if (!products.value) return []

  return products.value
    .filter(p => p.inStock)
    .sort((a, b) => a.name.localeCompare(b.name))
})

// Virtual scrolling for large lists
const { list, containerProps, wrapperProps } = useVirtualList(
  filteredProducts,
  { itemHeight: 80, overscan: 5 }
)

// Cleanup event listener
const windowWidth = ref(window.innerWidth)

function handleResize() {
  windowWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// Selected product
const selectedProduct = ref<Product | null>(null)

function selectProduct(product: Product) {
  selectedProduct.value = product
}
</script>

<template>
  <div class="p-6">
    <input
      v-model="searchTerm"
      placeholder="Search products..."
      class="w-full p-2 border rounded mb-4"
    />

    <div class="grid grid-cols-2 gap-4">
      <!-- Product list with virtual scrolling -->
      <div v-bind="containerProps" class="h-96 overflow-auto border rounded">
        <div v-if="isLoading" class="flex justify-center p-8">
          Loading...
        </div>

        <div v-else v-bind="wrapperProps">
          <div
            v-for="{ data: product } in list"
            :key="product.id"
            class="h-[80px] flex items-center px-4 border-b cursor-pointer hover:bg-gray-50"
            @click="selectProduct(product)"
          >
            <div>
              <h3 class="font-semibold">{{ product.name }}</h3>
              <p class="text-sm text-gray-600">${{ product.price }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Product details (lazy loaded) -->
      <div class="border rounded p-4">
        <ProductDetails v-if="selectedProduct" :product="selectedProduct" />
        <p v-else class="text-gray-500">Select a product to view details</p>
      </div>
    </div>

    <div class="mt-4 text-sm text-gray-500">
      Window width: {{ windowWidth }}px
    </div>
  </div>
</template>
```

---

## Summary

**Performance Checklist for Vue 3:**
- ✅ Use `computed()` for all derived state (filtering, sorting, transformations)
- ✅ Debounce search/filter inputs (300-500ms)
- ✅ Cleanup timeouts/intervals in `onUnmounted`
- ✅ Cleanup event listeners in `onUnmounted`
- ✅ Use stable `:key` in `v-for` (not index)
- ✅ `v-show` for frequently toggled, `v-if` for rarely shown
- ✅ Watch specific properties, not entire objects
- ✅ Virtual scrolling for large lists (1000+ items)
- ✅ Lazy load heavy libraries with dynamic imports
- ✅ Use `shallowRef`/`shallowReactive` for large data structures
- ✅ Route-level lazy loading handled automatically by Vue Router
- ✅ Use `defineAsyncComponent()` for heavy components

**Key Performance Principles:**
- computed() automatically caches derived state
- Vue's reactivity system is granular and efficient
- Components only update when their dependencies change
- TanStack Query handles fetch optimization automatically
- VueUse provides optimized composables with auto-cleanup

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [data-fetching.md](data-fetching.md) - TanStack Query optimization
- [complete-examples.md](complete-examples.md) - Performance patterns in context
