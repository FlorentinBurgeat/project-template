---
paths:
  - "back/**"
---

# Backend authentication (Keycloak)

Keycloak handles identity (OAuth2 / OpenID Connect). Each project fork can use its own realm on a shared instance, set via `KEYCLOAK_REALM_NAME` (e.g. `myproject-prod`).

## Endpoints

- Public: `GET /api/auth/login`, `GET /api/auth/register`, `POST /api/auth/callback`, `POST /api/auth/refresh`, `POST /api/auth/logout`.
- Everything else requires `Authorization: Bearer <token>`, validated by Spring Security OAuth2 Resource Server.

## Flow

1. Frontend redirects to Keycloak login.
2. Keycloak redirects back with an authorization code.
3. Backend exchanges the code for tokens (`POST /api/auth/callback`).
4. Access token: in memory on the frontend (15 min). Refresh token: HTTP-only cookie (7 days).
5. Refresh via `POST /api/auth/refresh` using the cookie.

## Code (`/features/keycloak`)

- `KeycloakConfig` - admin client
- `RealmInitializationService` - on startup creates/configures the realm, frontend client (public SPA), backend client (confidential), token lifespans, OIDC mappers
- `KeycloakAuthController` / `KeycloakAuthService` - OAuth2 flow and cookie handling
- `UserManagementService` - optional, for custom registration flows (default is Keycloak-managed registration)

`/features/authentication` is the deprecated custom JWT implementation; do not extend it.

## Security

HTTP-only + SameSite cookies, CORS limited to frontend origins, Keycloak brute-force protection and session limits. Identity providers (Google, Facebook, GitHub, Azure AD, SAML) are configured in Keycloak, not in code.

See `back/KEYCLOAK_SETUP.md` for setup, deployment modes and troubleshooting.
