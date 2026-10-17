---
paths:
  - "front/**"
---

# Frontend conventions

## Components

- `components/`: reusable, no business logic, props-driven, emit events to parent.
- `pages/`: business logic, data fetching, state, page-specific behavior.
- Use Nuxt UI components (`UButton`, `UCard`, ...) before writing custom ones.

## Data fetching

- `useFetch('/api/x')` for API calls, `useAsyncData('key', fn)` for custom async logic.
- Both return `{ data, pending, error, refresh }`.
- Details: `nuxt-dev-guidelines` skill (`references/data-fetching.md`).

## State

- Global state via `useState('key', () => initial)`; no Pinia/Vuex.
- Use `callOnce(async () => {})` for one-time init across SSR and client.

## SEO & head

- `useSeoMeta()` for title, description, OG, Twitter Card; unique per page.
- `useHead()` for scripts, links, htmlAttrs.
- `definePageMeta()` for layout and middleware.
- Use semantic HTML. Details: `references/seo-meta.md`.

## Server API routes

- Use `defineEventHandler()`, `readBody()`, `getCookie()`, `createError()`.
- Details: `references/server-api.md`.

## Configuration

- Env-driven values go in `runtimeConfig` (private at top level, client-safe under `public`).
- Keep TypeScript `strict: true`.
