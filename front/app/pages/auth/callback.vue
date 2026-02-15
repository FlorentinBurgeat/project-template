<template>
  <div class="flex items-center justify-center min-h-screen">
    <div class="text-center">
      <template v-if="error">
        <UIcon name="i-lucide-alert-circle" class="w-12 h-12 text-red-500 mx-auto mb-4" />
        <h2 class="text-lg font-semibold text-(--ui-text) mb-2">
          {{ $t('auth.callback.error') }}
        </h2>
        <p class="text-sm text-(--ui-text-muted)">
          {{ $t('auth.callback.redirecting') }}
        </p>
      </template>
      <template v-else>
        <UIcon
          name="i-lucide-loader-2"
          class="w-12 h-12 text-(--ui-primary) mx-auto mb-4 animate-spin"
        />
        <p class="text-sm text-(--ui-text-muted)">
          {{ $t('auth.callback.processing') }}
        </p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: false
})

const route = useRoute()
const { handleCallback } = useAuth()

const error = ref(false)

onMounted(async () => {
  const code = route.query.code as string | undefined

  if (!code) {
    error.value = true
    setTimeout(() => navigateTo('/'), 3000)
    return
  }

  const success = await handleCallback(code)

  if (success) {
    navigateTo('/dashboard')
  } else {
    error.value = true
    setTimeout(() => navigateTo('/'), 3000)
  }
})
</script>
