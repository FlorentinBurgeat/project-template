# Rename map

Every place that carries the project's identity, with the template value and what it becomes. Placeholders:

| Placeholder | Example ("Acme Shop") | Rule |
|---|---|---|
| `<Name>` | `Acme Shop` | display name, as typed |
| `<slug>` | `acme-shop` | kebab-case, `^[a-z][a-z0-9-]*$` |
| `<snake>` | `acme_shop` | `<slug>` with `-` → `_` |
| `<pkg>` | `acmeshop` | `<slug>` without `-`; not a Kotlin keyword |

Line numbers are hints: match on the value. Run the leftover sweep in `SKILL.md` Step 7 afterwards: it is the source of truth for "nothing missed".

## Keycloak realm

| File | Template value | New value |
|---|---|---|
| `back/keycloak/template-realm.json` | the file name | `git mv` → `back/keycloak/<slug>-realm.json` |
| `back/keycloak/<slug>-realm.json` | `"realm": "template"` | `"realm": "<slug>"` |
| same | `"displayName": "Template Realm"` | `"displayName": "<Name>"` |
| same | `"loginTheme": "project-template-theme"` | `"loginTheme": "<slug>-theme"` |
| `back/docker-compose.yml` | volume `./keycloak/template-realm.json:/opt/keycloak/data/import/template-realm.json:ro` | both sides `<slug>-realm.json` |
| same | comment `changes to template-realm.json` | `<slug>-realm.json` |
| same | `KEYCLOAK_REALM_NAME: ${KEYCLOAK_REALM_NAME:-template}` | `:-<slug>` |
| `back/.env.template` | `KEYCLOAK_REALM_NAME=template` | `<slug>` |
| `back/src/main/resources/application.yml` | `${KEYCLOAK_REALM_NAME:template}` (3 times: `issuer-uri`, `jwk-set-uri`, `keycloak.realm.name`) | `:<slug>` |
| same | comment `template-realm.json` | `<slug>-realm.json` |

The realm's backend client `"secret"` stays `${KEYCLOAK_BACKEND_CLIENT_SECRET:...}`: Keycloak substitutes it from the container env at import.

## Keycloak login theme

| File | Template value | New value |
|---|---|---|
| `keycloak-theme/vite.config.ts` | `themeName: "project-template-theme"` | `"<slug>-theme"` |
| `keycloak-theme/src/kc.gen.tsx` | `"project-template-theme"` (2 times) | `"<slug>-theme"` (the theme build regenerates it too) |
| `back/src/main/kotlin/features/keycloak/service/RealmInitializationService.kt` | `loginTheme = "project-template-theme"` | `"<slug>-theme"` |
| `keycloak-theme/README.md` | `` `project-template-theme` `` (2 times), realm path `back/keycloak/template-realm.json` | `` `<slug>-theme` ``, `back/keycloak/<slug>-realm.json` |
| `keycloak-theme/src/login/css/main.css` | header `Project Template - Keycloak Theme` | `<Name> - Keycloak Theme` |
| `keycloak-theme/index.html` | `<title>Keycloak Theme Dev</title>` | `<Name> - Keycloak Theme Dev` |

## Backend identity (Maven, Kotlin, Spring)

| File | Template value | New value |
|---|---|---|
| `back/pom.xml` | `<groupId>com.template</groupId>` | `com.<pkg>` |
| same | `<artifactId>template</artifactId>`, `<name>template</name>` | `<slug>` |
| same | `<description>Backend API with Keycloak authentication</description>` | `<Name> backend API` |
| same | `<start-class>com.template.ApplicationKt</start-class>` | `com.<pkg>.ApplicationKt` |
| `back/src/main/kotlin/Application.kt` | `package com.template`, `"com.template"` in `scanBasePackages` and `basePackages` | `com.<pkg>` |
| `back/src/main/resources/application.yml` | `spring.application.name: template-api` | `<slug>-api` |
| `back/src/test/resources/application-test.yml` | `name: template-api-test` | `<slug>-api-test` |

Unchanged: test fixtures using `template` as a sample realm (`JwtIssuerConfigTest`, `KeycloakAuthServiceTest`) set their own values.

## Database and Docker

| File | Template value | New value |
|---|---|---|
| `back/.env.template` | `POSTGRES_DB=template_db` | `<snake>_db` |
| same | `DOCKER_DATABASE_CONTAINER_NAME=template_db` | `<snake>_db` |
| same | `DOCKER_BACKEND_CONTAINER_NAME=template_api` | `<snake>_api` |
| `back/src/main/resources/application.yml` | `${POSTGRES_DB:template_db}` | `:<snake>_db` |
| `back/docker-compose.yml`, `back/docker-compose.prod.yml` | compose project `name: template` (prefixes volumes and networks) | `name: <slug>`; also the `template_pgdata` example in the dev file's comment |
| `back/KEYCLOAK_SETUP.md` | `App DB: template_db`, `Realm: template` (ASCII boxes), `docker exec -it template_api`, `realms/template` | `<snake>_db`, `<slug>`, `<snake>_api`, `realms/<slug>`; re-pad each box line to its border's width |
| same | env var table: `KEYCLOAK_REALM_NAME` default `` `template` `` | `` `<slug>` `` |

## Frontend branding

| File | Template value | New value |
|---|---|---|
| `front/i18n/locales/en.json` | every `AppName` (`landing.appName`, FAQ, final CTA, copyright, SEO and dashboard titles) | `<Name>` |
| `front/i18n/locales/fr.json` | every `NomApp` | `<Name>` |

Tagline, description, colors, logo and images: `front/LANDING_SETUP.md` (optional follow-up).

## Docs and agent config

| File | What changes |
|---|---|
| `README.md` | rewritten as the project's README (see `SKILL.md` Step 3); realm name (`KEYCLOAK_REALM_NAME=template`, "import the `template` realm") and `<slug>-realm.json` paths |
| `CLAUDE.md` | title `# CLAUDE.md - <Name>`, overview from the app description, drop the "New project forked from this template?" line and the `/setup-environment` mention, `<slug>-realm.json` paths |
| `back/README.md` | `keycloak/template-realm.json` (2 times) |
| `.claude/agents/backend-developer.md` | `back/keycloak/template-realm.json` |
| `docs/adr/0001-authentication-flow-and-hardening.md` | file paths `template-realm.json` only; the dated statements (e.g. `KEYCLOAK_REALM_NAME=template` for old stacks) are history and stay |

Kept on purpose: the `upstream` URL `FlorentinBurgeat/project-template.git` in `README.md`, and `LICENSE`.
