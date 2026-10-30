# ADR 0004: The back publishes the API contract, the front generates its types

- **Status**: Accepted
- **Date**: 2026-10-30

## Context

The front repeated the API by hand: DTO types, enum values and error codes. A change on one side was only caught at runtime. Since ADR 0003, web DTOs hold enums as `String`, so nothing in the Kotlin types tells the front which values are allowed.

## Decision

- **The back is the source of truth.** springdoc-openapi (no UI) describes `/api/**` from the controllers and DTOs. Enum values come from the domain enums (`@field:Schema(implementation = <DomainEnum>::class)`, `ModelResolver.enumsAsRef = true`), and a bounded context's `ErrorCode` schema from `ErrorCode.entries` plus web-only codes, never from hand-written lists. Response fields that are never null are `required`, request fields are not.
- **The contract is a committed file**: `back/openapi/template-api.json`. `OpenApiContractTest` fails when the API and the file differ. Refresh it with `./mvnw test -Dtest=OpenApiContractTest -Dcontract.update=true`. `/v3/api-docs` is only enabled in tests, never exposed in prod.
- **The front generates its types** from that file with openapi-typescript (`pnpm api:generate`, into `app/types/api.ts`, enum values exported as arrays). `pnpm api:check` runs in `pnpm lint` and fails on a stale file. A module's `_api` aliases the generated DTOs; its `_domain` takes only the contract enums from it.

**Option rejected**: contract-first (a hand-written `openapi.yaml` generating Kotlin and TypeScript). Heavier, and it would replace DTOs the web adapter needs to control for parse-don't-validate.

## Consequences

- Any change to a DTO, an enum or an error code shows up as a diff of `template-api.json` in review, then as a front typecheck error until the front follows.
- One more step when the API changes: refresh the contract on the back, then `pnpm api:generate` on the front.
- Every `/api/**` endpoint is published, auth included. Protected operations declare the `bearerAuth` scheme with `@SecurityRequirement(name = BEARER_AUTH)` at controller level: it is explicit, so every new protected controller must carry it. The 503 body of the auth endpoints has its own `AuthErrorResponse` schema. Only `AuthUser` stays hand-typed on the front: it is the decoded Keycloak JWT, not an API response. HTTP-only cookies appear in operation descriptions only.
