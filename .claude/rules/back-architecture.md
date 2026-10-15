---
paths:
  - "back/**"
---

# Backend architecture

Feature-based: each feature under `/features/<name>` is self-contained, with minimal coupling to others.

```
/feature-name
  /controllers     - REST endpoints
  /services        - business logic
  /models          - entities, business objects, DTOs
  /mappers         - DTO <-> entity transformations
  /repositories    - database access (Exposed)
  /connectors      - external services (optional)
```

## Adding a feature

1. Create `/features/<name>` with the sub-packages above.
2. Add a Flyway migration if the schema changes.

## Database

- PostgreSQL + Flyway; migrations are versioned (`V1__initial_schema.sql`, `V2__...`) and applied on startup.
- Exposed (DSL or DAO) for type-safe queries; one repository layer per feature.

## Configuration

- Everything sensitive or environment-specific comes from env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `KEYCLOAK_*`, `SSO_*`).
- See `.env.example` for the full list.
