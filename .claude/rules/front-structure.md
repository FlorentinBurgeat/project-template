---
paths:
  - "front/**"
---

# Frontend structure & routing

## App directory (`front/app`)

- `assets/css/main.css` - global styles
- `components/` - auto-imported components under their bare name (`pathPrefix: false`): `dummies/` (no business knowledge, split into `_common/`, `layout/` and `<domain>/`) and `business/<module>/`
- `composables/` - one subfolder per domain (`auth/`, `<domain>/`, `_common/` for generic ones). Only files named `use*.ts` are auto-imported (`imports.dirs: ['~/composables/**/use*.{ts,js,mjs,mts}']`). A composable holds reactive or Nuxt-aware logic only (state, fetch, router, i18n).
- `composables/<domain>/_domain/` - pure business rules of the module: root type, value-object types, a distinct type per lifecycle state (e.g. draft vs complete), pure functions. Framework-free (no Vue, Nuxt, i18n, no auto-import), relative imports only, never auto-imported, unit-tested in plain Node. Depends on nothing except the `Optional` type and the enum types and values of the back contract (`types/api.ts`), never its DTO shapes.
- `composables/<domain>/_api/` - backend adapter: DTO types (aliases of the generated `types/api.ts`, never hand-written) and mappers between DTOs and the domain, API error handling. Imports `_domain`, never the reverse.
- `composables/<domain>/_config/` - UI configuration (icons, grids, i18n key maps, library options). Plain data.
- Files in `_domain/`, `_api/`, `_config/` are imported explicitly, never auto-imported. `_domain/index.ts` is the single entry point of the domain: everything outside `_domain/` imports `~/composables/<domain>/_domain`, never a sub-file (`_domain/model`, `_domain/name`...). Only files inside `_domain/` import their siblings directly, to avoid cycles. Nuxt only scans `composables/*/index.ts`, so an `index.ts` one level deeper is not auto-imported.
- `types/` - shared kernel (`optional.ts`), types shared across modules (`auth.ts`: re-exports of the generated auth types, plus the hand-written `AuthUser`, the decoded JWT payload) and the API contract `api.ts`, generated from `back/openapi/template-api.json` by `pnpm api:generate` (ADR 0004): never edit it by hand, commit it; `pnpm api:check` runs in `pnpm lint`. Business types of a module live in its `_domain/`.
- `layouts/` - page layouts (`default.vue`)
- `middleware/` - route middleware (e.g. `auth.ts`)
- `pages/` - file-based routing
- `plugins/`, `utils/` - Vue plugins, auto-imported generic utilities (no business rules)
- `app.config.ts` (reactive app config), `app.vue` (root)

## Dependency direction

- `pages` and `components` -> `use*` composables -> `_api` -> `_domain`. `_config` may depend on `_domain`. The domain imports nothing else, except `Optional` and the contract enums.
- Pages and components (driving adapters) may import `_domain` directly for constants, types and pure functions (e.g. a max length, the list of options of an enum). They never import `_api`: that goes through a composable.

## Server directory (`front/server`)

- `api/` → routes prefixed with `/api` (`api/auth/login.ts` → `/api/auth/login`)
- `routes/` → routes without prefix
- `middleware/` → runs on every request
- `plugins/`, `utils/` - server-only

## Routing

- Routes come from `app/pages/`: `index.vue` → `/`, `[id].vue` → `/:id`, `[...slug].vue` → catch-all, `admin-[role].vue` → `/admin-:role`.
- Navigation, middleware, layouts: see `nuxt-dev-guidelines` skill (`references/routing-navigation.md`).
