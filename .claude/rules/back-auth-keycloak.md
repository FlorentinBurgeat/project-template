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

1. Frontend gets the Keycloak URL from `/api/auth/login` (or `/register`). The backend only accepts a `redirect_uri` listed in `keycloak.frontend.redirect-uris` (exact match), adds `state` + PKCE S256, and stores both in a 10-minute `auth_flow` cookie.
2. Keycloak redirects back with an authorization code and `state`.
3. Backend checks `state` against the cookie and exchanges the code + PKCE verifier for tokens (`POST /api/auth/callback`, JSON body).
4. Access token: in memory on the frontend. Refresh token: HTTP-only cookie, lifetime from Keycloak's `refresh_expires_in`.
5. Refresh via `POST /api/auth/refresh` using the cookie. Keycloak rejecting the token → 401 (cookie cleared); Keycloak unreachable → 503 (cookie kept).

## URLs

- `keycloak.url` (`KEYCLOAK_INTERNAL_URL`): backend → Keycloak over the Docker network (token, logout, admin API, JWKS).
- `keycloak.external-url` (`KEYCLOAK_EXTERNAL_URL`): browser-facing URLs, and the JWT issuer (`iss` = `KC_HOSTNAME`). The resource server's `issuer-uri` must use it, otherwise every token is rejected.

## Code (`/features/keycloak`)

- `KeycloakConfig` - admin client
- `RealmInitializationService` - on startup creates/configures the realm, frontend client (public SPA), backend client (confidential), token lifespans, OIDC mappers
- `KeycloakAuthController` / `KeycloakAuthService` - OAuth2 flow and cookie handling (`KeycloakAuthExceptionHandler` maps outages to 503)
- `KeycloakSecretsCheck` - fails startup under the `prod` profile when placeholder credentials are configured
- `UserManagementService` - optional, for custom registration flows (default is Keycloak-managed registration)

Read Keycloak settings through the typed `KeycloakProperties`, not `@Value`.

## Security

HTTP-only + `SameSite=Strict` cookies (`Secure` via `AUTH_COOKIE_SECURE`), CORS limited to `keycloak.frontend.web-origins`, Keycloak brute-force protection and session limits. Identity providers (Google, Facebook, GitHub, Azure AD, SAML) are configured in Keycloak, not in code.

See `back/KEYCLOAK_SETUP.md` for setup, deployment modes and troubleshooting.
