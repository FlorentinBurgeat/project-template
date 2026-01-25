# CLAUDE.md - Project Template

## Project Overview

**project-template** is a production-ready full-stack template designed to kickstart any new web application project. It provides a solid foundation with complete authentication, modular architecture, and integrated development best practices.

### Purpose
Eliminate initial project setup time by providing a proven, configured, and functional structure from day one. Developers can clone this template and immediately focus on business features.

> **Note**: This project contains additional CLAUDE.md files in `/back` and `/front` directories with architecture-specific details.

---

## Tech Stack

### Frontend
- **Framework**: Vue 3 (Composition API)
- **Routing**: Vue Router
- **State Management**: Singleton composables (ref/reactive, no Pinia)
- **HTTP Client**: TanStack Query
- **Design System**: ShadCN + Tailwind CSS
- **Build Tool**: Vite

### Backend
- **Language**: Kotlin
- **Framework**: Spring Boot
- **Build Tool**: Maven
- **ORM**: Exposed
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Authentication**: Keycloak (OpenID Connect provider)
- **SSO**: Keycloak (supports Google, Facebook, and extensible providers)

### DevOps
- **Containerization**: Docker + Docker Compose
- **Configuration**: Environment variables

---

## Authentication Flow

### Keycloak-based Authentication
1. User authenticates via Keycloak (email/password, social providers, or other configured identity providers)
2. Keycloak returns OpenID Connect tokens (access token + refresh token + ID token)
3. Frontend stores tokens and uses them for all authenticated requests
4. Backend validates tokens using Keycloak's public key endpoints
5. All requests to protected endpoints include token in `Authorization: Bearer <token>` header

### Token Refresh
When access token expires, frontend automatically uses refresh token to obtain a new access token from Keycloak without re-prompting the user.

### Social Login & SSO
Keycloak supports multiple identity providers out of the box:
- Email/Password (default)
- Google, Facebook, and other OAuth 2.0/OpenID Connect providers
- LDAP, SAML, and custom providers (configurable)

The system centralizes identity management through Keycloak, eliminating the need for custom authentication logic.

---

## Configuration & Deployment

### Docker
The project includes a `docker-compose.yml` to launch the entire stack (frontend, backend, PostgreSQL) with a single command. Goal: any developer can clone the repo and launch the application in minutes.

### Environment Variables
The project uses environment variables for all sensitive or environment-specific configuration:
- Database credentials
- Keycloak server URL and realm
- Keycloak client ID and secret
- Service URLs (frontend, backend)
- Etc.

A `.env.example` file is provided with all necessary variables documented.

---

## Development Principles

### Extensibility
- Add new backend feature = create new feature package with controllers, services, models, etc.
- Add new frontend page = create new folder in `/pages` with components and composables
- Add new SSO provider = add OpenID Connect configuration

### Modularity
Each feature is isolated and can be removed or modified without impacting others. Dependencies between features should be minimal.

### Maintainability
- Code organized predictably
- Clear separation of concerns
- Mappers to isolate data transformations
- Shared types/models between frontend and backend

### Developer Experience
The template should allow to:
1. Clone the repo
2. Run `docker-compose up`
3. Start developing business features immediately

---

## Base Features Included

The template includes **Authentication & Account Management** via Keycloak by default:
- Registration (delegated to Keycloak)
- Login with email/password
- Social login (Google, Facebook, and other configured providers)
- Automatic token refresh
- Protected API endpoints with token validation
- SSO across multiple applications

### Default Pages (Frontend)
- Login/Registration page (redirects to Keycloak)
- Home page with header and navigation menu
- User settings page (redirects to Keycloak account management for password/email changes)

---

## Skills Reference

This section lists the available skills to work on this project. Skills are reusable prompts/workflows for common development tasks.

### Available Skills
<!-- To be completed with actual skills -->
- TBD

---

## What This Template Is NOT

- Not a framework, just a starting point
- Not a complete application, but a base to extend
- Not set in stone: adapt the structure to your specific needs

---

**Version**: 1.1
**Last Updated**: January 2025