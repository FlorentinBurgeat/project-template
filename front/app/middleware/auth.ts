export default defineNuxtRouteMiddleware(async () => {
  const { isAuthenticated, refresh } = useAuth()

  if (isAuthenticated.value) return

  const success = await refresh()
  if (!success) {
    return navigateTo('/')
  }
})
