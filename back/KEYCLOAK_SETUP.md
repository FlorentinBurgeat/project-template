# Keycloak Setup Guide

## Overview

This template uses **Keycloak** as the identity and access management solution, replacing the previous custom JWT implementation. Each project fork can have its own isolated realm on a shared Keycloak instance.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture](#architecture)
3. [Environment Configuration](#environment-configuration)
4. [Deployment Modes](#deployment-modes)
5. [Realm Initialization](#realm-initialization)
6. [Authentication Flow](#authentication-flow)
7. [User Registration Options](#user-registration-options)
8. [Token Management](#token-management)
9. [Troubleshooting](#troubleshooting)
10. [Production Considerations](#production-considerations)

---

## Quick Start

### 1. Update Environment Variables

Copy `.env.template` to `.env` and customize:

```bash
cp .env.template .env
```

**Critical variables to set:**

```env
# Unique realm name for your project fork
KEYCLOAK_REALM_NAME=your-project-realm

# Keycloak admin credentials (change in production!)
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=secure-password

# Backend client secret (change in production!)
KEYCLOAK_BACKEND_CLIENT_SECRET=very-secure-secret
```

### 2. Start Services

```bash
docker-compose up -d
```

This will start:
- PostgreSQL (shared by app and Keycloak)
- Keycloak (on port 8081)
- Backend API (on port 8080)

### 3. Verify Realm Creation

The backend automatically creates the realm on startup. Check logs:

```bash
docker-compose logs backend | grep "Realm"
```

You should see:
```
Realm 'your-project-realm' initialized successfully
```

### 4. Access Keycloak Admin Console

Open: http://localhost:8081/admin

Login with admin credentials from `.env` file.

---

## Architecture

### Deployment Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Frontend  │─────▶│   Backend    │─────▶│  Keycloak   │
│   (Vue 3)   │◀─────│  (Kotlin/    │◀─────│   Server    │
│             │      │Spring Boot)  │      │             │
└─────────────┘      └──────────────┘      └─────────────┘
                            │                      │
                            ▼                      ▼
                     ┌──────────────────────────────┐
                     │      PostgreSQL Database      │
                     │  - App DB: wishlist_db       │
                     │  - Keycloak DB: keycloak_db  │
                     └──────────────────────────────┘
```

### Multi-Realm Shared Instance

```
┌────────────────────────────────────────────┐
│         Keycloak Instance (Shared)         │
├────────────────────────────────────────────┤
│  Realm: project-template-dev              │
│    - Users: Alice, Bob                     │
│    - Clients: frontend-client, backend     │
├────────────────────────────────────────────┤
│  Realm: myproject-prod                     │
│    - Users: John, Jane                     │
│    - Clients: frontend-client, backend     │
├────────────────────────────────────────────┤
│  Realm: acme-staging                       │
│    - Users: Test User 1, Test User 2       │
│    - Clients: frontend-client, backend     │
└────────────────────────────────────────────┘
```

Each project fork has its own realm with isolated:
- Users
- Clients
- Roles
- Sessions

---

## Environment Configuration

### Required Environment Variables

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `KEYCLOAK_REALM_NAME` | Unique realm name per project | `template-realm` | `myproject-prod` |
| `KEYCLOAK_ADMIN_USERNAME` | Keycloak admin username | `admin` | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | `admin` | `SuperSecure123!` |
| `KEYCLOAK_PORT` | External port for Keycloak | `8081` | `8081` |
| `KEYCLOAK_INTERNAL_URL` | Internal URL (Docker network) | `http://keycloak:8080` | - |
| `KEYCLOAK_EXTERNAL_URL` | External URL (browser) | `http://localhost:8081` | `https://auth.myapp.com` |
| `KEYCLOAK_FRONTEND_CLIENT_ID` | Frontend SPA client ID | `frontend-client` | `myapp-web` |
| `KEYCLOAK_BACKEND_CLIENT_ID` | Backend service client ID | `backend-service` | `myapp-api` |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | Backend client secret | `change-this` | `Generated-Secret-123` |
| `KEYCLOAK_DB_NAME` | Keycloak database name | `keycloak_db` | `keycloak_db` |

### Frontend vs Backend URLs

**KEYCLOAK_INTERNAL_URL**: Used by backend (inside Docker network)
- Example: `http://keycloak:8080`
- Backend uses this to validate JWT tokens

**KEYCLOAK_EXTERNAL_URL**: Used by frontend (browser)
- Example: `http://localhost:8081` (dev)
- Example: `https://auth.myapp.com` (prod)
- Frontend redirects user to this URL for login

---

## Deployment Modes

### Development Mode (Standalone)

Default configuration for local development.

**Characteristics:**
- Single Keycloak instance
- No clustering
- H2 or PostgreSQL database
- Fast startup
- Minimal resources

**Configuration:**
```yaml
keycloak:
  command: start-dev
```

### Production Mode (Domain Clustered)

For production deployments requiring high availability.

**Characteristics:**
- Multiple Keycloak instances
- Infinispan clustering
- Session replication
- Load balancer required
- PostgreSQL required

**Configuration:**
See Keycloak skill documentation for detailed setup:
- `/DEPLOYMENT_MODES.md` - Complete clustering guide
- Domain mode setup with host controllers
- JGroups network configuration
- Load balancer configuration

---

## Realm Initialization

The backend automatically initializes the realm on startup using `RealmInitializationService`.

### What Gets Created

1. **Realm**: Named from `KEYCLOAK_REALM_NAME`
2. **Frontend Client** (Public SPA)
   - Client ID: `frontend-client`
   - Type: Public (no secret)
   - Flows: Authorization Code + PKCE
   - Redirect URIs: `http://localhost:5173/*`
3. **Backend Client** (Confidential Service)
   - Client ID: `backend-service`
   - Type: Confidential (with secret)
   - Service accounts enabled
4. **Token Settings**
   - Access token: 15 minutes
   - Refresh token: 7 days
   - SSO session: 30 minutes idle, 10 hours max

### Manual Realm Creation

If you need to create the realm manually:

```bash
# Access Keycloak admin CLI
docker exec -it keycloak bash

# Login to admin CLI
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Create realm
/opt/keycloak/bin/kcadm.sh create realms \
  -s realm=my-realm \
  -s enabled=true
```

Or use the Keycloak Admin API (see `keycloak` skill).

---

## Authentication Flow

### Authorization Code Flow with PKCE (Recommended)

```
1. User clicks "Login" in frontend
   ↓
2. Frontend calls: GET /api/auth/login
   ← Backend returns: { "redirect_url": "https://keycloak.../auth?..." }
   ↓
3. Frontend redirects to Keycloak login page
   ↓
4. User enters credentials on Keycloak
   ↓
5. Keycloak redirects back: http://localhost:5173/callback?code=ABC123
   ↓
6. Frontend calls: POST /api/auth/callback?code=ABC123
   ↓
7. Backend exchanges code for tokens with Keycloak
   ↓
8. Backend sets refresh_token as HTTP-only cookie
   ← Backend returns: { "access_token": "...", "expires_in": 900 }
   ↓
9. Frontend stores access_token in memory
   ↓
10. Frontend makes API calls with: Authorization: Bearer <access_token>
```

### Token Refresh Flow

```
1. Access token expires (after 15 minutes)
   ↓
2. Frontend detects 401 Unauthorized
   ↓
3. Frontend calls: POST /api/auth/refresh
   (refresh_token sent automatically via HTTP-only cookie)
   ↓
4. Backend exchanges refresh_token for new tokens
   ↓
5. Backend sets new refresh_token cookie
   ← Backend returns: { "access_token": "...", "expires_in": 900 }
   ↓
6. Frontend retries original request with new access_token
```

---

## User Registration Options

### Option A: Keycloak-Managed Registration (Recommended)

**Pros:**
- ✅ Built-in security (CSRF, rate limiting, captcha)
- ✅ Email verification out of the box
- ✅ Less code to maintain
- ✅ Password policies enforced

**Implementation:**

```typescript
// Frontend
const registerUrl = await fetch('/api/auth/register').then(r => r.json())
window.location.href = registerUrl.redirect_url
```

User fills out Keycloak's registration form, then redirects back to your app.

### Option B: Custom Registration (Advanced)

**Use when you need:**
- Custom registration UI/UX
- Additional business logic
- Integration with other systems

**Implementation:**

See `UserManagementService.kt` for example code:

```kotlin
userManagementService.createUser(
    email = "user@example.com",
    password = "secure-password",
    firstName = "John",
    lastName = "Doe"
)
```

**Note:** You'll need to handle security (rate limiting, captcha, etc.) yourself.

---

## Token Management

### Access Token

**Storage:** In-memory (JavaScript variable)
**Lifetime:** 15 minutes
**Usage:** Sent in Authorization header
**Security:** XSS can steal, but short-lived

```typescript
let accessToken: string | null = null

// Make API request
fetch('/api/users/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
})
```

### Refresh Token

**Storage:** HTTP-only cookie
**Lifetime:** 7 days
**Usage:** Automatically sent by browser
**Security:** XSS-proof, CSRF-protected

```kotlin
// Backend sets cookie
val cookie = Cookie("refresh_token", refreshToken).apply {
    isHttpOnly = true  // JavaScript cannot access
    secure = true      // HTTPS only in production
    sameSite = "Strict" // CSRF protection
    maxAge = 604800    // 7 days
}
```

### Security Comparison

| Storage | XSS Vulnerable? | CSRF Vulnerable? | Recommended |
|---------|----------------|------------------|-------------|
| localStorage | ✅ Yes | ❌ No | ❌ No |
| In-memory + HTTP-only cookie | ⚠️ Access token only (15 min) | ❌ With SameSite | ✅ Yes |

---

## Troubleshooting

### Realm Not Created

**Symptoms:** Backend logs show "Failed to initialize realm"

**Causes:**
1. Keycloak not accessible from backend
2. Wrong admin credentials
3. Realm already exists with different config

**Solution:**
```bash
# Check Keycloak is running
docker ps | grep keycloak

# Check backend can reach Keycloak
docker exec -it template_api ping keycloak

# Verify admin credentials
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin
```

### Invalid Token Errors

**Symptoms:** 401 Unauthorized with "Invalid token"

**Causes:**
1. Wrong `issuer-uri` in application.yml
2. Clock skew between backend and Keycloak
3. Token expired

**Solution:**
```yaml
# Verify issuer URI matches exactly
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8080/realms/template-realm
```

### CORS Errors

**Symptoms:** Frontend gets "CORS policy" errors

**Solution:**
```kotlin
// In SecurityConfig.kt, add frontend origin
configuration.allowedOrigins = listOf(
    "http://localhost:5173",
    "http://localhost:3000",
    "https://your-frontend-domain.com"
)
```

---

## Production Considerations

### 1. HTTPS/TLS

**Development:**
```yaml
KC_HTTP_ENABLED: true
KC_HOSTNAME_STRICT_HTTPS: false
```

**Production:**
```yaml
KC_HTTP_ENABLED: false
KC_HOSTNAME_STRICT_HTTPS: true
KC_HOSTNAME: auth.myapp.com
```

Add TLS certificate:
```bash
docker run -v /path/to/certs:/opt/keycloak/certs \
  -e KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/certs/cert.pem \
  -e KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/certs/key.pem \
  quay.io/keycloak/keycloak:26.0.7 start
```

### 2. Database

Use external PostgreSQL with:
- Connection pooling
- High availability (replication)
- Regular backups
- Encrypted connections

### 3. Secrets

**Never commit:**
- `KEYCLOAK_ADMIN_PASSWORD`
- `KEYCLOAK_BACKEND_CLIENT_SECRET`
- Database passwords

**Use:**
- Environment variables
- Secret management (Vault, AWS Secrets Manager)
- Kubernetes secrets

### 4. Monitoring

Enable metrics:
```yaml
KC_METRICS_ENABLED: true
KC_HEALTH_ENABLED: true
```

Monitor:
- `/health/live` - Liveness probe
- `/health/ready` - Readiness probe
- `/metrics` - Prometheus metrics

### 5. Email Configuration

For password reset and email verification:

```yaml
# In Keycloak Admin Console
Realm Settings → Email
  - SMTP Host: smtp.gmail.com
  - Port: 587
  - From: noreply@myapp.com
  - Enable StartTLS: ON
  - Username: smtp-user
  - Password: smtp-password
```

---

## Additional Resources

- **Keycloak Skill**: `/path/to/.claude/skills/keycloak/` - Comprehensive Keycloak guide
- **Official Docs**: https://www.keycloak.org/documentation
- **Admin REST API**: https://www.keycloak.org/docs-api/latest/rest-api/
- **Security Best Practices**: https://www.keycloak.org/docs/latest/server_admin/#_hardening

---

**Version:** 1.0
**Last Updated:** January 2025
