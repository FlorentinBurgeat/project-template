# ADR 0001: Authentication flow and hardening of the Keycloak integration

- **Status**: Accepted
- **Date**: 2026-10-17
- **Scope**: `back/` (Spring Boot API), `front/` (Nuxt app), `keycloak-theme/`, Docker Compose stack

## Context

The template delegates identity to Keycloak. The backend runs the OpenID Connect authorization code flow on behalf of the frontend, so the browser never calls Keycloak's token endpoint. A review of the `feat/register-and-login` branch found problems in correctness, security and operations:

- Every Bearer token would have been rejected. Keycloak issues tokens with its browser-facing hostname as `iss`, but the backend validated them against its internal Docker URL.
- The flow had no `state` or PKCE, so login CSRF and code injection were possible.
- The backend accepted any `redirect_uri`, and Keycloak clients allowed wildcard URIs.
- The refresh-token cookie had no `SameSite` attribute and `Secure=false`. Spring CSRF protection was disabled.
- Every failure returned 401. A Keycloak outage looked like bad credentials and signed users out.
- The auth middleware tried to refresh during SSR, where the refresh cookie is never available.
- The `/api` proxy target was fixed at build time.
- The type checker and linter could not run with the installed dependency versions.

This ADR records the decisions taken to fix these problems, and the alternatives considered.

## Decisions

### 1. Backend-owned `state` and PKCE, stored in a short-lived cookie

`GET /api/auth/login` and `GET /api/auth/register` generate a random `state` and a PKCE `code_verifier`, and send its S256 challenge in the Keycloak URL. Both values go in an `auth_flow` cookie (HTTP-only, `SameSite=Strict`, `Path=/api/auth`, 10 minutes). `POST /api/auth/callback` takes `{ code, state, redirect_uri }` as a JSON body. It compares `state` with the cookie in constant time, exchanges the code with the verifier, then clears the cookie.

- **Why**: the backend owns the flow end to end. JavaScript never sees the verifier, and the frontend needs no crypto or `sessionStorage` handling.
- **Alternative rejected**: frontend-generated `state` and verifier in `sessionStorage`. This needs more client code, and the verifier would be readable by JavaScript.

### 2. Exact `redirect_uri` allow-list shared by the backend and Keycloak

`keycloak.frontend.redirect-uris` lists exact callback URLs, one per locale (`http://localhost:3000/{en,fr}/auth/callback`). The backend rejects any other value with 400 and does not log the rejected value. The same list is registered on the Keycloak clients (`RealmInitializationService` and `template-realm.json`) in place of the `/*` wildcards.

- **Why**: this is defense in depth. A fork promoted to production keeps strict redirect URIs instead of open wildcards.

### 3. Token storage and CSRF strategy

- Access token: kept in memory only (`useState`), never persisted.
- Refresh token: HTTP-only cookie, `SameSite=Strict`, `Path=/api/auth`. Its lifetime follows Keycloak's `refresh_expires_in`. `Secure` is set by `AUTH_COOKIE_SECURE`, which must be `true` over HTTPS.
- Cookies are built with Spring's `ResponseCookie`, because the Servlet `Cookie` API cannot set `SameSite`.
- Spring's token-based CSRF protection stays disabled. The cookie-authenticated endpoints are protected because the cookies are `SameSite=Strict`, and because the CORS filter rejects requests whose `Origin` is outside `keycloak.frontend.web-origins`.
- **Alternative rejected**: synchronizer CSRF tokens. They add state and frontend plumbing without meaningful extra protection for this cookie scope.

### 4. Separate issuer and JWKS URLs

`spring.security.oauth2.resourceserver.jwt.issuer-uri` uses `KEYCLOAK_EXTERNAL_URL`, which matches the `iss` claim (`KC_HOSTNAME`). `jwk-set-uri` uses `KEYCLOAK_INTERNAL_URL` (the Docker network). In `KeycloakProperties`:

- `url` (internal) is used for the token, logout, admin and JWKS calls.
- `externalUrl` is used for URLs the browser visits.

A config test (`JwtIssuerConfigTest`) keeps this split from regressing.

### 5. Error semantics: 401 means rejected, 503 means unavailable

`KeycloakAuthService` turns a Keycloak 4xx into `InvalidTokenException` (401), and a 5xx or I/O error into `KeycloakUnavailableException` (503, via `KeycloakAuthExceptionHandler`). Consequences:

- `/refresh` clears the cookie only when Keycloak rejects the token.
- `useAuth().refresh()` signs the user out only on a 401. On an outage or a network error it keeps the session and retries every 30 s.
- `/logout` clears the cookie even when revocation fails.

The 503 mapping lives in an advice scoped to the auth controller. The global `GlobalExceptionHandler` is not component-scanned today, and its catch-all handler would turn standard Spring MVC errors into 500s.

### 6. Same-origin runtime proxy for `/api`

The Nitro server route `front/server/api/[...path].ts` proxies `/api/**` to the backend. The target is read from runtime config (`NUXT_API_BASE_URL`), replacing a `routeRules` proxy whose target was fixed at build time.

- **Why**: the browser stays same-origin, so the cookies work without cross-site rules. The backend URL can change per environment without a rebuild.

### 7. Session restoration is client-only

The `auth` route middleware does nothing during SSR (`import.meta.server`). The refresh cookie is scoped to `/api/auth` and only exists in the browser. After hydration, the client plugin (`auth.client.ts`) and the middleware restore the session. The callback page:

- uses only the first value of a repeated query parameter,
- handles Keycloak's `error` parameter,
- never resubmits an authorization code it already used,
- redirects to locale-aware paths.

### 8. Configuration conventions

- Keycloak settings are read through the typed `KeycloakProperties`, not `@Value`.
- Debug logging (Spring Security, Exposed) moved to `application-dev.yml`. Compose sets `SPRING_PROFILES_ACTIVE` (default `dev`).
- `KeycloakSecretsCheck` fails startup under the `prod` profile when the placeholder client secret or admin password is configured. In other profiles it logs a warning.
- The leftover `/api/auth/health` debug endpoint is removed; use `/actuator/health`.

### 9. Database migrations are immutable once applied

Never edit a migration that has run on any database. Add a new version instead. Editing `V4` broke the startup of an existing dev database with a Flyway checksum mismatch, so the edit was reverted.

The only exception is `V3`. It was rewritten as a no-op because it referenced tables that no migration on `main` creates, so it could never have applied to a fresh database. Upgrade steps for existing local volumes are documented in `back/README.md`.

### 10. Keycloak container

- Local development uses `start-dev --import-realm`. The image's default `CMD` (`start --optimized`) is for production-style runs and needs `KC_HOSTNAME` and TLS.
- `--import-realm` already skips realms that exist, so the undocumented `KC_SPI_IMPORT_DIR_STRATEGY` setting was removed. Changes to `template-realm.json` only apply to a fresh volume.
- The Compose health check calls `/health/ready` on the management port (9000), so the backend waits until the realm import has finished.

### 11. Frontend toolchain versions

- `typescript` is pinned to `^5.9.3`. TypeScript 7 has no JavaScript compiler API, so `vue-tsc` and `typescript-eslint` (peer range `<6.1`) cannot use it.
- `eslint` is pinned to `^9.39.5`. The resolved `@typescript-eslint/utils` crashes on ESLint 10.
- Prettier uses `endOfLine: "auto"`, because Windows checkouts with `core.autocrlf=true` produce CRLF files.
- `happy-dom` was added so the Nuxt Vitest project configured in `vitest.config.ts` can run.

Revisit these pins when `vue-tsc` and `typescript-eslint` support the newer majors.

### 12. Keycloak theme

Server-provided messages rendered through `dangerouslySetInnerHTML` go through Keycloakify's `kcSanitize`. Hard-coded `tabIndex` values were removed in favor of the natural DOM order, for accessibility.

## Consequences

**Positive**

- Protected endpoints accept valid Keycloak tokens, and the issuer configuration is covered by a test.
- Login CSRF, code injection, open redirects and cross-site use of the cookies are mitigated.
- Keycloak outages surface as 503, and users stay signed in through transient failures.
- The deployment target of the `/api` proxy and the cookie `Secure` flag are runtime configuration.
- The auth controller, service, configuration and composable are covered by tests (26 backend, 5 frontend), and the type checker and linter run again.

**Negative / trade-offs**

- Starting a login or registration in two tabs at once overwrites the `auth_flow` cookie, so the first tab's callback fails and must be retried.
- Adding a locale or a frontend origin requires updating `keycloak.frontend.redirect-uris` and `web-origins`, plus `template-realm.json` for fresh imports.
- Local stacks created before these changes need `docker compose down -v` once, and `KEYCLOAK_REALM_NAME=template` in `.env`.
- `/dashboard` renders on the server without user data. The user appears after hydration.

**Follow-ups (not decided here)**

- An API client wrapper that attaches the Bearer token to protected calls; no protected endpoint exists yet.
- Enabling and fixing `GlobalExceptionHandler`, which is not component-scanned.
- Pre-existing type errors in `keycloak-theme` (`KcPage.tsx`, the social-provider class name in `Login.tsx`), and the theme's older TypeScript, Vite and React versions.
- A `.gitattributes` file (`eol=lf`) as the root fix for line endings.
- The alpha Nuxt modules (`@nuxt/a11y`, `@nuxt/hints`).
