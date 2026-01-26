# Project Template

A production-ready full-stack template to kickstart your next web application. Built with Nuxt 4, Kotlin Spring Boot, Keycloak, and PostgreSQL.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Features

- ✅ **Keycloak Authentication**: Centralized identity and access management with OpenID Connect
- ✅ **SSO Ready**: Multi-provider support (Email/Password, Google, Facebook, and extensible)
- ✅ **Account Management**: Registration, login, profile management (delegated to Keycloak)
- ✅ **Modern Frontend**: Nuxt 4 + Nuxt UI + Tailwind CSS + SSR
- ✅ **Robust Backend**: Kotlin + Spring Boot + Exposed ORM
- ✅ **Feature-Based Architecture**: Modular and scalable structure
- ✅ **Docker Ready**: One command to launch the entire stack
- ✅ **Database Migrations**: Flyway for schema versioning
- ✅ **Type-Safe**: End-to-end type safety with Kotlin and TypeScript
- ✅ **AI-Enhanced Development**: MCP server integration for Nuxt UI documentation

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** (v20.10+) and **Docker Compose** (v2.0+)
- **Node.js** (v20+) and **pnpm** (v9+)
- **Java JDK** (v17+)
- **Maven** (v3.8+)
- **Git**
- **Claude Code CLI** (optional, for MCP integration): Install from [claude.com](https://claude.com/download)

---

## 🛠️ Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/project-template.git
cd project-template
```

### 2. Configure environment variables

Copy the example environment file and configure it:

```bash
cp .env.example .env
```

Edit `.env` and set your values:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=project_template
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password

# Keycloak
KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=project-template
KEYCLOAK_CLIENT_ID=frontend-app
KEYCLOAK_CLIENT_SECRET=your_keycloak_client_secret

# Service URLs
FRONTEND_URL=http://localhost:3000
BACKEND_URL=http://localhost:8080
```

### 3. Launch with Docker

```bash
docker-compose up
```

This will start:
- **Frontend (Nuxt 4)**: http://localhost:3000
- **Backend API (Spring Boot)**: http://localhost:8080
- **Keycloak**: http://localhost:8180
- **PostgreSQL**: localhost:5432

### 4. Access the application

Open your browser and navigate to:
- **Application**: http://localhost:3000
- **Keycloak Admin Console**: http://localhost:8180/admin (admin/admin)
- **API Documentation**: http://localhost:8080/swagger-ui.html (if Swagger is configured)

### 5. Setup MCP for Nuxt UI Documentation (Optional)

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
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── features/      # Feature-based modules
│   │   │   │       └── authentication/
│   │   │   │           ├── controllers/
│   │   │   │           ├── services/
│   │   │   │           ├── models/
│   │   │   │           ├── mappers/
│   │   │   │           └── repositories/
│   │   │   └── resources/
│   │   │       └── db/migration/  # Flyway migrations
│   ├── pom.xml
│   └── CLAUDE.md          # Backend architecture docs
│
├── front/                 # Nuxt 4 frontend
│   ├── app/               # Nuxt 4 app directory
│   │   ├── pages/         # File-based routing
│   │   ├── components/    # Auto-imported components
│   │   ├── composables/   # Auto-imported composables
│   │   ├── layouts/       # Page layouts
│   │   ├── middleware/    # Route middleware
│   │   ├── plugins/       # Vue plugins
│   │   ├── utils/         # Auto-imported utilities
│   │   ├── assets/        # Build-time assets
│   │   └── app.vue        # Root component
│   ├── server/            # Nitro server directory
│   │   ├── api/           # API routes (prefixed /api)
│   │   ├── routes/        # Server routes (no prefix)
│   │   └── middleware/    # Server middleware
│   ├── public/            # Static assets
│   ├── nuxt.config.ts     # Nuxt configuration
│   ├── package.json
│   └── CLAUDE.md          # Frontend architecture docs
│
├── .claude/               # Claude Code skills & documentation
│   └── skills/
│       ├── nuxt-dev-guidelines/   # Nuxt 4 development guide
│       ├── kotlin-spring-boot-backend/
│       └── keycloak/      # Keycloak IAM expertise
│
├── docker-compose.yml
├── .env.example
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
- **Password hashing** via Keycloak (bcrypt)
- **Protected routes** requiring authentication
- **Token validation** using Keycloak public keys
- **CORS configuration**
- **Environment-based secrets**
- **Centralized identity management** via Keycloak

**Important:** Always change default Keycloak admin credentials and secrets in production!

---

## 🌐 SSO Configuration (Optional)

To enable Google, Facebook, or other identity providers:

1. Access Keycloak Admin Console at http://localhost:8180/admin (admin/admin)
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
If you get port conflicts, modify ports in `docker-compose.yml`

### Database connection failed
Check your `.env` configuration and ensure PostgreSQL is running

### Keycloak connection failed
- Ensure Keycloak container is running: `docker ps | grep keycloak`
- Check Keycloak logs: `docker logs keycloak`
- Verify `KEYCLOAK_URL` in `.env` matches your Keycloak server address

### Frontend can't reach backend
Verify CORS configuration and API URL in `nuxt.config.ts`

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