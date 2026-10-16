import type { AuthUser, PublicTokenResponse, LoginRedirectResponse } from '~/types/auth'

const RETRY_DELAY_MS = 30_000

// Shared by every useAuth() caller (client only), so clearState() always cancels the pending refresh
let refreshTimer: ReturnType<typeof setTimeout> | null = null

// Decodes the JWT payload for display only: the backend validates the token, never trust these claims
function decodeJwtPayload(token: string): AuthUser | null {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0))
    const payload = JSON.parse(new TextDecoder().decode(bytes))
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

function statusOf(error: unknown): number | undefined {
  return (error as { statusCode?: number } | null)?.statusCode
}

export function useAuth() {
  const accessToken = useState<string | null>('auth:token', () => null)
  const user = useState<AuthUser | null>('auth:user', () => null)
  const isLoading = useState<boolean>('auth:loading', () => false)

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)

  const nuxtApp = useNuxtApp()
  const toast = useToast()

  function callbackUri(): string {
    return `${window.location.origin}/${nuxtApp.$i18n.locale.value}/auth/callback`
  }

  function scheduleRefresh(delayMs: number) {
    if (import.meta.server) return
    if (refreshTimer) clearTimeout(refreshTimer)
    refreshTimer = setTimeout(() => {
      refresh()
    }, delayMs)
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
    scheduleRefresh(Math.max((data.expires_in - 60) * 1000, 10_000))
  }

  async function redirectToKeycloak(endpoint: 'login' | 'register') {
    try {
      const data = await $fetch<LoginRedirectResponse>(`/api/auth/${endpoint}`, {
        params: { redirect_uri: callbackUri(), locale: nuxtApp.$i18n.locale.value },
        credentials: 'include'
      })
      window.location.href = data.redirect_url
    } catch (error) {
      console.error(`Failed to get ${endpoint} URL:`, error)
      toast.add({
        title: nuxtApp.$i18n.t('auth.errors.unavailable'),
        color: 'error',
        icon: 'i-lucide-alert-circle'
      })
    }
  }

  const login = () => redirectToKeycloak('login')
  const register = () => redirectToKeycloak('register')

  async function handleCallback(code: string, state: string): Promise<boolean> {
    isLoading.value = true
    try {
      const data = await $fetch<PublicTokenResponse>(`/api/auth/callback`, {
        method: 'POST',
        body: { code, state, redirect_uri: callbackUri() },
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

  /**
   * Refreshes the access token from the HTTP-only cookie. Returns whether the user is authenticated.
   * Only a 401 (cookie missing or rejected) signs the user out; an outage or network error keeps
   * the current session and retries later.
   */
  async function refresh(): Promise<boolean> {
    try {
      const data = await $fetch<PublicTokenResponse>(`/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include'
      })
      handleTokenResponse(data)
      return true
    } catch (error) {
      if (statusOf(error) === 401) {
        clearState()
        return false
      }
      if (accessToken.value) scheduleRefresh(RETRY_DELAY_MS)
      return isAuthenticated.value
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
