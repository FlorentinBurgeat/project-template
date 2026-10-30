# CLAUDE.md - Backend

Kotlin / Spring Boot backend: modular monolith (Spring Modulith), one hexagonal module per bounded context (ADR 0003).

## Tech Stack

- **Language**: Kotlin, **Framework**: Spring Boot, **Build**: Maven
- **ORM**: Exposed, **DB**: PostgreSQL, **Migrations**: Flyway
- **Auth**: Keycloak (OpenID Connect), JWT validation via Spring Security
- **API contract**: springdoc-openapi, committed as `openapi/template-api.json` (ADR 0004)
- **Tests**: JUnit 5, MockK, Testcontainers (PostgreSQL), Spring Modulith + ArchUnit architecture tests

## Rules

Detailed conventions live in `/.claude/rules/` (scoped to `back/**`):
`back-architecture.md`, `back-auth-keycloak.md`.

## Skills

- **kotlin-spring-boot-backend** - controllers, services, repositories, DDD, Exposed
- **keycloak** - server setup, realms, admin client
- **route-tester** - HTTP route testing with JWT auth
