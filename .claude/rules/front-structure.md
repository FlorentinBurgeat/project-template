---
paths:
  - "front/**"
---

# Frontend structure & routing

## App directory (`front/app`)

- `assets/css/main.css` - global styles
- `components/` - auto-imported components (`ui/Card.vue` → `<UiCard />`)
- `composables/` - auto-imported composables (e.g. `useAuth.ts`)
- `layouts/` - page layouts (`default.vue`)
- `middleware/` - route middleware (e.g. `auth.ts`)
- `pages/` - file-based routing
- `plugins/`, `utils/` - Vue plugins, auto-imported utilities
- `app.config.ts` (reactive app config), `app.vue` (root)

## Server directory (`front/server`)

- `api/` → routes prefixed with `/api` (`api/auth/login.ts` → `/api/auth/login`)
- `routes/` → routes without prefix
- `middleware/` → runs on every request
- `plugins/`, `utils/` - server-only

## Routing

- Routes come from `app/pages/`: `index.vue` → `/`, `[id].vue` → `/:id`, `[...slug].vue` → catch-all, `admin-[role].vue` → `/admin-:role`.
- Navigation, middleware, layouts: see `nuxt-dev-guidelines` skill (`references/routing-navigation.md`).
