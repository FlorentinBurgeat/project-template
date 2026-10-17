---
paths:
  - "front/**"
---

# Frontend best practices

## Performance

- DO keep SSR on for SEO-critical pages; DON'T use `server: false` there.
- DO use `{ lazy: true }` for non-critical data.
- DO use `<NuxtLink prefetch>` for likely navigations.

## TypeScript

- Type composables, e.g. `useState<User | null>("user", () => null)`.
- Type API responses, e.g. `useFetch<User[]>("/api/users")`.
