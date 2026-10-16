# Frontend

Nuxt 4 application (Vue 3, Nuxt UI, Tailwind CSS, `@nuxtjs/i18n`) with Keycloak authentication handled through the backend.

## Prerequisites

- Node.js 20+ and pnpm 10+
- The backend stack running (see the [root README](../README.md)). Requests to `/api/**` are proxied (`server/api/[...path].ts`) to `NUXT_API_BASE_URL` (default `http://localhost:8080`, read at runtime).

## Setup

```bash
pnpm install
pnpm dev        # http://localhost:3000
```

## Scripts

| Script | Purpose |
|--------|---------|
| `pnpm dev` | Dev server with hot reload |
| `pnpm build` / `pnpm preview` | Production build / local preview |
| `pnpm lint` | ESLint |
| `pnpm fmt` / `pnpm fmt:check` | Prettier write / check |
| `pnpm typecheck` | `nuxt typecheck` |
| `pnpm test:unit` / `pnpm test:nuxt` | Vitest (unit / Nuxt environment) |
| `pnpm test:e2e` | Playwright |

## Structure

- `app/pages/` - file-based routes (landing, dashboard, `auth/callback`)
- `app/composables/useAuth.ts` - login, registration, callback, token refresh, logout
- `app/middleware/auth.ts` - protects authenticated routes
- `i18n/` - translations (en, fr)

See [`CLAUDE.md`](./CLAUDE.md) for conventions.
