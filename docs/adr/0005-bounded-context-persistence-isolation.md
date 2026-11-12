# ADR 0005: One PostgreSQL schema and one role per bounded context

- **Status**: Accepted
- **Date**: 2026-10-30

## Context

ADR 0003 forbids a module from reading another module's tables, but only as a sentence in a rule file: with a single schema and a single database user, a `JOIN` across two bounded contexts (BC) works silently. The coupling surfaces much later, when one BC must move to another store (a NoSQL database for instance) or be extracted as a service, which is when it is most expensive to remove.

The goal is no coupling between BCs, technical included, even though all of them run in the same PostgreSQL instance.

## Decision

- **One PostgreSQL schema per BC**, named after the BC. A technical module that persists data follows the same rule. Today none does: `auth` keeps its state in Keycloak (its own `keycloak_db`) and `shared` holds no data, so the template ships no schema, role or table in PostgreSQL.
- **Profile data is a BC concern.** Keycloak keeps the basic identity (email, names, credentials); the rest belongs to a profile BC in a fork, which owns its schema and refers to the user by `shared/UserId` (the Keycloak `sub`). The former orphan `user_profiles` table was removed from the template (V4 is a no-op).
- **One database role per BC**, owner of its schema, with no privilege on any other schema. A cross-BC query fails at the database, not in a review. Removing the isolation means changing a role or a `GRANT`, which is a visible infrastructure change, unlike deleting a test.
- **One Flyway per BC**: migrations in `db/migration/<bc>/`, history table inside the BC's schema, run with the BC's role.
- **One `DataSource` and one Exposed `Database` per BC.** They are internal to the module (not part of its public API) and used by its `adapters/persistence` only. `ModularityTest` rejects any other module touching them.
- **No foreign key, `JOIN`, view or trigger across schemas.** A BC that needs a concept owned by another BC holds its own value class for the reference (never a raw `String` or `UUID`), and gets the data through the other BC's public API or an application event.
- **No transaction spans two BCs.** Cross-BC consistency is eventual, through application events.
- **Moving a BC to another store** (NoSQL, another service) is done behind its repository port: write the new adapter, backfill idempotently from the schema, dual-write while checking parity, switch reads, then drop the schema. Dropping the schema is a separate, explicitly authorized step.

## Consequences

- More infrastructure for each BC: an init script for the role and schema, a `DataSource`, a secret, a Flyway location. This is repeated for every BC and inherited by every fork, which is accepted to keep the isolation guaranteed by the database rather than by convention.
- Tests start PostgreSQL through Testcontainers and must replay the init script.
- The rule still depends on `ModularityTest` for the wiring: a role does not stop a module from being injected another module's `Database`, the module boundary does.
- Implementation (init script, per-BC configuration, `.env.template`, `PostgresTestConfig`) follows this ADR and is tracked separately.
