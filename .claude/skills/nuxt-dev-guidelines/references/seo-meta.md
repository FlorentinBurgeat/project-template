# SEO and Meta Tags Reference

## useSeoMeta - Primary Method

Type-safe and XSS-safe meta tag management. This is the recommended approach for most SEO needs.

### Static Meta Tags

```vue
<script setup lang="ts">
useSeoMeta({
  title: 'My Page Title',
  description: 'Page description for search engines',
  ogTitle: 'My Page Title',
  ogDescription: 'Page description for social sharing',
  ogImage: 'https://example.com/image.png',
  ogUrl: 'https://example.com/page',
  twitterCard: 'summary_large_image',
  twitterTitle: 'My Page Title',
  twitterDescription: 'Page description for Twitter',
  twitterImage: 'https://example.com/image.png',
})
</script>
```

### Reactive Meta Tags

```vue
<script setup lang="ts">
const title = ref('Dynamic Title')
useSeoMeta({
  title,
  description: () => `Description for ${title.value}`,
})
</script>
```

### Server-Only Optimization

```vue
<script setup lang="ts">
// For static meta tags, optimize by only setting on server
if (import.meta.server) {
  useSeoMeta({
    robots: 'index, follow',
    ogImage: 'https://example.com/static-image.png',
  })
}

// Only use reactive meta tags outside the condition when necessary
const dynamicTitle = ref('My Title')
useSeoMeta({
  title: () => dynamicTitle.value,
  ogTitle: () => dynamicTitle.value,
})
</script>
```

## useHead - Advanced Head Management

For complex head customization beyond SEO meta tags.

### Basic Usage

```vue
<script setup lang="ts">
useHead({
  title: 'My Page',
  titleTemplate: '%s | My Site',
  meta: [
    { name: 'description', content: 'Page description' },
    { name: 'keywords', content: 'nuxt, vue, ssr' }
  ],
  link: [
    { rel: 'canonical', href: 'https://example.com/page' }
  ],
  htmlAttrs: {
    lang: 'en'
  }
})
</script>
```

### Reactive Head

```vue
<script setup lang="ts">
const isDark = ref(false)
useHead({
  bodyAttrs: {
    class: computed(() => isDark.value ? 'dark' : 'light')
  }
})
</script>
```

### Complete Example

```vue
<script setup lang="ts">
useHead({
  title: 'My Page',
  titleTemplate: '%s | My Site',
  meta: [
    { name: 'description', content: 'Page description' },
    { name: 'keywords', content: 'nuxt, vue, ssr' },
    { charset: 'utf-8' },
    { name: 'viewport', content: 'width=device-width, initial-scale=1' }
  ],
  link: [
    { rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' },
    { rel: 'canonical', href: 'https://example.com/page' }
  ],
  script: [
    { src: 'https://analytics.example.com/script.js', async: true }
  ],
  htmlAttrs: {
    lang: 'en'
  },
  bodyAttrs: {
    class: 'dark-mode'
  }
})
</script>
```

## definePageMeta - Page Configuration

Define meta at page level for route-specific configuration.

```vue
<script setup lang="ts">
definePageMeta({
  title: 'User Profile',
  layout: 'default',
  middleware: ['auth'],
  keepalive: true
})
</script>
```

## SEO Best Practices

✅ **DO**: Use `useSeoMeta` for all SEO meta tags
✅ **DO**: Set unique titles and descriptions per page
✅ **DO**: Include Open Graph and Twitter Card tags
✅ **DO**: Use semantic HTML
✅ **DO**: Add canonical URLs for duplicate content

### Complete SEO Example

```vue
<script setup lang="ts">
const route = useRoute()
const title = 'My Awesome Page'
const description = 'A comprehensive guide to building amazing web apps'
const image = 'https://example.com/og-image.png'
const url = `https://example.com${route.path}`

useSeoMeta({
  // Basic meta
  title,
  description,

  // Open Graph
  ogTitle: title,
  ogDescription: description,
  ogImage: image,
  ogUrl: url,
  ogType: 'website',
  ogSiteName: 'My Site',

  // Twitter Card
  twitterCard: 'summary_large_image',
  twitterTitle: title,
  twitterDescription: description,
  twitterImage: image,
  twitterSite: '@mysite',

  // Additional
  robots: 'index, follow',
  author: 'My Name',
})

useHead({
  link: [
    { rel: 'canonical', href: url }
  ]
})
</script>
```
