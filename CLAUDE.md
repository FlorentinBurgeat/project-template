# CLAUDE.md - Project Template

## Project Overview

**project-template** is a production-ready full-stack template designed to kickstart any new web application project. It provides a solid foundation with complete authentication, modular architecture, and integrated development best practices.

### Purpose
Eliminate initial project setup time by providing a proven, configured, and functional structure from day one. Developers can clone this template and immediately focus on business features.

> **Note**: This project contains additional CLAUDE.md files in `/backend` and `/frontend` directories with architecture-specific details.

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
- **Authentication**: JWT (with refresh tokens)
- **SSO**: OpenID Connect (Google, Facebook, extensible)

### DevOps
- **Containerization**: Docker + Docker Compose
- **Configuration**: Environment variables

---

## Authentication Flow

### Classic Registration & Login
1. User registers with email and password
2. Backend returns JWT access token + refresh token
3. Frontend stores tokens (localStorage/cookies depending on implementation)
4. All requests to protected endpoints include token in `Authorization: Bearer <token>` header

### Refresh Token
When access token expires, frontend automatically uses refresh token to obtain a new access token without re-requesting credentials.

### SSO (OpenID Connect)
The system is designed to support SSO providers (Google, Facebook, etc.) via OpenID Connect. Infrastructure is ready, full implementation can be added as needed.

---

## Configuration & Deployment

### Docker
The project includes a `docker-compose.yml` to launch the entire stack (frontend, backend, PostgreSQL) with a single command. Goal: any developer can clone the repo and launch the application in minutes.

### Environment Variables
The project uses environment variables for all sensitive or environment-specific configuration:
- Database credentials
- JWT secrets
- Service URLs
- SSO configuration (client IDs, secrets)
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

The template includes the **Authentication & Account Management** feature by default:
- Registration (email/password)
- Login (email/password)
- Refresh token
- Password change
- Email change
- Account deletion
- SSO ready (Google, Facebook via OpenID Connect)

### Default Pages (Frontend)
- Login/Registration page
- Home page with header and navigation menu
- User settings page (change email/password, account deletion)

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

**Version**: 1.0  
**Last Updated**: December 2024