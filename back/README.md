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

## Configuration

All settings come from environment variables, documented in `.env.template`: container names, PostgreSQL credentials, ports, and `KEYCLOAK_*` (realm name, client IDs and secret, internal and external URLs). Change the admin credentials and `KEYCLOAK_BACKEND_CLIENT_SECRET` outside local development.

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
