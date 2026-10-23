# CLAUDE.md

## Project Overview

**project-template** is a production-ready full-stack template to kickstart a web application: Keycloak authentication, modular architecture, and a working Docker-based dev setup. Clone it and focus on business features.

> New project forked from this template? Run `/setup-environment`: it renames the project, creates `back/.env`, installs every dependency and starts the stack.

> Architecture details: `back/CLAUDE.md`, `front/CLAUDE.md`, and the scoped rules in `.claude/rules/`.

---

## Repository Layout

- `back/` - Kotlin / Spring Boot API, `docker-compose.yml`, `.env.template`, Keycloak realm import (`keycloak/template-realm.json`)
- `front/` - Nuxt 4 app (Nuxt UI, Tailwind, `@nuxtjs/i18n`, landing page)
- `keycloak-theme/` - custom Keycloak login theme (Keycloakify); its JAR must be built before starting Docker
- `mobile/` - placeholder, see its README

---

## Tech Stack

- **Frontend**: Nuxt 4 (Vue 3), Nuxt UI + Tailwind CSS, `useState` for state, pnpm
- **Backend**: Kotlin, Spring Boot, Maven, Exposed ORM, PostgreSQL, Flyway
- **Auth**: Keycloak (OpenID Connect), custom theme via Keycloakify
- **DevOps**: Docker Compose (PostgreSQL, Keycloak, pgAdmin, backend), env vars for configuration

---

## Authentication Flow

The backend mediates the OpenID Connect authorization code flow; the frontend never talks to Keycloak's token endpoint directly.

1. Frontend calls `GET /api/auth/login` (or `/register`) with a `redirect_uri` (`/<locale>/auth/callback`, must be listed in `keycloak.frontend.redirect-uris`) and redirects the browser to the returned Keycloak URL. The backend adds `state` and a PKCE S256 challenge, keeping `state` and the verifier in a short-lived HTTP-only `auth_flow` cookie.
2. After login, Keycloak redirects to the callback page with an authorization `code` and `state`.
3. Frontend calls `POST /api/auth/callback` with `{ code, state, redirect_uri }`; the backend checks `state` against the cookie and exchanges the code (with the PKCE verifier) for tokens.
4. The access token is returned in the response body and kept **in memory** (`useAuth` state); the refresh token is set as an **HTTP-only cookie**.
5. Protected endpoints require `Authorization: Bearer <access token>`; the backend validates it as an OAuth2 Resource Server against the Keycloak realm.
6. `useAuth` schedules a refresh ~60s before expiry via `POST /api/auth/refresh` (cookie-based); `POST /api/auth/logout` revokes tokens and clears the cookie.

Social login, SSO and other identity providers (Google, Facebook, SAML, LDAP...) are configured in Keycloak, not in code. The realm is imported from `back/keycloak/template-realm.json`, and `RealmInitializationService` configures it on startup.

---

## Configuration & Local Setup

Full steps are in `README.md` (`/setup-environment` runs them for a new fork). In short:

1. `cp back/.env.template back/.env` (env vars: DB credentials, Keycloak admin, `KEYCLOAK_REALM_NAME`, client secrets, ports)
2. Build the Keycloak theme: `cd keycloak-theme && pnpm install && pnpm build-keycloak-theme`
3. Start the backend stack: `cd back && docker compose up --build`
4. Start the frontend: `cd front && pnpm install && pnpm dev`

Ports: frontend 3000, backend 8080, Keycloak 8081, pgAdmin 5050, PostgreSQL 5432.

---

## Development Principles

- **Extensibility**: new backend feature = new package under `features/`; new frontend page = new file in `app/pages/`; new SSO provider = Keycloak configuration.
- **Modularity**: features are isolated and can be removed or changed independently, with minimal cross-dependencies.
- **Maintainability**: predictable organization, clear separation of concerns, mappers to isolate data transformations.
- **Developer experience**: clone, configure `.env`, `docker compose up`, start building features.

---

## Base Features Included

- Registration and login delegated to Keycloak (email/password, social providers)
- Automatic token refresh and logout
- Protected API endpoints with token validation
- Frontend: landing page, dashboard page, auth callback page, language switcher (en/fr)

---

## Agent skills

### Issue tracker

Local markdown under `.scratch/<feature>/` (spec + one file per ticket). See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical roles, recorded as a `Status:` line in each ticket. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
