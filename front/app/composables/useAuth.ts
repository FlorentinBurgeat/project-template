import type { AuthUser, PublicTokenResponse, LoginRedirectResponse } from '~/types/auth'

function decodeJwtPayload(token: string): AuthUser | null {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(base64))
    return {
      sub: payload.sub ?? '',
      email: payload.email ?? '',
      name: payload.name ?? '',
      given_name: payload.given_name ?? '',
      family_name: payload.family_name ?? '',
      email_verified: payload.email_verified ?? false
    }
  } catch {
    return null
  }
}

export function useAuth() {
  const accessToken = useState<string | null>('auth:token', () => null)
  const user = useState<AuthUser | null>('auth:user', () => null)
  const isLoading = useState<boolean>('auth:loading', () => false)

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)

  const nuxtApp = useNuxtApp()

  function getLocale(): string {
    return nuxtApp.$i18n.locale.value as string
  }

  let refreshTimer: ReturnType<typeof setTimeout> | null = null

  function scheduleRefresh(expiresIn: number) {
    if (refreshTimer) clearTimeout(refreshTimer)
    const delay = Math.max((expiresIn - 60) * 1000, 10_000)
    refreshTimer = setTimeout(() => {
      refresh()
    }, delay)
  }

  function clearState() {
    accessToken.value = null
    user.value = null
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  function handleTokenResponse(data: PublicTokenResponse) {
    accessToken.value = data.access_token
    user.value = decodeJwtPayload(data.access_token)
    scheduleRefresh(data.expires_in)
  }

  async function login() {
    const origin = window.location.origin
    const redirectUri = `${origin}/${getLocale()}/auth/callback`

    try {
      const data = await $fetch<LoginRedirectResponse>(`/api/auth/login`, {
        params: { redirect_uri: redirectUri }
      })
      window.location.href = data.redirect_url
    } catch (error) {
      console.error('Failed to get login URL:', error)
    }
  }

  async function register() {
    const origin = window.location.origin
    const redirectUri = `${origin}/${getLocale()}/auth/callback`

    try {
      const data = await $fetch<LoginRedirectResponse>(`/api/auth/register`, {
        params: { redirect_uri: redirectUri }
      })
      window.location.href = data.redirect_url
    } catch (error) {
      console.error('Failed to get registration URL:', error)
    }
  }

  async function handleCallback(code: string): Promise<boolean> {
    const origin = window.location.origin
    const redirectUri = `${origin}/${getLocale()}/auth/callback`

    isLoading.value = true
    try {
      const data = await $fetch<PublicTokenResponse>(`/api/auth/callback`, {
        method: 'POST',
        params: { code, redirect_uri: redirectUri },
        credentials: 'include'
      })
      handleTokenResponse(data)
      return true
    } catch (error) {
      console.error('Callback failed:', error)
      clearState()
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function refresh(): Promise<boolean> {
    try {
      const data = await $fetch<PublicTokenResponse>(`/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include'
      })
      handleTokenResponse(data)
      return true
    } catch {
      clearState()
      return false
    }
  }

  async function logout() {
    try {
      await $fetch(`/api/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      })
    } catch (error) {
      console.error('Logout request failed:', error)
    } finally {
      clearState()
      navigateTo('/')
    }
  }

  function getAuthHeaders(): Record<string, string> {
    if (!accessToken.value) return {}
    return { Authorization: `Bearer ${accessToken.value}` }
  }

  return {
    accessToken: readonly(accessToken),
    user: readonly(user),
    isAuthenticated,
    isLoading: readonly(isLoading),
    login,
    register,
    handleCallback,
    refresh,
    logout,
    getAuthHeaders
  }
}
