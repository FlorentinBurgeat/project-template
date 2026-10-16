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
const localePath = useLocalePath()
const { handleCallback, isAuthenticated } = useAuth()

// Authorization codes are single-use: remember the last one submitted so a remount
// or back-navigation doesn't replay it (which Keycloak would reject)
const lastSubmittedCode = useState<string | null>('auth:callback-code', () => null)

const error = ref(false)

// A repeated query param arrives as an array: keep the first non-empty value
function firstQueryValue(value: unknown): string | undefined {
  const first = Array.isArray(value) ? value[0] : value
  return typeof first === 'string' && first ? first : undefined
}

function fail() {
  error.value = true
  setTimeout(() => navigateTo(localePath('/')), 3000)
}

onMounted(async () => {
  const code = firstQueryValue(route.query.code)
  const state = firstQueryValue(route.query.state)
  const keycloakError = firstQueryValue(route.query.error)

  if (keycloakError || !code || !state) {
    if (keycloakError) {
      console.error('Keycloak returned an error:', keycloakError, route.query.error_description)
    }
    return fail()
  }

  if (code === lastSubmittedCode.value) {
    return isAuthenticated.value ? navigateTo(localePath('/dashboard')) : fail()
  }
  lastSubmittedCode.value = code

  const success = await handleCallback(code, state)

  if (success) {
    navigateTo(localePath('/dashboard'), { replace: true })
  } else {
    fail()
  }
})
</script>
