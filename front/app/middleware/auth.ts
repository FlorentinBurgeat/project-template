export default defineNuxtRouteMiddleware(async () => {
  // The refresh-token cookie only exists in the browser (and is scoped to /api/auth), so the
  // session can't be restored during SSR: let the client decide after hydration.
  if (import.meta.server) return

  const { isAuthenticated, refresh } = useAuth()

  if (isAuthenticated.value) return

  const success = await refresh()
  if (!success) {
    return navigateTo(useLocalePath()('/'))
  }
})
