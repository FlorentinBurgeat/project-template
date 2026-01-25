# Migration Guide: JWT to Keycloak

## Overview

This guide helps you migrate from the custom JWT authentication system to Keycloak-based authentication.

---

## What Changed

### Removed Components

The following components are **deprecated** and will be removed:

1. **Custom JWT Implementation**
   - `security/JwtProvider.kt` - JWT token generation/validation
   - `security/JwtAuthenticationFilter.kt` - Custom authentication filter
   - JWT dependencies in `pom.xml` (io.jsonwebtoken)

2. **Custom User Management**
   - `features/authentication/entity/User.kt` - User entity (now in Keycloak)
   - `features/authentication/entity/RefreshToken.kt` - Refresh token entity (now in Keycloak)
   - `features/authentication/repository/UserRepository.kt`
   - `features/authentication/repository/RefreshTokenRepository.kt`

3. **Custom Authentication Service**
   - `features/authentication/service/AuthService.kt` - Custom login/register logic
   - `features/authentication/controller/AuthController.kt` - Old auth endpoints

### Added Components

1. **Keycloak Integration** (`features/keycloak/`)
   - `config/KeycloakConfig.kt` - Keycloak admin client configuration
   - `service/RealmInitializationService.kt` - Automatic realm setup
   - `service/KeycloakAuthService.kt` - OAuth2 token management
   - `service/UserManagementService.kt` - Optional custom user operations
   - `controller/KeycloakAuthController.kt` - New auth endpoints

2. **Spring Security OAuth2**
   - OAuth2 Resource Server for JWT validation
   - Automatic token validation against Keycloak

3. **Docker Configuration**
   - Keycloak service in `docker-compose.yml`
   - Keycloak database (`keycloak_db`)

---

## Migration Steps

### For Existing Projects (With Users)

If you have existing users in the database:

#### Step 1: Export Users

```kotlin
// Create a migration script
val users = userRepository.findAll()
val exportData = users.map { user ->
    mapOf(
        "id" to user.id.toString(),
        "email" to user.email,
        "firstName" to user.firstName,
        "lastName" to user.lastName,
        "createdAt" to user.createdAt.toString()
    )
}
// Save to JSON file
File("users_export.json").writeText(Json.encodeToString(exportData))
```

#### Step 2: Import to Keycloak

```kotlin
// Import script using UserManagementService
val importData = Json.decodeFromString<List<Map<String, String>>>(
    File("users_export.json").readText()
)

importData.forEach { userData ->
    val keycloakUserId = userManagementService.createUser(
        email = userData["email"]!!,
        password = "TemporaryPassword123!", // Users must reset
        firstName = userData["firstName"],
        lastName = userData["lastName"],
        emailVerified = false
    )

    // Track migration in database
    keycloakUserMigrationRepository.save(
        legacyUserId = UUID.fromString(userData["id"]),
        keycloakUserId = keycloakUserId
    )
}
```

#### Step 3: Send Password Reset Emails

```kotlin
// Notify all migrated users to reset their password
importData.forEach { userData ->
    emailService.sendPasswordResetEmail(userData["email"]!!)
}
```

#### Step 4: Update References

If you have foreign keys to `users` table:

```sql
-- Add Keycloak user ID column to referencing tables
ALTER TABLE orders ADD COLUMN keycloak_user_id VARCHAR(255);

-- Update references using migration tracking table
UPDATE orders o
SET keycloak_user_id = (
    SELECT keycloak_user_id
    FROM keycloak_user_migration m
    WHERE m.legacy_user_id = o.user_id
);

-- Add foreign key constraint (optional, Keycloak is external)
-- Or keep as a simple VARCHAR for flexibility
```

#### Step 5: Clean Up

After verifying the migration is successful:

```sql
-- Drop old tables
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS keycloak_user_migration CASCADE;
```

---

### For New Projects (Template Forks)

If you're starting fresh:

#### Step 1: Update Environment

```bash
# Update .env with your realm name
KEYCLOAK_REALM_NAME=myproject-prod

# Generate secure secrets
KEYCLOAK_BACKEND_CLIENT_SECRET=$(openssl rand -base64 32)
```

#### Step 2: Start Services

```bash
docker-compose up -d
```

#### Step 3: Verify

```bash
# Check realm creation
docker-compose logs backend | grep "Realm.*initialized successfully"

# Access Keycloak Admin Console
open http://localhost:8081/admin
```

#### Step 4: Test Authentication

```bash
# Get login URL
curl http://localhost:8080/api/auth/login

# Register first user via Keycloak
# Or use the Admin Console to create test users
```

---

## API Changes

### Old Endpoints (Deprecated)

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### New Endpoints

#### Get Login URL

```http
GET /api/auth/login?redirect_uri=http://localhost:5173/callback

Response:
{
  "redirect_url": "http://localhost:8081/realms/myproject/protocol/openid-connect/auth?..."
}
```

#### Exchange Code for Tokens

```http
POST /api/auth/callback?code=ABC123&redirect_uri=http://localhost:5173/callback

Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "token_type": "Bearer"
}

Set-Cookie: refresh_token=...; HttpOnly; Path=/api/auth; Max-Age=604800
```

#### Refresh Token

```http
POST /api/auth/refresh
Cookie: refresh_token=...

Response:
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "token_type": "Bearer"
}
```

---

## Frontend Changes Required

### Old Frontend Code (JWT)

```typescript
// Login
const response = await fetch('/api/auth/login', {
  method: 'POST',
  body: JSON.stringify({ email, password })
})
const { accessToken, refreshToken } = await response.json()

// Store tokens
localStorage.setItem('access_token', accessToken)
localStorage.setItem('refresh_token', refreshToken)

// Use token
fetch('/api/users/me', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
})
```

### New Frontend Code (Keycloak)

```typescript
// Get login URL
const { redirect_url } = await fetch('/api/auth/login').then(r => r.json())

// Redirect to Keycloak
window.location.href = redirect_url

// Handle callback (in /callback route)
const params = new URLSearchParams(window.location.search)
const code = params.get('code')

const response = await fetch(`/api/auth/callback?code=${code}`, {
  method: 'POST',
  credentials: 'include' // Important: sends cookies
})

const { access_token } = await response.json()

// Store access token in memory (NOT localStorage)
let accessToken = access_token

// Use token
fetch('/api/users/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  credentials: 'include' // Include cookies
})

// Refresh token (automatic when 401)
const refreshToken = async () => {
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    credentials: 'include'
  })
  const { access_token } = await response.json()
  accessToken = access_token
  return accessToken
}
```

---

## Testing

### 1. Test Realm Creation

```bash
# Start services
docker-compose up -d

# Check logs
docker-compose logs backend | grep -i "realm"

# Expected output:
# Realm 'myproject' initialized successfully
```

### 2. Test Login Flow

```bash
# Get login URL
curl http://localhost:8080/api/auth/login

# Open URL in browser, login with Keycloak
# Get authorization code from redirect

# Exchange code for tokens
curl -X POST "http://localhost:8080/api/auth/callback?code=YOUR_CODE" \
  -c cookies.txt

# Check access token in response
# Check refresh_token cookie in cookies.txt
```

### 3. Test Protected Endpoints

```bash
# Use access token
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 4. Test Token Refresh

```bash
# Wait 15+ minutes or use short-lived token for testing
# Then refresh
curl -X POST http://localhost:8080/api/auth/refresh \
  -b cookies.txt \
  -c cookies.txt
```

---

## Rollback Plan

If you need to rollback to the old JWT system:

### 1. Revert Code Changes

```bash
git revert <commit-hash>
```

### 2. Stop Keycloak

```bash
docker-compose stop keycloak
```

### 3. Restore pom.xml

Uncomment JWT dependencies, remove OAuth2 dependencies.

### 4. Restore application.yml

Remove `spring.security.oauth2` configuration, restore `jwt` properties.

### 5. Restart Backend

```bash
docker-compose restart backend
```

---

## Troubleshooting

### "Realm initialization failed"

**Cause:** Backend can't connect to Keycloak

**Solution:**
```bash
# Verify Keycloak is running
docker ps | grep keycloak

# Check network connectivity
docker exec -it template_api ping keycloak

# Verify admin credentials in .env
```

### "Invalid token" errors

**Cause:** Token issuer mismatch

**Solution:**
```yaml
# In application.yml, ensure issuer-uri matches Keycloak realm
spring.security.oauth2.resourceserver.jwt.issuer-uri:
  http://keycloak:8080/realms/myproject
```

### Frontend gets CORS errors

**Solution:**
```kotlin
// Add frontend origin to SecurityConfig.kt
configuration.allowedOrigins = listOf(
    "http://localhost:5173",
    "https://your-frontend.com"
)
```

---

## Support

For detailed Keycloak configuration, see:
- [KEYCLOAK_SETUP.md](KEYCLOAK_SETUP.md) - Complete setup guide
- `.claude/skills/keycloak/` - Keycloak skill with detailed documentation
- Keycloak official docs: https://www.keycloak.org/documentation

---

**Version:** 1.0
**Last Updated:** January 2025
