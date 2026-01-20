# Styling Guide

Modern styling with Tailwind CSS utility classes and ShadCN Vue components for Vue 3 applications.

---

## Overview

**Stack:**
- **Tailwind CSS**: Utility-first CSS framework
- **ShadCN Vue**: Pre-built, accessible component library
- **Scoped Styles**: Component-specific CSS when needed

**No CSS-in-JS**, no styled-components, no emotion. Keep styling simple and declarative.

---

## Component Composition for Styling

### Create Styled Components, Don't Style Everywhere

**Key Principle**: If a component needs different styles or behaviors, create a new component or use props for variants.

#### Bad: Styling the same component differently everywhere

```vue
<!-- ❌ AVOID - Styling Button differently in multiple places -->
<template>
  <!-- Page 1 -->
  <button class="px-4 py-2 bg-blue-500 text-white rounded">
    Action
  </button>

  <!-- Page 2 -->
  <button class="px-6 py-3 bg-blue-600 text-white rounded-lg shadow-md">
    Action
  </button>

  <!-- Page 3 -->
  <button class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600">
    Action
  </button>
</template>
```

#### Good: Create a reusable component with variants

```vue
<!-- components/AppButton.vue -->
<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'danger'
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md'
})

const variantClasses = {
  primary: 'bg-blue-500 hover:bg-blue-600 text-white',
  secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-900',
  danger: 'bg-red-500 hover:bg-red-600 text-white'
}

const sizeClasses = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg'
}
</script>

<template>
  <button
    class="rounded font-medium transition-colors"
    :class="[variantClasses[variant], sizeClasses[size]]"
  >
    <slot />
  </button>
</template>

<!-- Usage everywhere -->
<AppButton variant="primary">Save</AppButton>
<AppButton variant="danger" size="sm">Delete</AppButton>
<AppButton variant="secondary" size="lg">Cancel</AppButton>
```

#### Good: Create specialized components

```vue
<!-- components/PrimaryButton.vue -->
<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <Button
    class="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700"
  >
    <slot />
  </Button>
</template>

<!-- components/DangerButton.vue -->
<script setup lang="ts">
import { Button } from '@/components/ui/button'
</script>

<template>
  <Button
    variant="destructive"
    class="shadow-md"
  >
    <slot />
  </Button>
</template>

<!-- Usage -->
<PrimaryButton>Save</PrimaryButton>
<DangerButton>Delete</DangerButton>
```

### When to Create a New Component

Create a new component when:
- ✅ The style pattern repeats more than 2-3 times
- ✅ You need different behaviors (variants, sizes, states)
- ✅ The component represents a distinct UI pattern
- ✅ You want to enforce consistency across the app

Don't create a component when:
- ❌ Used only once in the entire app
- ❌ The styling is trivial (1-2 classes)
- ❌ It's page-specific and won't be reused

---

## Tailwind CSS Basics

### Utility Classes in Template

Apply Tailwind classes directly in the `<template>`:

```vue
<script setup lang="ts">
import { ref } from 'vue'

const isActive = ref(false)
</script>

<template>
  <div class="p-4 bg-white rounded-lg shadow-md">
    <h2 class="text-2xl font-bold text-gray-900 mb-4">
      Title
    </h2>
    <p class="text-gray-600 leading-relaxed">
      Content goes here
    </p>
    <button
      class="mt-4 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
      :class="{ 'opacity-50': !isActive }"
    >
      Click me
    </button>
  </div>
</template>
```

---

## Common Tailwind Patterns

### Layout & Spacing

```vue
<template>
  <!-- Padding & Margin -->
  <div class="p-4">           <!-- padding: 1rem -->
  <div class="px-6 py-4">     <!-- padding-x: 1.5rem, padding-y: 1rem -->
  <div class="mt-8 mb-4">     <!-- margin-top: 2rem, margin-bottom: 1rem -->

  <!-- Flexbox -->
  <div class="flex items-center justify-between">
    <span>Left</span>
    <span>Right</span>
  </div>

  <!-- Flex column with gap -->
  <div class="flex flex-col gap-4">
    <div>Item 1</div>
    <div>Item 2</div>
  </div>

  <!-- Grid -->
  <div class="grid grid-cols-3 gap-4">
    <div>Col 1</div>
    <div>Col 2</div>
    <div>Col 3</div>
  </div>

  <!-- Center content -->
  <div class="flex items-center justify-center min-h-screen">
    <p>Centered content</p>
  </div>
</template>
```

### Typography

```vue
<template>
  <!-- Font sizes -->
  <h1 class="text-4xl font-bold">Large heading</h1>
  <h2 class="text-2xl font-semibold">Medium heading</h2>
  <p class="text-base text-gray-700">Body text</p>
  <small class="text-sm text-gray-500">Small text</small>

  <!-- Font weights -->
  <p class="font-light">Light</p>
  <p class="font-normal">Normal</p>
  <p class="font-medium">Medium</p>
  <p class="font-semibold">Semibold</p>
  <p class="font-bold">Bold</p>

  <!-- Text alignment -->
  <p class="text-left">Left aligned</p>
  <p class="text-center">Center aligned</p>
  <p class="text-right">Right aligned</p>

  <!-- Text colors -->
  <p class="text-gray-900">Dark gray</p>
  <p class="text-blue-500">Blue</p>
  <p class="text-red-500">Red</p>
</template>
```

### Responsive Design

```vue
<template>
  <!-- Mobile-first responsive -->
  <div class="
    w-full           <!-- Full width on mobile -->
    md:w-1/2         <!-- 50% width on medium screens -->
    lg:w-1/3         <!-- 33% width on large screens -->
  ">
    Responsive width
  </div>

  <!-- Hide/show on different screens -->
  <div class="hidden md:block">
    Only visible on medium+ screens
  </div>

  <!-- Responsive padding -->
  <div class="p-4 md:p-8 lg:p-12">
    Responsive padding
  </div>

  <!-- Responsive grid -->
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
    <div>Item 1</div>
    <div>Item 2</div>
    <div>Item 3</div>
  </div>
</template>
```

---

## Dynamic Classes with :class

### Conditional Classes with Props

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'danger'
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  disabled: false
})

const buttonClasses = computed(() => ({
  'bg-blue-500 hover:bg-blue-600': props.variant === 'primary',
  'bg-gray-200 hover:bg-gray-300': props.variant === 'secondary',
  'bg-red-500 hover:bg-red-600': props.variant === 'danger',
  'opacity-50 cursor-not-allowed': props.disabled
}))
</script>

<template>
  <button
    class="px-4 py-2 rounded text-white transition-colors"
    :class="buttonClasses"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>
```

---

## ShadCN Vue Components

### Importing Components

```vue
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
</script>

<template>
  <Card>
    <CardHeader>
      <CardTitle>Form Title</CardTitle>
    </CardHeader>
    <CardContent>
      <div class="space-y-4">
        <div>
          <Label for="name">Name</Label>
          <Input id="name" placeholder="Enter name" />
        </div>
        <Button>Submit</Button>
      </div>
    </CardContent>
  </Card>
</template>
```

### Common ShadCN Components

```vue
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
</script>

<template>
  <div class="space-y-4">
    <!-- Button variants -->
    <Button variant="default">Default</Button>
    <Button variant="destructive">Delete</Button>
    <Button variant="outline">Outline</Button>
    <Button variant="ghost">Ghost</Button>

    <!-- Input -->
    <Input placeholder="Type something..." />

    <!-- Badge -->
    <Badge>New</Badge>
    <Badge variant="secondary">Updated</Badge>

    <!-- Alert -->
    <Alert>
      <AlertDescription>
        This is an alert message
      </AlertDescription>
    </Alert>
  </div>
</template>
```

---

## Scoped Styles

### When to Use <style scoped>

Use scoped styles for:
- Component-specific styles not covered by Tailwind
- Complex animations
- Pseudo-elements (::before, ::after)
- Custom CSS that doesn't fit utility classes

```vue
<script setup lang="ts">
</script>

<template>
  <div class="custom-card">
    <div class="custom-header">Header</div>
    <div class="custom-content">Content</div>
  </div>
</template>

<style scoped>
.custom-card {
  position: relative;
}

.custom-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(to right, #3b82f6, #8b5cf6);
}

.custom-content {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
```

---

## Form Styling

### Complete Form Example

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const email = ref('')
const password = ref('')

function handleSubmit() {
  console.log('Submit', email.value, password.value)
}
</script>

<template>
  <Card class="max-w-md mx-auto">
    <CardHeader>
      <CardTitle>Login</CardTitle>
    </CardHeader>
    <CardContent>
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div class="space-y-2">
          <Label for="email">Email</Label>
          <Input
            id="email"
            v-model="email"
            type="email"
            placeholder="Enter your email"
            class="w-full"
          />
        </div>

        <div class="space-y-2">
          <Label for="password">Password</Label>
          <Input
            id="password"
            v-model="password"
            type="password"
            placeholder="Enter your password"
            class="w-full"
          />
        </div>

        <Button type="submit" class="w-full">
          Login
        </Button>
      </form>
    </CardContent>
  </Card>
</template>
```

---

## Loading States

### Loading Spinner Component

```vue
<!-- components/LoadingSpinner.vue -->
<script setup lang="ts">
interface Props {
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md'
})

const sizeClasses = {
  sm: 'w-6 h-6 border-2',
  md: 'w-10 h-10 border-4',
  lg: 'w-16 h-16 border-4'
}
</script>

<template>
  <div
    class="loading-spinner border-gray-200 border-t-blue-500 rounded-full"
    :class="sizeClasses[size]"
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

<!-- Usage -->
<div class="flex items-center justify-center p-8">
  <LoadingSpinner size="lg" />
</div>
```

---

## Best Practices

### Do's

✅ **Create reusable components with props for variants**
```vue
<AppButton variant="primary" size="lg">Action</AppButton>
```

✅ **Use Tailwind utilities first**
```vue
<div class="p-4 bg-blue-500 rounded">Content</div>
```

✅ **Use ShadCN components for complex UI**
```vue
<Button variant="outline">Click me</Button>
```

✅ **Extract repeated patterns to components**
```vue
<!-- Create <PrimaryButton>, <DangerButton> instead of repeating classes -->
```

✅ **Use computed for dynamic classes**
```vue
const buttonClasses = computed(() => ({
  'bg-blue-500': props.variant === 'primary'
}))
```

### Don'ts

❌ **Don't style the same component differently everywhere**
```vue
<!-- Bad - inconsistent styling -->
<button class="px-4 py-2 bg-blue-500 ...">Action</button>
<button class="px-6 py-3 bg-blue-600 ...">Action</button>
```

❌ **Don't write custom CSS for things Tailwind provides**
```vue
<!-- Bad -->
<style scoped>
.my-box { padding: 1rem; }
</style>

<!-- Good -->
<div class="p-4">
```

❌ **Don't use inline styles**
```vue
<!-- Bad -->
<div :style="{ padding: '16px' }">

<!-- Good -->
<div class="p-4">
```

---

## Summary

**Styling in Vue 3:**
1. **Component Composition**: Create styled components with props for variants
2. **Tailwind First**: Use utility classes for styling
3. **ShadCN Components**: Pre-built, accessible UI components
4. **Scoped Styles**: Only for animations, pseudo-elements, complex CSS
5. **Responsive**: Mobile-first with Tailwind's responsive modifiers
6. **Props for Variants**: Use props to handle different behaviors and styles
7. **Reusability**: Extract repeated patterns into components

**See Also:**
- [component-patterns.md](component-patterns.md) - Component structure
- [complete-examples.md](complete-examples.md) - Full styled examples
