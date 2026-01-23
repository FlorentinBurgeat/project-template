# Keycloak Realm Management

Comprehensive guide to creating, configuring, and managing Keycloak realms.

## Table of Contents

- [RealmRepresentation Reference](#realmrepresentation-reference)
- [Realm Configuration Options](#realm-configuration-options)
- [Client Management](#client-management)
- [User Management](#user-management)
- [Role Management](#role-management)
- [Identity Providers](#identity-providers)
- [Authentication Flows](#authentication-flows)
- [Event Listeners](#event-listeners)
- [Import/Export](#importexport)

---

## RealmRepresentation Reference

Complete reference for the `RealmRepresentation` object used in REST API and Java Admin Client.

### Basic Properties

```kotlin
val realm = RealmRepresentation().apply {
    // Core identification
    realm = "my-app-realm"                      // Realm name (required, unique)
    id = "a1b2c3d4-e5f6-7890-1234-567890abcdef" // Internal ID (optional)
    displayName = "My Application Realm"        // Display name
    displayNameHtml = "<b>My Application</b>"   // HTML display name
    isEnabled = true                            // Enable/disable realm

    // Registration and account management
    isRegistrationAllowed = true                // Allow user self-registration
    isRegistrationEmailAsUsername = true        // Use email as username
    isEditUsernameAllowed = false               // Allow username changes
    isResetPasswordAllowed = true               // Allow password reset
    isRememberMe = true                         // Enable "remember me" on login
    isVerifyEmail = true                        // Require email verification
    isLoginWithEmailAllowed = true              // Allow login with email
    isDuplicateEmailsAllowed = false            // Allow duplicate emails

    // Password policies
    passwordPolicy = "length(8) and digits(1) and specialChars(1) and notUsername()"

    // Token settings
    accessTokenLifespan = 300                   // Access token lifespan (seconds)
    accessTokenLifespanForImplicitFlow = 900    // For implicit flow
    ssoSessionIdleTimeout = 1800                // SSO session idle timeout
    ssoSessionMaxLifespan = 36000               // SSO session max lifespan
    offlineSessionIdleTimeout = 2592000         // Offline session idle (30 days)
    offlineSessionMaxLifespan = 5184000         // Offline session max (60 days)
    refreshTokenMaxReuse = 0                    // Refresh token reuse count
    accessCodeLifespan = 60                     // Authorization code lifespan
    accessCodeLifespanLogin = 1800              // Login action timeout
    accessCodeLifespanUserAction = 300          // User action timeout

    // Security
    isBruteForceProtected = true                // Enable brute force detection
    isPermanentLockout = false                  // Permanent vs temporary lockout
    maxFailureWaitSeconds = 900                 // Wait time after failures
    minimumQuickLoginWaitSeconds = 60           // Minimum wait between logins
    waitIncrementSeconds = 60                   // Wait time increment
    quickLoginCheckMilliSeconds = 1000L         // Quick login detection window
    maxDeltaTimeSeconds = 43200                 // Max delta for failure tracking
    failureFactor = 30                          // Failure threshold

    // SSL/TLS
    sslRequired = "external"                    // "all", "external", or "none"

    // SMTP (email)
    smtpServer = mapOf(
        "host" to "smtp.example.com",
        "port" to "587",
        "from" to "noreply@example.com",
        "fromDisplayName" to "My Application",
        "replyTo" to "support@example.com",
        "auth" to "true",
        "user" to "smtp-user",
        "password" to "smtp-password",
        "starttls" to "true"
    )

    // Themes
    loginTheme = "keycloak"                     // Login page theme
    accountTheme = "keycloak"                   // Account management theme
    adminTheme = "keycloak"                     // Admin console theme
    emailTheme = "keycloak"                     // Email template theme

    // Internationalization
    isInternationalizationEnabled = true
    supportedLocales = setOf("en", "fr", "es", "de")
    defaultLocale = "en"

    // User federation
    userFederationProviders = listOf(/* providers */)
    userFederationMappers = listOf(/* mappers */)

    // Events
    isEventsEnabled = true                      // Enable event logging
    eventsExpiration = 604800L                  // Event expiration (7 days)
    eventsListeners = listOf("jboss-logging")
    enabledEventTypes = listOf(
        "LOGIN", "LOGIN_ERROR", "LOGOUT", "REGISTER",
        "UPDATE_PASSWORD", "UPDATE_PROFILE"
    )
    isAdminEventsEnabled = true                 // Enable admin event logging
    isAdminEventsDetailsEnabled = true          // Include details in admin events

    // Default roles
    defaultRoles = listOf("user", "offline_access")

    // Client scopes
    defaultDefaultClientScopes = listOf("email", "profile", "roles")
    defaultOptionalClientScopes = listOf("address", "phone", "microprofile-jwt")
}
```

### Complete Example

```kotlin
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.RealmRepresentation

val keycloak = KeycloakBuilder.builder()
    .serverUrl("http://localhost:8080")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("admin")
    .build()

val realm = RealmRepresentation().apply {
    realm = "production-realm"
    displayName = "Production Application"
    isEnabled = true

    // Registration settings
    isRegistrationAllowed = true
    isRegistrationEmailAsUsername = true
    isVerifyEmail = true
    isResetPasswordAllowed = true

    // Security settings
    isBruteForceProtected = true
    maxFailureWaitSeconds = 900
    failureFactor = 5
    sslRequired = "external"

    // Password policy
    passwordPolicy = "length(12) and upperCase(1) and lowerCase(1) and " +
        "digits(1) and specialChars(1) and notUsername() and " +
        "passwordHistory(3)"

    // Token settings
    accessTokenLifespan = 900              // 15 minutes
    ssoSessionIdleTimeout = 1800           // 30 minutes
    ssoSessionMaxLifespan = 36000          // 10 hours
    refreshTokenMaxReuse = 0               // One-time use

    // Themes
    loginTheme = "custom-theme"
    accountTheme = "custom-theme"
    emailTheme = "custom-theme"

    // SMTP configuration
    smtpServer = mapOf(
        "host" to "smtp.gmail.com",
        "port" to "587",
        "from" to "noreply@myapp.com",
        "fromDisplayName" to "My Application",
        "auth" to "true",
        "user" to "noreply@myapp.com",
        "password" to "app-specific-password",
        "starttls" to "true"
    )

    // Events
    isEventsEnabled = true
    eventsExpiration = 604800L
    eventsListeners = listOf("jboss-logging", "custom-listener")
}

// Create the realm
keycloak.realms().create(realm)
```

---

## Realm Configuration Options

### Updating Existing Realm

```kotlin
// Get existing realm
val realm = keycloak.realm("my-realm").toRepresentation().apply {
    // Modify settings
    accessTokenLifespan = 1800
    isBruteForceProtected = true
}

// Update realm
keycloak.realm("my-realm").update(realm)
```

### REST API Example

```bash
# Get access token
TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" | jq -r '.access_token')

# Update realm
curl -X PUT http://localhost:8080/admin/realms/my-realm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "my-realm",
    "enabled": true,
    "accessTokenLifespan": 1800,
    "bruteForceProtected": true,
    "failureFactor": 5,
    "passwordPolicy": "length(12)"
  }'
```

---

## Client Management

### Creating a Client

```kotlin
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.ProtocolMapperRepresentation

val client = ClientRepresentation().apply {
    // Basic settings
    clientId = "my-application"
    name = "My Application"
    description = "Main application client"
    isEnabled = true

    // Client type
    isPublicClient = false                      // Confidential client
    secret = "client-secret-here"               // Client secret
    isBearerOnly = false                        // Can initiate login

    // Protocol
    protocol = "openid-connect"                 // or "saml"

    // URLs
    rootUrl = "https://app.example.com"
    baseUrl = "/"
    redirectUris = listOf(
        "https://app.example.com/*",
        "https://app.example.com/callback",
        "http://localhost:3000/*"  // For development
    )
    webOrigins = listOf(
        "https://app.example.com",
        "http://localhost:3000"
    )
    adminUrl = "https://app.example.com/admin"

    // Authentication flow
    isStandardFlowEnabled = true                // Authorization Code flow
    isImplicitFlowEnabled = false               // Implicit flow
    isDirectAccessGrantsEnabled = true          // Resource Owner Password flow
    isServiceAccountsEnabled = true             // Client Credentials flow

    // Advanced settings
    isConsentRequired = false                   // Require user consent
    isFrontchannelLogout = true                 // Front-channel logout
    isAlwaysDisplayInConsole = false
    isFullScopeAllowed = true

    // Token settings
    attributes = mapOf(
        "access.token.lifespan" to "900",
        "client.session.idle.timeout" to "1800",
        "client.session.max.lifespan" to "36000"
    )
}

// Create client
val response = keycloak.realm("my-realm").clients().create(client)
val clientUuid = response.location.path.substringAfterLast("/")
```

### Client Scopes

```kotlin
import org.keycloak.representations.idm.ClientScopeRepresentation

// Create custom client scope
val scope = ClientScopeRepresentation().apply {
    name = "custom-claims"
    description = "Custom claims for application"
    protocol = "openid-connect"
}

// Add protocol mapper
val mapper = ProtocolMapperRepresentation().apply {
    name = "custom-claim-mapper"
    protocol = "openid-connect"
    protocolMapper = "oidc-usermodel-attribute-mapper"
    config = mapOf(
        "user.attribute" to "customAttribute",
        "claim.name" to "custom_claim",
        "jsonType.label" to "String",
        "id.token.claim" to "true",
        "access.token.claim" to "true",
        "userinfo.token.claim" to "true"
    )
}

scope.protocolMappers = listOf(mapper)

// Create scope
keycloak.realm("my-realm").clientScopes().create(scope)
```

### Service Account Roles

```kotlin
// Get service account user
val serviceAccount = keycloak.realm("my-realm")
    .clients().get(clientUuid)
    .serviceAccountUser

// Get realm management client
val realmManagementClientId = keycloak.realm("my-realm")
    .clients()
    .findByClientId("realm-management")
    .first()
    .id

// Get roles
val viewUsersRole = keycloak.realm("my-realm")
    .clients().get(realmManagementClientId)
    .roles().get("view-users")
    .toRepresentation()

// Assign role to service account
keycloak.realm("my-realm")
    .users().get(serviceAccount.id)
    .roles().clientLevel(realmManagementClientId)
    .add(listOf(viewUsersRole))
```

---

## User Management

### Creating Users

```kotlin
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.CredentialRepresentation

val user = UserRepresentation().apply {
    username = "john.doe"
    email = "john.doe@example.com"
    firstName = "John"
    lastName = "Doe"
    isEnabled = true
    isEmailVerified = true

    // Custom attributes
    attributes = mapOf(
        "phoneNumber" to listOf("+1-555-1234"),
        "department" to listOf("Engineering")
    )
}

// Create user
val response = keycloak.realm("my-realm").users().create(user)
val userId = response.location.path.substringAfterLast("/")

// Set password
val credential = CredentialRepresentation().apply {
    type = CredentialRepresentation.PASSWORD
    value = "SecurePassword123!"
    isTemporary = false
}

keycloak.realm("my-realm").users().get(userId).resetPassword(credential)
```

### Querying Users

```kotlin
// Search users
val users = keycloak.realm("my-realm")
    .users()
    .search("john", 0, 10)  // search term, offset, max results

// Get user by username
val users = keycloak.realm("my-realm")
    .users()
    .search("john.doe", true)  // exact match

// Get user by email
val users = keycloak.realm("my-realm")
    .users()
    .search(null, null, null, "john.doe@example.com", 0, 10)

// Get all users with specific role
val users = keycloak.realm("my-realm")
    .roles().get("admin")
    .roleUserMembers
```

### Updating Users

```kotlin
// Get user
val user = keycloak.realm("my-realm")
    .users()
    .get(userId)
    .toRepresentation().apply {
        // Update fields
        email = "new.email@example.com"
        isEmailVerified = false
        attributes["department"] = listOf("Sales")
    }

// Update user
keycloak.realm("my-realm").users().get(userId).update(user)

// Send verification email
keycloak.realm("my-realm").users().get(userId).sendVerifyEmail()
```

### User Sessions

```kotlin
// Get user sessions
val sessions = keycloak.realm("my-realm")
    .users().get(userId)
    .userSessions

// Logout user (all sessions)
keycloak.realm("my-realm").users().get(userId).logout()
```

---

## Role Management

### Realm Roles

```kotlin
import org.keycloak.representations.idm.RoleRepresentation

// Create realm role
val role = RoleRepresentation().apply {
    name = "premium-user"
    description = "Premium subscription users"
    isComposite = false
}

keycloak.realm("my-realm").roles().create(role)

// Create composite role
val compositeRole = RoleRepresentation().apply {
    name = "admin"
    description = "Administrator role"
    isComposite = true
}

keycloak.realm("my-realm").roles().create(compositeRole)

// Add roles to composite
val manageUsers = keycloak.realm("my-realm")
    .roles().get("manage-users").toRepresentation()
val viewReports = keycloak.realm("my-realm")
    .roles().get("view-reports").toRepresentation()

keycloak.realm("my-realm")
    .roles().get("admin")
    .addComposites(listOf(manageUsers, viewReports))
```

### Client Roles

```kotlin
// Create client role
val clientRole = RoleRepresentation().apply {
    name = "app-admin"
    description = "Application administrator"
}

keycloak.realm("my-realm")
    .clients().get(clientUuid)
    .roles().create(clientRole)

// Assign client role to user
val role = keycloak.realm("my-realm")
    .clients().get(clientUuid)
    .roles().get("app-admin")
    .toRepresentation()

keycloak.realm("my-realm")
    .users().get(userId)
    .roles().clientLevel(clientUuid)
    .add(listOf(role))
```

### Role Mapping in Token

```kotlin
// Add role mapper to client
val mapper = ProtocolMapperRepresentation().apply {
    name = "client-roles"
    protocol = "openid-connect"
    protocolMapper = "oidc-usermodel-client-role-mapper"
    config = mapOf(
        "claim.name" to "resource_access.\${client_id}.roles",
        "jsonType.label" to "String",
        "multivalued" to "true",
        "access.token.claim" to "true",
        "id.token.claim" to "true"
    )
}

keycloak.realm("my-realm")
    .clients().get(clientUuid)
    .protocolMappers
    .createMapper(mapper)
```

---

## Identity Providers

### Google OpenID Connect

```kotlin
import org.keycloak.representations.idm.IdentityProviderRepresentation

val google = IdentityProviderRepresentation().apply {
    alias = "google"
    providerId = "google"
    isEnabled = true
    isTrustEmail = true
    isStoreToken = false
    isLinkOnly = false
    firstBrokerLoginFlowAlias = "first broker login"

    config = mapOf(
        "clientId" to "your-google-client-id.apps.googleusercontent.com",
        "clientSecret" to "your-google-client-secret",
        "hostedDomain" to "example.com",  // Optional: restrict to domain
        "defaultScope" to "openid profile email"
    )
}

keycloak.realm("my-realm").identityProviders().create(google)
```

### Generic OpenID Connect Provider

```kotlin
val oidc = IdentityProviderRepresentation().apply {
    alias = "custom-oidc"
    providerId = "oidc"
    isEnabled = true
    isTrustEmail = false
    isStoreToken = true

    config = mapOf(
        "clientId" to "client-id",
        "clientSecret" to "client-secret",
        "authorizationUrl" to "https://provider.com/oauth/authorize",
        "tokenUrl" to "https://provider.com/oauth/token",
        "userInfoUrl" to "https://provider.com/oauth/userinfo",
        "issuer" to "https://provider.com",
        "defaultScope" to "openid profile email",
        "validateSignature" to "true",
        "useJwksUrl" to "true",
        "jwksUrl" to "https://provider.com/.well-known/jwks.json"
    )
}

keycloak.realm("my-realm").identityProviders().create(oidc)
```

### SAML Identity Provider

```kotlin
val saml = IdentityProviderRepresentation().apply {
    alias = "enterprise-saml"
    providerId = "saml"
    isEnabled = true

    config = mapOf(
        "singleSignOnServiceUrl" to "https://idp.example.com/saml/sso",
        "singleLogoutServiceUrl" to "https://idp.example.com/saml/slo",
        "nameIDPolicyFormat" to "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
        "signingCertificate" to "MIIDdzCCAl+gAwIBAgIE...",  // X509 certificate
        "wantAuthnRequestsSigned" to "true",
        "validateSignature" to "true"
    )
}

keycloak.realm("my-realm").identityProviders().create(saml)
```

---

## Authentication Flows

### Custom Authentication Flow

```kotlin
import org.keycloak.representations.idm.AuthenticationFlowRepresentation
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation

// Create authentication flow
val flow = AuthenticationFlowRepresentation().apply {
    alias = "custom-browser-flow"
    description = "Custom browser authentication flow"
    providerId = "basic-flow"
    isTopLevel = true
    isBuiltIn = false
}

val response = keycloak.realm("my-realm").flows().createFlow(flow)

// Add executions
val execution = AuthenticationExecutionRepresentation().apply {
    authenticator = "auth-cookie"
    requirement = "ALTERNATIVE"
    priority = 10
}

keycloak.realm("my-realm")
    .flows()
    .addExecution("custom-browser-flow", execution)
```

---

## Event Listeners

### Custom Event Listener Configuration

```kotlin
// Update realm to add custom event listener
val realm = keycloak.realm("my-realm").toRepresentation().apply {
    // Add custom listener
    eventsListeners = eventsListeners?.toMutableList()?.apply {
        add("custom-event-listener")
    } ?: mutableListOf("custom-event-listener")

    // Configure event types
    enabledEventTypes = listOf(
        "LOGIN", "LOGIN_ERROR",
        "LOGOUT", "LOGOUT_ERROR",
        "REGISTER", "REGISTER_ERROR",
        "UPDATE_PASSWORD", "UPDATE_PASSWORD_ERROR",
        "SEND_RESET_PASSWORD",
        "UPDATE_PROFILE", "UPDATE_EMAIL"
    )
}

keycloak.realm("my-realm").update(realm)
```

---

## Import/Export

### Export Realm

```bash
# Via command line
./bin/kc.sh export \
  --file /tmp/my-realm-export.json \
  --realm my-realm \
  --users realm_file

# Export all realms
./bin/kc.sh export \
  --dir /tmp/keycloak-export \
  --users realm_file
```

### Import Realm

```bash
# Via command line
./bin/kc.sh import \
  --file /tmp/my-realm-export.json

# Override existing realm
./bin/kc.sh import \
  --file /tmp/my-realm-export.json \
  --override true
```

### Programmatic Export/Import

```kotlin
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.io.path.Path
import kotlin.io.path.readText

// Export realm to JSON
val realm = keycloak.realm("my-realm").toRepresentation()
val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(realm)

// Import from JSON
val json = Path("/tmp/realm.json").readText()
val realm = jacksonObjectMapper().readValue(json, RealmRepresentation::class.java)
keycloak.realms().create(realm)
```

---

**Document Version**: 1.0
**Last Updated**: January 2025
