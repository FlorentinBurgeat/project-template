# CLAUDE.md - Backend

## Architecture Overview

The backend follows a **feature-based architecture** where each business functionality is isolated in its own module containing all necessary elements.

---

## Tech Stack

- **Language**: Kotlin
- **Framework**: Spring Boot
- **Build Tool**: Maven
- **ORM**: Exposed
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Authentication**: JWT with refresh tokens
- **SSO**: OpenID Connect ready

---

## Feature-Based Structure

### Feature Package Organization
Each feature is a self-contained module with the following structure:

```
/feature-name
  /controllers     - REST API endpoints
  /services        - Business logic
  /models          - Entities and business objects
  /mappers         - DTO ↔ Entity transformations
  /repositories    - Database connectors (Exposed)
  /connectors      - External service connectors (optional)
```

### Principles
- **Isolation**: Each feature is independent and can be added/removed without affecting others
- **Cohesion**: All elements related to a feature stay together
- **Minimal coupling**: Features should have minimal dependencies on each other

---

## Authentication & Security

### Keycloak Integration

This backend uses **Keycloak** as the identity and access management solution. Each project fork can have its own isolated realm on a shared Keycloak instance.

**Key Features:**
- OAuth2 / OpenID Connect authentication
- Multi-realm support for project isolation
- Automatic realm initialization on startup
- HTTP-only cookies for refresh tokens (XSS protection)
- Built-in user management via Keycloak Admin API

### Endpoints

**Public endpoints:**
- `GET /api/auth/login` - Returns Keycloak login URL
- `GET /api/auth/register` - Returns Keycloak registration URL
- `POST /api/auth/callback` - Exchanges authorization code for tokens
- `POST /api/auth/refresh` - Refreshes access token using HTTP-only cookie
- `POST /api/auth/logout` - Revokes tokens and clears cookies

**Protected endpoints:**
- All other endpoints require JWT Bearer token from Keycloak
- Token must be included in `Authorization: Bearer <token>` header

### Authentication Flow

1. **Login**: Frontend redirects to Keycloak login page
2. **Callback**: Keycloak redirects back with authorization code
3. **Token Exchange**: Backend exchanges code for tokens
4. **Token Storage**:
   - Access token: In-memory (frontend) - 15 minutes
   - Refresh token: HTTP-only cookie - 7 days
5. **API Calls**: Frontend sends access token in Authorization header
6. **Token Refresh**: Automatic refresh using HTTP-only cookie

### Realm Management

The `RealmInitializationService` automatically creates and configures the realm on startup:
- Creates realm if it doesn't exist
- Configures frontend client (public SPA)
- Configures backend client (confidential service)
- Sets up token lifespans and security policies
- Configures OIDC protocol mappers

**Realm Naming:** Set via `KEYCLOAK_REALM_NAME` environment variable
- Example: `project-template-dev`
- Example: `myproject-prod`

### User Management

**Option A: Keycloak-Managed (Recommended)**
- Users register via Keycloak's built-in registration page
- Built-in security, email verification, password policies
- Less code to maintain

**Option B: Custom Registration (Advanced)**
- Use `UserManagementService` for custom registration flows
- Provides full control over registration UI/UX
- Example code provided for reference

### SSO (OpenID Connect)
Keycloak natively supports SSO providers:
- Google
- Facebook
- GitHub
- Microsoft Azure AD
- Any OpenID Connect or SAML 2.0 provider

Configuration is done via Keycloak Admin Console or Admin API.

### Security Features

- **HTTP-only cookies**: Refresh tokens protected from XSS
- **CORS**: Configured for allowed frontend origins
- **SameSite cookies**: CSRF protection
- **Brute force protection**: Built into Keycloak
- **Session management**: Idle timeout, max session lifespan
- **Token validation**: Spring Security OAuth2 Resource Server

### Setup Documentation

See [KEYCLOAK_SETUP.md](KEYCLOAK_SETUP.md) for:
- Quick start guide
- Environment configuration
- Deployment modes (dev standalone, prod clustered)
- Troubleshooting
- Production considerations

---

## Database

### Technology
PostgreSQL with Flyway for schema migrations.

### Migrations
- Migration scripts are versioned (`V1__initial_schema.sql`, `V2__add_user_preferences.sql`, etc.)
- Applied automatically on application startup
- Ensures database schema is always synchronized with code

### Initial Schema
The template includes tables for:
- **Users**: credentials, email, created/updated timestamps
- **Refresh Tokens**: token value, expiration, user association
- **User Configuration**: preferences, settings

### ORM: Exposed
Exposed is used for type-safe SQL queries in Kotlin. Each feature has its own repository layer using Exposed DSL or DAO pattern.

---

## Base Features Included

### Keycloak Feature
Located in `/features/keycloak`:
- Keycloak admin client configuration (`KeycloakConfig`)
- Automatic realm initialization (`RealmInitializationService`)
- OAuth2 authentication flow (`KeycloakAuthController`, `KeycloakAuthService`)
- User management service (`UserManagementService`) - optional for custom flows
- HTTP-only cookie handling for refresh tokens
- Integration with Spring Security OAuth2 Resource Server

### Legacy Authentication Feature (Deprecated)
Located in `/features/authentication`:
- ⚠️ **DEPRECATED**: Custom JWT implementation replaced by Keycloak
- Kept for reference during migration
- Will be removed in future versions

The Keycloak feature serves as a reference implementation for:
- OAuth2/OIDC integration
- Keycloak Admin API usage
- Multi-realm architecture

---

## Configuration

### Environment Variables
All sensitive configuration is externalized via environment variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME` - Database connection
- `DB_USERNAME`, `DB_PASSWORD` - Database credentials
- `JWT_SECRET` - Secret for signing JWT tokens
- `JWT_ACCESS_EXPIRATION` - Access token expiration time
- `JWT_REFRESH_EXPIRATION` - Refresh token expiration time
- `SSO_GOOGLE_CLIENT_ID`, `SSO_GOOGLE_CLIENT_SECRET` - Google SSO config
- `SSO_FACEBOOK_CLIENT_ID`, `SSO_FACEBOOK_CLIENT_SECRET` - Facebook SSO config

See `.env.example` for complete list with descriptions.

---

## Adding a New Feature

### Steps
1. Create new package under `/features/feature-name`
2. Create controllers for REST endpoints
3. Create services for business logic
4. Create models/entities
5. Create mappers for DTO transformations
6. Create repositories for database access
7. (Optional) Create connectors for external services
8. Add database migration if schema changes are needed

### Example Structure
```kotlin
/features/todo
  /controllers
    TodoController.kt
  /services
    TodoService.kt
  /models
    Todo.kt
    TodoDTO.kt
  /mappers
    TodoMapper.kt
  /repositories
    TodoRepository.kt
```

---

## Skills Reference

This section lists the available skills for backend development tasks.

### Available Skills

- **kotlin-spring-boot-backend** - Kotlin/Spring Boot development with REST controllers, services, repositories, DDD patterns, and Exposed ORM
- **keycloak** - Keycloak identity and access management for server setup, realm management, clustering, and admin client integration
- **route-tester** - HTTP API route testing patterns with JWT authentication and integration testing strategies

---

**Version**: 1.0  
**Last Updated**: December 2024