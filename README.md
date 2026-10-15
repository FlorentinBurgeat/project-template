# Project Template

A production-ready full-stack template to kickstart your next web application. Built with Nuxt 4, Kotlin Spring Boot, Keycloak, and PostgreSQL.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Features

- ✅ **Keycloak Authentication**: Centralized identity and access management with OpenID Connect
- ✅ **SSO Ready**: Multi-provider support (Email/Password, Google, Facebook, and extensible)
- ✅ **Account Management**: Registration, login, profile management (delegated to Keycloak)
- ✅ **Modern Frontend**: Nuxt 4 + Nuxt UI + Tailwind CSS + SSR, with i18n (en/fr)
- ✅ **Custom Login Theme**: Keycloakify-based Keycloak theme
- ✅ **Robust Backend**: Kotlin + Spring Boot + Exposed ORM
- ✅ **Feature-Based Architecture**: Modular and scalable structure
- ✅ **Docker Ready**: PostgreSQL, Keycloak, pgAdmin and the API start with `docker compose up`
- ✅ **Database Migrations**: Flyway for schema versioning
- ✅ **Type-Safe**: Kotlin on the backend, strict TypeScript on the frontend
- ✅ **AI-Enhanced Development**: MCP server integration for Nuxt UI documentation

---

## 📋 Prerequisites

Install the following before you begin:

| Tool | Version | Used for |
|------|---------|----------|
| **Docker** + **Docker Compose v2** (`docker compose`, not the legacy `docker-compose` 1.x) | 24+ | Runs PostgreSQL, Keycloak, pgAdmin and the backend |
| **Docker Buildx** | any | Required by the Keycloak image build |
| **Node.js** | 20+ | Frontend and Keycloak theme |
| **pnpm** | 10+ (`npm i -g pnpm`) | Package manager (frontend and theme) |
| **JDK** | 17+ | Building the Keycloak theme JAR (Keycloakify) |
| **Maven** | 3.8+ | Building the Keycloak theme JAR (the backend builds inside Docker) |
| **Git** | any | |
| **Claude Code CLI** | optional | MCP integration, see step 7 |

> **PostgreSQL**: no local installation is needed, it runs in Docker.
> If you have a PostgreSQL service installed on your machine, **stop it**
> (`sudo systemctl disable --now postgresql`) because the container publishes port `5432`.

**Linux only**: allow your user to talk to Docker without `sudo`, then log out and back in:

```bash
sudo usermod -aG docker $USER
```

Ports used (must be free): `5432` (PostgreSQL), `5050` (pgAdmin), `8080` (backend), `8081` (Keycloak), `3000` (frontend).

---

## 🛠️ Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/project-template.git
cd project-template
```

### 2. Configure environment variables

```bash
cd back
cp .env.template .env
```

The defaults work out of the box for local development. Review at least:

```env
POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB   # PostgreSQL credentials
PGADMIN_DEFAULT_EMAIL / PGADMIN_DEFAULT_PASSWORD  # pgAdmin login
KEYCLOAK_ADMIN_USERNAME / KEYCLOAK_ADMIN_PASSWORD # Keycloak admin console (admin/admin)
KEYCLOAK_REALM_NAME=template                      # must match "realm" in keycloak/template-realm.json
KEYCLOAK_BACKEND_CLIENT_SECRET                    # change it outside local dev
```

### 3. Build the Keycloak theme (first time only)

The Keycloak image embeds a custom login theme. Its JAR is a build output (git-ignored) and must exist before starting Docker:

```bash
cd keycloak-theme
pnpm install
pnpm build-keycloak-theme
cd ..
```

This generates `keycloak-theme/dist_keycloak/keycloak-theme-for-kc-all-other-versions.jar`.
Re-run it whenever you change the theme.

### 4. Start the backend stack (PostgreSQL, Keycloak, pgAdmin, API)

```bash
cd back
docker compose up --build
```

The first start takes a few minutes (image builds, Maven dependencies). It will:
- create the `keycloak_db` and application databases (`back/init-databases.sql`),
- import the `template` realm from `back/keycloak/template-realm.json`,
- apply the Flyway migrations and start the Spring Boot API.

Add `-d` to run in the background. To reset everything (including database data): `docker compose down -v`.

### 5. Start the frontend

In another terminal:

```bash
cd front
pnpm install
pnpm dev
```

### 6. Access the application

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Keycloak admin console | http://localhost:8081/admin (admin / admin) |
| pgAdmin | http://localhost:5050 |

Quick check that the backend and Keycloak are wired together:

```bash
curl http://localhost:8080/api/auth/login   # returns the Keycloak login URL
```

### 7. Setup MCP for Nuxt UI Documentation (Optional)

If you're using Claude Code CLI, add the Nuxt UI MCP server for instant access to component documentation:

```bash
claude mcp add --transport http nuxt-ui-remote https://ui.nuxt.com/mcp
```

This enables AI-assisted development with real-time access to Nuxt UI component docs, props, slots, and examples.

---

## 📁 Project Structure

```
project-template/
├── back/                  # Kotlin Spring Boot backend
│   ├── src/main/
│   │   ├── kotlin/
│   │   │   ├── config/        # Security, database
│   │   │   ├── exception/     # Global error handling
│   │   │   └── features/      # Feature-based modules
│   │   │       └── keycloak/  # config, controller, dto, service
│   │   └── resources/
│   │       └── db/migration/  # Flyway migrations
│   ├── keycloak/              # Realm import (template-realm.json)
│   ├── docker-compose.yml     # PostgreSQL, Keycloak, pgAdmin, backend
│   ├── .env.template          # Environment variables
│   ├── pom.xml
│   └── CLAUDE.md
│
├── front/                 # Nuxt 4 frontend
│   ├── app/
│   │   ├── pages/         # File-based routing
│   │   ├── components/    # Auto-imported components
│   │   ├── composables/   # Auto-imported composables (useAuth)
│   │   ├── middleware/    # Route middleware
│   │   ├── plugins/       # Vue plugins
│   │   ├── types/         # TypeScript types
│   │   └── app.vue        # Root component
│   ├── i18n/              # Translations (en, fr)
│   ├── public/            # Static assets
│   ├── nuxt.config.ts     # Nuxt configuration (proxies /api/** to the backend)
│   ├── package.json
│   └── CLAUDE.md
│
├── keycloak-theme/        # Keycloakify login theme (built into a JAR)
├── mobile/                # Placeholder for a mobile app
│
├── .claude/               # Claude Code configuration
│   ├── rules/             # Path-scoped rules (front/**, back/**)
│   └── skills/            # Nuxt, Kotlin/Spring Boot, Keycloak, ...
│
├── CLAUDE.md              # General project documentation
└── README.md
```

---

## 🎯 Usage

### Development Workflow

#### Adding a New Backend Feature

1. Create a new package under `/back/src/main/kotlin/features/feature-name`
2. Implement controllers, services, models, mappers, and repositories
3. Add database migration if needed in `/resources/db/migration`
4. Restart the backend service

#### Adding a New Frontend Page

1. Create a new file in `/front/app/pages/` (e.g., `about.vue` → `/about`)
2. Nuxt automatically generates routes based on file structure
3. Create components in `/front/app/components/` (auto-imported)
4. Add composables in `/front/app/composables/` for state management
5. Define API routes in `/front/server/api/` if needed (prefixed with `/api/`)

**Example:**
```vue
<!-- app/pages/users/[id].vue → /users/:id -->
<script setup lang="ts">
const route = useRoute()
const { data: user } = await useFetch(`/api/users/${route.params.id}`)
</script>

<template>
  <div>
    <h1>{{ user.name }}</h1>
  </div>
</template>
```

### Running Tests

**Backend:**
```bash
cd back
./mvnw test
```

**Frontend:**
```bash
cd front
pnpm test
```

### Building for Production

**Backend:**
```bash
cd back
./mvnw clean package
```

**Frontend:**
```bash
cd front
pnpm build
```

### Development Mode

**Frontend (with hot reload):**
```bash
cd front
pnpm dev
```

**Backend:**
```bash
cd back
./mvnw spring-boot:run
```

---

## 🔒 Security

This template includes:
- **Keycloak Authentication** with OpenID Connect (access, refresh, and ID tokens)
- **Password hashing** handled by Keycloak
- **Protected routes** requiring authentication
- **Token validation** using Keycloak public keys
- **Refresh token in an HTTP-only cookie**, access token kept in memory only
- **CORS configuration**
- **Environment-based secrets**
- **Centralized identity management** via Keycloak

**Important:** Always change default Keycloak admin credentials and secrets in production!

---

## 🌐 SSO Configuration (Optional)

To enable Google, Facebook, or other identity providers:

1. Access Keycloak Admin Console at http://localhost:8081/admin (admin/admin)
2. Navigate to your realm → Identity Providers
3. Add a new identity provider (Google, Facebook, GitHub, etc.)
4. Create OAuth app in [Google Cloud Console](https://console.cloud.google.com/), [Facebook Developers](https://developers.facebook.com/), or other provider
5. Copy the client ID and secret to Keycloak identity provider configuration
6. Configure redirect URLs in your OAuth app (Keycloak provides the redirect URL)
7. Keycloak automatically handles the SSO flow and token management

**Supported Providers:**
- Email/Password (default)
- Google, Facebook, GitHub, Microsoft
- Any OAuth 2.0 / OpenID Connect provider
- LDAP, SAML, and custom providers

---

## 🧪 Tech Stack Details

### Frontend
- **Nuxt 4** - Full-stack Vue 3 framework with SSR
- **Vue 3 Composition API** - Modern reactive UI
- **Nuxt UI** - Pre-built accessible components on Tailwind CSS
- **File-based Routing** - Automatic route generation
- **useFetch / useAsyncData** - Built-in data fetching
- **useState** - Built-in state management
- **Nitro** - Server engine for API routes
- **Tailwind CSS** - Utility-first CSS framework
- **Vite** - Next-generation frontend tooling
- **TypeScript** - Zero-config with auto-generated types

### Backend
- **Kotlin** - Modern JVM language
- **Spring Boot** - Production-ready framework
- **Exposed** - Kotlin SQL framework
- **Flyway** - Database migration tool
- **Maven** - Dependency management

### Authentication & Identity
- **Keycloak** - Identity and access management
- **OpenID Connect** - Modern authentication protocol
- **OAuth 2.0** - Authorization framework

### Database
- **PostgreSQL** - Robust relational database

---

## 📚 Documentation

- [Backend Architecture](./back/CLAUDE.md)
- [Frontend Architecture (Nuxt 4)](./front/CLAUDE.md)
- [General Documentation](./CLAUDE.md)
- [Claude Code Skills](./.claude/skills/)
  - [Nuxt 4 Development Guidelines](./.claude/skills/nuxt-dev-guidelines/)
  - [Kotlin Spring Boot Backend](./.claude/skills/kotlin-spring-boot-backend/)
  - [Keycloak IAM](./.claude/skills/keycloak/)

---

## 🤝 What This Template IS

✅ A solid starting point for new projects  
✅ A reference architecture with best practices  
✅ A time-saver for initial setup  
✅ Production-ready authentication system  

## 🚫 What This Template IS NOT

❌ A complete application ready to deploy  
❌ A rigid framework you can't modify  
❌ A one-size-fits-all solution  
❌ A replacement for understanding the stack  

**Adapt it to your needs!** This is YOUR starting point.

---

## 🗺️ Roadmap

- [ ] Configure Keycloak email verification flow
- [ ] Add password reset functionality via Keycloak
- [ ] Implement role-based access control (RBAC) with Keycloak roles
- [ ] Add API rate limiting
- [ ] Add logging and monitoring (ELK stack)
- [ ] Add CI/CD pipeline examples (GitHub Actions, GitLab CI)
- [ ] Configure additional SSO providers in Keycloak (GitHub, Microsoft, Apple)
- [ ] Add example business feature (e.g., todo list, dashboard)
- [ ] Add mobile app directory with React Native / Flutter
- [ ] Add WebSocket support with Nitro server

---

## 🐛 Troubleshooting

### Port already in use
If you get port conflicts, change the ports in `back/.env` (e.g. `KEYCLOAK_PORT`) or `back/docker-compose.yml`

### Database connection failed
Check your `back/.env` configuration and ensure the PostgreSQL container is running (`docker compose ps` in `back/`)

### Keycloak connection failed
- Ensure the Keycloak container is running: `docker ps | grep keycloak`
- Check Keycloak logs: `docker logs <container>` (name set by `DOCKER_KEYCLOAK_CONTAINER_NAME`, default `keycloak`)
- Verify `KEYCLOAK_EXTERNAL_URL` in `back/.env` matches your Keycloak address (default `http://localhost:8081`)

### Frontend can't reach backend
Verify `NUXT_PUBLIC_API_BASE_URL` (default `http://localhost:8080`, used by the `/api/**` proxy in `front/nuxt.config.ts`) and the CORS configuration in `back/src/main/kotlin/config/SecurityConfig.kt`

### Authentication redirect loop
- Clear browser cookies and local storage
- Verify Keycloak realm and client configuration
- Check that redirect URIs are correctly configured in Keycloak client settings

### MCP tools not working
Ensure you've added the Nuxt UI MCP server:
```bash
claude mcp add --transport http nuxt-ui-remote https://ui.nuxt.com/mcp
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Nuxt](https://nuxt.com/)
- [Nuxt UI](https://ui.nuxt.com/)
- [Vue.js](https://vuejs.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Keycloak](https://www.keycloak.org/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Claude Code](https://claude.com/claude-code)

---

## 📧 Support

If you have questions or need help:
- Open an issue on GitHub
- Check the documentation in `CLAUDE.md` files
- Review the example authentication feature

---

**Happy coding! 🚀**