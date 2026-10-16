import { describe, expect, it } from 'vitest'
import { registerEndpoint } from '@nuxt/test-utils/runtime'
import { readBody } from 'h3'

// Unsigned JWT with { sub: 'user-1', email: 'jane@example.com' }
const accessToken = `e30.${btoa(JSON.stringify({ sub: 'user-1', email: 'jane@example.com' }))}.sig`

describe('useAuth', () => {
  it('posts code and state in the callback body and stores the access token in memory', async () => {
    let received: Record<string, string> | undefined
    registerEndpoint('/api/auth/callback', {
      method: 'POST',
      handler: async (event) => {
        received = await readBody(event)
        return { access_token: accessToken, expires_in: 900, token_type: 'Bearer' }
      }
    })

    const { handleCallback, isAuthenticated, user, logout } = useAuth()
    const success = await handleCallback('the-code', 'the-state')

    expect(success).toBe(true)
    expect(received).toMatchObject({ code: 'the-code', state: 'the-state' })
    expect(received?.redirect_uri).toMatch(/\/en\/auth\/callback$/)
    expect(isAuthenticated.value).toBe(true)
    expect(user.value?.email).toBe('jane@example.com')

    registerEndpoint('/api/auth/logout', { method: 'POST', handler: () => null })
    await logout()
    expect(isAuthenticated.value).toBe(false)
  })

  it('decodes non-ASCII claims as UTF-8', async () => {
    const payload = new TextEncoder().encode(
      JSON.stringify({ sub: 'user-2', name: 'Hélène Müller' })
    )
    const token = `e30.${btoa(String.fromCharCode(...payload))}.sig`
    registerEndpoint('/api/auth/refresh', {
      method: 'POST',
      handler: () => ({ access_token: token, expires_in: 900, token_type: 'Bearer' })
    })

    const { refresh, user } = useAuth()

    expect(await refresh()).toBe(true)
    expect(user.value?.name).toBe('Hélène Müller')
  })

  it('keeps the session when Keycloak is unavailable, and signs out on 401', async () => {
    registerEndpoint('/api/auth/refresh', {
      method: 'POST',
      handler: () => ({ access_token: accessToken, expires_in: 900, token_type: 'Bearer' })
    })
    const { refresh, isAuthenticated } = useAuth()
    await refresh()

    registerEndpoint('/api/auth/refresh', {
      method: 'POST',
      handler: () => {
        throw createError({ statusCode: 503 })
      }
    })
    expect(await refresh()).toBe(true)
    expect(isAuthenticated.value).toBe(true)

    registerEndpoint('/api/auth/refresh', {
      method: 'POST',
      handler: () => {
        throw createError({ statusCode: 401 })
      }
    })
    expect(await refresh()).toBe(false)
    expect(isAuthenticated.value).toBe(false)
  })

  it('stays signed out when the backend rejects the callback', async () => {
    registerEndpoint('/api/auth/callback', {
      method: 'POST',
      handler: () => {
        throw createError({ statusCode: 401 })
      }
    })

    const { handleCallback, isAuthenticated } = useAuth()

    expect(await handleCallback('the-code', 'forged-state')).toBe(false)
    expect(isAuthenticated.value).toBe(false)
  })
})
