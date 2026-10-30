---
paths:
  - "front/**"
---

# Frontend best practices

## Performance

- DO keep SSR on for SEO-critical pages; DON'T use `server: false` there.
- DO use `{ lazy: true }` for non-critical data.
- DO use `<NuxtLink prefetch>` for likely navigations.
- AVOID `onMounted` and `await import(...)` in component code. When one is unavoidable (browser-only API, client-only library), justify it in a one-line comment.

## TypeScript

- Type composables, e.g. `useState<Optional<User>>("user", () => undefined)`.
- Type API responses, e.g. `useFetch<User[]>("/api/users")`.
- NEVER use `null`: use `undefined` only. Declare `type Optional<T> = undefined | T` once and use it for every value that may be absent.
- Convert `null` to `undefined` at the boundary where it enters the app (a JSON payload, a DOM API), and back only where an API contract requires it. The generated contract types allow `null` on request fields: accepted in `_api/` only.
- Enums, DTO shapes and error codes of the back come from the generated `types/api.ts` (ADR 0004), never retyped. A mapping over a contract enum is `Record<Enum, …>` or `satisfies Record<Enum, …>`, so a new back value breaks `typecheck`. After a back contract change, run `pnpm api:generate` and commit `types/api.ts`.
- No `let` in component code (`<script>` of `.vue` files): use `const`, `ref`, `computed`, or move the logic to a composable.
- NEVER use `provide` / `inject` in Vue (components or composables). Pass data through props and events, or share state through a composable (`useState` for shared state, a composable called where needed).
- NEVER use the non-null assertion `!`: it hides a typing mistake. Fix the type instead (discriminated union, type guard, `Optional`, a function that returns `Optional`).
- Run `pnpm typecheck` after each change; it must report no error.

## Domain modelling (DDD, functional style)

- An anemic model is intended: types hold data, pure functions hold the rules. One root type per bounded context, a type for each value object, and a distinct type per lifecycle state of an entity (e.g. a draft row with optional fields vs a complete row where they are required), so an invalid value cannot reach a calculation or a request.
- Pure business functions are small and single-purpose (no function that validates, totals and maps at once); a big function is split and composed.
- Hexagonal: the domain depends on nothing but the contract enums (`types/api.ts`); DTO types and mappers are an adapter (`_api/`); composables and components are driving adapters. Dependencies point inward.

## Structure

- A page assembles business components and holds as little script as possible; business logic lives in composables (`composables/`).
- `components/` has two subfolders: `dummies/` and `business/`.
- One business component per concern (e.g. one component per form section), in `components/business/<module>/` (one subfolder per module, e.g. `components/business/<domain>/`).
- A component with no business knowledge (props in, events out, pure rendering) is a dummy component: it goes in `components/dummies/`, never in `business/`.
- `dummies/` is split by domain: `dummies/<domain>/` (e.g. `landing/`), `dummies/layout/` for the app shell (logo, language switcher), `dummies/_common/` for generic ones (inputs, section cards, `ErrorAlert` / `TipAlert` wrappers of `UAlert`).
- A business component holds no Nuxt UI configuration and no classes (`size`, `variant`, `ui`, `class`, `placeholder`, `maxlength`, grid placement...). It composes domain dummies that expose only business props and events (`id`, `modelValue`, `error`, the line's name for aria labels...) and preconfigure everything else, the front counterpart of the back value objects (e.g. `<Domain>NameField`: `UFormField` + input). A rule shared by several fields (a max length) lives in one base dummy that reads the domain constant, never a hard-coded value. A select computes its own items from the domain options composable: the business component never passes `items`. Row and list layouts are dummies with named slots (`<Domain>RowLayout` with `#name`, `#amount`, `#actions`...) that own the grid and placement classes; a list that needs its DOM element exposes it with `defineExpose({ list })`. A section card is one `<Domain>SectionCard` keyed by a `section` prop, its icon, variant, title and i18n keys coming from a config map in the module's `_config/`.
- `composables/` has one subfolder per business domain (e.g. `composables/auth/`, `composables/<domain>/`). Generic non-business composables go in `composables/_common/` (underscore so it sorts first). Keep components and composables auto-imported under their bare name (no folder prefix in the component name).
- Extract large configuration objects (menus, column definitions, option lists, theme variants) into their own file instead of inlining them in a component.

## Markup

- Prefer semantic elements (`section`, `header`, `nav`, `ul`/`li`, `form`, `fieldset`, `dl`, `button`, `p`, `h1`-`h6`) over generic `div`; keep a `div` only when no semantic element fits.
