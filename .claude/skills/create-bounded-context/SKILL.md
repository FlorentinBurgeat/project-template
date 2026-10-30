---
name: create-bounded-context
description: Checklist to add a new bounded context (hexagonal Spring Modulith module) to the back-end, including its isolated PostgreSQL schema, role, Flyway, DataSource and Exposed Database when it persists data. Use when asked to create a bounded context, a new module or a new domain in back/.
argument-hint: "<bounded context name>"
---

# Create a bounded context

Rules of record: `.claude/rules/back-architecture.md`, `.claude/rules/back-bounded-contexts.md`, ADR 0003, ADR 0005, ADR 0006. Read them first; this skill is the order of work, not a second copy of the rules.

If no bounded context persists data yet, the per-BC database infrastructure does not exist. The first BC that persists data builds it (step 3); the next ones copy it.

## 1. Frame it with the user

Report the plan before writing anything. Settle:

- The BC name (singular business noun, lowercase, one word: it is the package, the schema and the role).
- Whether it persists data. If not, skip step 3.
- The notions it needs from an existing BC. For each one, go through the order in `back-bounded-contexts.md` (duplicate and specialize, then ACL inside the BC, then kernel, then external ACL) and say which applies. If a kernel or an external ACL looks needed, stop and hand over to the `model-mitosis` agent.

## 2. Skeleton

1. `<root package>.<name>` (the package holding the existing modules) with `domain/<aggregate>/{model,logic,port}`, `application/<Name>Service.kt`, `adapters/{web,persistence}`. Anemic aggregates, value classes, `Parsed<T>`, ports in `domain/`, no Spring in `domain/`.
2. The base package is the public API: expose only what other modules need there. Everything else stays in sub-packages.
3. A reference to a concept owned by another BC is a value class owned by this BC, never a raw `String`, `UUID` or `Long`. The user is `shared/UserId`.
4. Talk to other BCs through their public API or application events (`ApplicationEventPublisher`, `@ApplicationModuleListener`). Never import their sub-packages.

## 3. Persistence isolation (only if the BC persists data)

The BC gets its own schema, role, migrations and connection (ADR 0005). Nothing is shared with another BC.

1. **Schema and role**: append to `back/init-databases.sql` a role `<name>` and a schema `<name>` owned by it, with no privilege on any other schema. That script only runs on a fresh PostgreSQL volume: on an existing one, run the statements by hand and tell the user.
2. **Secret**: one password per BC role, from an env var (`<NAME>_DB_PASSWORD`), added to the documented env file, the compose files and any setup script that generates secrets.
3. **Flyway**: migrations in `src/main/resources/db/migration/<name>/`, history table in the `<name>` schema, run with the BC's role. No FK, `JOIN`, view or trigger toward another schema.
4. **Connection**: in `adapters/persistence`, one `DataSource`, one Flyway and one Exposed `Database` for the BC, internal to the module, not `@Primary`. The existing `shared/DatabaseConfig.kt` single `Database` bean and the `spring.datasource` / `spring.flyway` settings in `application.yml` must be reworked by the first persisting BC so that no BC depends on a default connection. Show the user that change before making it.
5. **Repository**: `Exposed<Name>Repository : <Name>Repository` uses the BC's own `Database` only.
6. **Tests**: `shared/PostgresTestConfig` must replay the init script (role and schema) so tests run with the BC's role, not a superuser.

## 4. Exposing an SPI (only for strong coupling, with an external ACL)

Declare the SPI in `domain/<aggregate>/port` or a dedicated sub-package, then expose that package as a named interface: a type annotated `@PackageInfo` and `@NamedInterface("spi")` in it (Kotlin has no `package-info.java`). The consuming module declares `@ApplicationModule(allowedDependencies = "<name> :: spi")` the same way. Nothing else is exposed this way.

## 5. Tests and checks

1. Pure unit tests for `logic/`.
2. One API test through `adapters/web` on Testcontainers.
3. `ModularityTest` and `HexagonalArchitectureTest`. A failure is a design signal: fix the dependency, never relax the test or the rule.
4. Refresh the API contract (the published OpenAPI file, ADR 0004) and the front types if endpoints changed.

## 6. Docs

- New business terms in `GLOSSARY.md`.
- An ADR for any decision that departs from the rules or that a reader could not infer.
- Update the layout in `.claude/rules/back-architecture.md` only if the structure itself changed.

Do not commit unless asked.
