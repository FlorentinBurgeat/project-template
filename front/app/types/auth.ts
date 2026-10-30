import type { LoginRedirectResponse, PublicTokenResponse } from './api'

// API responses come from the generated contract (`types/api.ts`), never hand-written
export type { LoginRedirectResponse, PublicTokenResponse }

// Decoded Keycloak JWT payload (claims read from the access token), not an API response: hand-written on purpose
export interface AuthUser {
  sub: string
  email: string
  name: string
  given_name: string
  family_name: string
  email_verified: boolean
}
