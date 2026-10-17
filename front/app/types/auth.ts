export interface AuthUser {
  sub: string
  email: string
  name: string
  given_name: string
  family_name: string
  email_verified: boolean
}

export interface PublicTokenResponse {
  access_token: string
  expires_in: number
  token_type: string
}

export interface LoginRedirectResponse {
  redirect_url: string
}
