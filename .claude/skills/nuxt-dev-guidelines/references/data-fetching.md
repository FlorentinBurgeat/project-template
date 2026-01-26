# Data Fetching Reference

## useFetch - Primary Method

Best for fetching data from API endpoints.

### Basic Usage

```vue
<script setup lang="ts">
const { data, pending, error, refresh } = await useFetch('/api/users')
</script>

<template>
  <div>
    <div v-if="pending">Loading...</div>
    <div v-else-if="error">Error: {{ error.message }}</div>
    <div v-else>
      <div v-for="user in data" :key="user.id">
        {{ user.name }}
      </div>
    </div>
  </div>
</template>
```

### Advanced Options

```vue
<script setup lang="ts">
const { data } = await useFetch('/api/users', {
  method: 'GET',
  query: { page: 1, limit: 10 },
  headers: {
    'Authorization': 'Bearer token'
  },
  // Transform response
  transform: (data) => data.map(u => ({
    ...u,
    fullName: `${u.firstName} ${u.lastName}`
  })),
  // Pick specific fields
  pick: ['id', 'name', 'email'],
  // Lazy load (don't block navigation)
  lazy: false,
  // Server-only fetch
  server: true
})
</script>
```

### Reactive Parameters

```vue
<script setup lang="ts">
const page = ref(1)
const { data } = await useFetch('/api/users', {
  query: { page }
})

// Manual refresh
await refresh()
</script>
```

### Key Options Reference

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `key` | `string` | auto | Unique key for deduplication |
| `server` | `boolean` | `true` | Fetch on server |
| `lazy` | `boolean` | `false` | Don't block navigation |
| `immediate` | `boolean` | `true` | Fetch immediately |
| `default` | `() => any` | - | Default value before fetch |
| `transform` | `(data) => any` | - | Transform response |
| `pick` | `string[]` | - | Pick specific keys |
| `watch` | `Array` | - | Watch sources for refetch |

## useAsyncData - Custom Logic

Best for non-fetch operations and complex data processing.

### Basic Usage

```vue
<script setup lang="ts">
// Custom async logic
const { data, pending } = await useAsyncData('users', async () => {
  const response = await $fetch('/api/users')
  return response.filter(user => user.active)
})
</script>
```

### With Key for Deduplication

```vue
<script setup lang="ts">
const { data } = await useAsyncData(`user-${id}`, () =>
  $fetch(`/api/users/${id}`)
)
</script>
```

### Lazy Variant

```vue
<script setup lang="ts">
// Doesn't block navigation
const { data } = await useLazyAsyncData('users', () =>
  $fetch('/api/users')
)
</script>
```

## Form Handling Pattern

```vue
<script setup lang="ts">
const form = ref({ name: '', email: '' })
const { execute, pending, error } = await useFetch('/api/submit', {
  method: 'POST',
  body: form,
  immediate: false, // Don't fetch immediately
  watch: false // Don't refetch on changes
})

const handleSubmit = async () => {
  await execute()
  if (!error.value) {
    console.log('Success!')
  }
}
</script>

<template>
  <form @submit.prevent="handleSubmit">
    <input v-model="form.name" />
    <input v-model="form.email" />
    <button :disabled="pending">Submit</button>
  </form>
</template>
```

## Best Practices

### Performance

✅ **DO**: Use SSR for SEO-critical pages
```typescript
// Enabled by default
const { data } = await useFetch('/api/data')
```

✅ **DO**: Use lazy loading for non-critical data
```typescript
const { data } = await useFetch('/api/data', { lazy: true })
```

❌ **DON'T**: Disable SSR for SEO pages
```typescript
// Bad for SEO
const { data } = await useFetch('/api/data', { server: false })
```

### TypeScript

✅ **DO**: Type your data
```typescript
interface User {
  id: number
  name: string
}

const { data } = await useFetch<User[]>('/api/users')
```
