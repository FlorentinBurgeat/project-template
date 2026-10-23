---
name: setup-environment
description: Initialize a new project forked from this template - rename the project everywhere (Keycloak realm and theme, database, Docker, Maven/Kotlin package, docs, branding), turn the README into the project's own, create back/.env with generated secrets, install all dependencies, start the stack and smoke-test it.
disable-model-invocation: true
argument-hint: "[app name]"
---

# Setup environment

Take a fresh fork of the template to **ready to code**: renamed, configured, dependencies installed, stack running, tests green. The argument, if given, is the app's display name.

Every place that carries the project's identity is listed in [`references/rename-map.md`](references/rename-map.md). Read it before Step 2: it is the change list you show the user and then apply.

Commands below use POSIX shell syntax; on Windows, run them through the Bash tool (Git Bash).

## Step 0 - Preflight

1. `git status --porcelain` is empty. Otherwise stop: the user commits or stashes first, so every change of this skill is reviewable as one diff.
2. Prerequisites (README "Prerequisites"): `docker compose version` (v2), `docker buildx version`, `docker info` (daemon running), `node -v` ≥ 20, `pnpm -v` ≥ 10, `java -version` ≥ 21, `mvn -v`. `java` on `PATH` and `JAVA_HOME` must both be JDK 21+ (`back/pom.xml` targets 21; an older `java` on `PATH` makes the surefire fork fail with "class file version 65.0"). Report every missing or too-old tool in one message, with how to install it, and stop.
3. Detect a re-run: if `back/keycloak/template-realm.json` no longer exists, the rename is done. Tell the user, skip to Step 4, and reuse the names already in place (read them from `back/.env.template` and `back/pom.xml`).
4. Detect an old stack **before renaming** (the compose project is still `template`, so compose can still find it):
   - `docker ps -a --format '{{.Names}}\t{{.Label "com.docker.compose.project"}}\t{{.Status}}'`: containers of project `template`, or of `back` (stacks from template versions without a compose `name:`).
   - Busy ports: `node -e "for(const p of [3000,5050,5432,8080,8081]){const s=require('net').createServer().once('error',()=>console.log('busy',p)).once('listening',()=>s.close()).listen(p)}"`.
   - Leftover volumes: `docker volume ls -q --filter name=template_ --filter name=back_`.

   If any of these turns up something, show it and ask before deleting (it destroys that stack's local data). With approval: `cd back && docker compose down -v --remove-orphans`, plus `docker compose -p back down -v --remove-orphans` for a legacy stack, then `docker volume rm` the leftover volumes the user confirms. A busy port held by something else (e.g. a local PostgreSQL service) is for the user to stop.

Done when: tree clean, every prerequisite present, the five ports free, and no old stack or volume left unless the user chose to keep it.

## Step 1 - Collect names and description

Ask with `AskUserQuestion` (recommended value first):

- **Display name** `<Name>`: from the argument, or ask.
- Derived from it, shown for confirmation and editable: `<slug>` (kebab-case), `<snake>`, `<pkg>`, Maven `groupId` `com.<pkg>`. The rules are in the rename map's placeholder table. Re-ask until `<slug>` matches `^[a-z][a-z0-9-]*$` and `<pkg>` is a valid, non-keyword Kotlin identifier.
- **pgAdmin email** (default `admin@example.com`).
- **App description**: what the app does, for whom, and its main features. Ask in plain text (free-form answer); a few sentences plus a bullet list of features is enough.

Done when: every placeholder has a validated value and the description is written down.

## Step 2 - Confirm the change list

Render the rename map with the real values: one table per section, `file | current → new`. Add the README rewrite (Step 3) and the files Step 4 creates (`back/.env`). Ask the user to approve, or to correct a value (then re-render).

Done when: the user explicitly approved this exact list. Edit nothing before it.

## Step 3 - Apply the renames

Apply every row of the rename map, section by section. `git mv` the realm file so history follows it.

Rewrite `README.md` into the project's own README:
- Title `# <Name>`, intro paragraph and a **Features** list written from the description.
- The template's "🚀 Features" bullets (auth, Docker, i18n...) leave the Features list: fold the ones that matter into the existing "Tech Stack Details" section, which stays the single place for the stack. Add no separate "Built on" section.
- Remove the template-only sections: "Start a New Project from This Template", "What This Template IS / IS NOT", "Roadmap", and the `/setup-environment` and rename-map links in "Documentation".
- Keep Prerequisites, Installation, Keep Up with the Template, Project Structure (root `<slug>/`), Usage, Security, SSO, Troubleshooting, License.
- Show the new title, intro and Features to the user and apply their edits before writing the rest.

Rewrite the `CLAUDE.md` "Project Overview" from the description in the same way (2-4 sentences, what the app is and for whom).

Done when: every rename-map row is applied, and the README and CLAUDE.md read as the project's own.

## Step 4 - Create `back/.env`

1. If `back/.env` exists, ask before overwriting it. Otherwise `cp back/.env.template back/.env`.
2. In `back/.env` only, set `PGADMIN_DEFAULT_EMAIL`, and replace `POSTGRES_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`, `PGADMIN_DEFAULT_PASSWORD` and `KEYCLOAK_BACKEND_CLIENT_SECRET` with fresh values, one per variable:
   `node -e "console.log(require('crypto').randomBytes(24).toString('hex'))"`
3. Check that `git check-ignore back/.env` prints the path: the secrets stay out of git.

The backend client secret reaches Keycloak through the `${KEYCLOAK_BACKEND_CLIENT_SECRET:...}` placeholder in the realm file (the compose file passes it to the Keycloak container), so the realm JSON is not edited. Tell the user the admin logins live in `back/.env`; keep the values themselves out of your messages.

Done when: `back/.env` exists, is git-ignored, and holds no template password or placeholder secret.

## Step 5 - Install dependencies

Run in this order (the theme JAR must exist before the Keycloak image builds):

1. `cd keycloak-theme && pnpm install && pnpm build-keycloak-theme > theme-build.log 2>&1`, then check `keycloak-theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar` exists and `src/kc.gen.tsx` names `<slug>-theme`. The build runs Maven and can fail once on a cold Maven cache: on failure, read the end of `theme-build.log` and retry once before diagnosing. Delete the log once the JAR exists.
2. `cd front && pnpm install` (runs `nuxt prepare`).
3. `cd back && ./mvnw -q dependency:go-offline` (local tests and IDE; the container builds by itself).
4. `cd .claude/hooks && pnpm install` (the hooks run on `tsx`).

Done when: all four exit 0 and the JAR exists. On failure, show the error and fix the cause before going on.

## Step 6 - Start the stack and smoke-test

1. The stack now runs as compose project `<slug>` (volumes `<slug>_pgdata`, ...). On a re-run, if `docker volume ls -q --filter name=<slug>_` lists volumes and the realm name or secret changed since they were created, warn the user: Keycloak imports a realm only into a fresh volume, so `docker compose down -v` (deleting local data) is needed. Ask first.
2. `cd back && docker compose up --build -d`. The first build takes minutes; poll `curl -sf http://localhost:8080/actuator/health` every ~10 s (up to ~5 min) until it returns `UP`.
3. `curl -s "http://localhost:8080/api/auth/login?redirect_uri=http://localhost:3000/en/auth/callback"` returns a URL containing `/realms/<slug>/`.
4. `cd back && ./mvnw test`, then `cd front && pnpm test --run && pnpm typecheck`.

On any failure: `docker compose logs <service> --tail 100`, diagnose, fix, re-run the failed check.

Done when: health `UP`, the login URL points at `/realms/<slug>/`, and both test suites pass.

## Step 7 - Sweep and hand off

1. Leftover sweep, which must print nothing:
   ```bash
   git grep -n -E "project-template|Project Template|template-realm|com\.template|template_db|template_api|template-api|Template Realm|realms/template|REALM_NAME[=:-]+template|\"realm\": \"template\"|Realm: +template|\`template\`|^name: template$|<(artifactId|name)>template<|AppName|NomApp" \
     -- ':!LICENSE' ':!.claude/skills/setup-environment' ':!claude-code-infrastructure-showcase' ':!*lock*' ':!*.pyc' ':!back/src/test' ':!docs/adr' \
     | grep -v "FlorentinBurgeat/project-template.git"
   ```
   Fix each hit or justify it to the user.
2. If `git remote get-url upstream` fails, offer to add the template as `upstream` (`git remote add upstream https://github.com/FlorentinBurgeat/project-template.git`). Leave `origin` as it is.
3. Summarize: URLs (front 3000 after `cd front && pnpm dev`, API 8080, Keycloak admin 8081/admin, pgAdmin 5050), credentials in `back/.env`, and the optional branding follow-up `front/LANDING_SETUP.md` (tagline, colors, logo).
4. Suggest the first commit, `chore: initialize <slug> from template`, and let the user commit.
5. Ask whether to delete `.claude/skills/setup-environment/` from the fork. Keeping it lets someone re-run Steps 4-6 on a fresh machine.
