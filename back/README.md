# Backend

Kotlin / Spring Boot API (Java 21, Spring Boot 3.5, Maven) with Exposed ORM, PostgreSQL, Flyway and Keycloak authentication. This directory also holds the Docker Compose stack for local development.

## Prerequisites

- Docker with Compose v2
- The Keycloak theme JAR built (see [`keycloak-theme/README.md`](../keycloak-theme/README.md))

## Setup

```bash
cp .env.template .env      # defaults work for local development
docker compose up --build  # add -d to run in the background
```

Services started by `docker-compose.yml`:

| Service | Default URL |
|---------|-------------|
| Backend API | http://localhost:8080 |
| Keycloak (admin / admin) | http://localhost:8081 |
| pgAdmin | http://localhost:5050 |
| PostgreSQL | localhost:5432 |

On startup the stack creates the databases (`init-databases.sql`), imports the realm from `keycloak/template-realm.json`, applies the Flyway migrations and starts the API. The backend container restarts on source changes (`dev-watch*.sh`). Reset everything, data included, with `docker compose down -v`.

Quick check: `curl http://localhost:8080/api/auth/login` returns the Keycloak login URL.

### Upgrading an existing local stack

Migration `V3` and the realm import (realm renamed `template-realm` → `template`, exact callback redirect URIs) changed on the `feat/register-and-login` branch. A database volume created by an earlier version can fail Flyway validation (checksum mismatch), and Keycloak keeps the realm it imported first. Recreate the volumes once, and update `KEYCLOAK_REALM_NAME` in an existing `.env`:

```bash
docker compose down -v && docker compose up --build
```

## Configuration

All settings come from environment variables, documented in `.env.template`: container names, PostgreSQL credentials, ports, and `KEYCLOAK_*` (realm name, client IDs and secret, internal and external URLs). Change the admin credentials and `KEYCLOAK_BACKEND_CLIENT_SECRET` outside local development, and set `AUTH_COOKIE_SECURE=true` when served over HTTPS. `SPRING_PROFILES_ACTIVE=dev` enables debug logging (`application-dev.yml`).

`keycloak.frontend.redirect-uris` in `application.yml` lists the exact frontend callback URLs (one per locale). The backend rejects any other `redirect_uri`, and the same list is registered on the Keycloak clients (keep `keycloak/template-realm.json` in sync).

The frontend also sends its current `locale`; if it is listed in `keycloak.realm.supported-locales`, the backend adds `ui_locales` to the Keycloak URL so the login/register pages use that language (otherwise Keycloak's default applies). The realm's internationalization settings are applied only when the realm is first created: on an existing realm, enable Localization → Internationalization in the admin console or recreate the Keycloak volume.

## Tests and build

```bash
./mvnw test
./mvnw clean package
```

## Structure

- `src/main/kotlin/config/` - security and database configuration
- `src/main/kotlin/exception/` - global error handling
- `src/main/kotlin/features/keycloak/` - authentication feature (config, controller, dto, service)
- `src/main/resources/db/migration/` - Flyway migrations
- `keycloak/` - realm import and Keycloak image

## Documentation

- [`CLAUDE.md`](./CLAUDE.md) - architecture and conventions
- [`KEYCLOAK_SETUP.md`](./KEYCLOAK_SETUP.md) - Keycloak setup, deployment modes, troubleshooting
