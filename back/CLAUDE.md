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

### Endpoints

**Public endpoints:**
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh access token

**Protected endpoints:**
- All other endpoints require JWT Bearer token authentication
- Token must be included in `Authorization: Bearer <token>` header

### JWT Implementation
- **Access Token**: Short-lived token (e.g., 15 minutes) for API access
- **Refresh Token**: Long-lived token (e.g., 7 days) stored securely, used to obtain new access tokens
- Tokens contain user ID and roles/permissions for authorization

### SSO (OpenID Connect)
Infrastructure is ready to support OAuth2/OpenID Connect providers:
- Google
- Facebook
- Any other OpenID Connect compliant provider

Configuration is done via environment variables (client ID, secret, redirect URLs).

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

### Authentication Feature
Located in `/features/authentication`:
- User registration (email/password)
- User login (email/password)
- Token refresh
- Password change
- Email change
- Account deletion
- SSO integration points

This feature serves as a reference implementation for creating new features.

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
<!-- To be completed with actual skills -->
- TBD

---

**Version**: 1.0  
**Last Updated**: December 2024