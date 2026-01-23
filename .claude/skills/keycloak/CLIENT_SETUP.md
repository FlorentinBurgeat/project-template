# Keycloak Admin Client Setup (Kotlin)

Complete guide to using the Keycloak Admin Client in Kotlin applications.

## Table of Contents

- [Dependencies](#dependencies)
- [KeycloakBuilder Configuration](#keycloakbuilder-configuration)
- [Authentication Methods](#authentication-methods)
- [Resource APIs](#resource-apis)
- [Error Handling](#error-handling)
- [Connection Pooling](#connection-pooling)
- [Testing Strategies](#testing-strategies)
- [Spring Boot Integration](#spring-boot-integration)

---

## Dependencies

###

 Maven (pom.xml)

```xml
<dependencies>
    <!-- Keycloak Admin Client -->
    <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-admin-client</artifactId>
        <version>26.4.0</version>
    </dependency>

    <!-- Required dependencies -->
    <dependency>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>resteasy-client</artifactId>
        <version>6.2.10.Final</version>
    </dependency>

    <dependency>
        <groupId>org.jboss.resteasy</groupId>
        <artifactId>resteasy-jackson2-provider</artifactId>
        <version>6.2.10.Final</version>
    </dependency>
</dependencies>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    // Keycloak Admin Client
    implementation("org.keycloak:keycloak-admin-client:26.4.0")

    // Required dependencies
    implementation("org.jboss.resteasy:resteasy-client:6.2.10.Final")
    implementation("org.jboss.resteasy:resteasy-jackson2-provider:6.2.10.Final")
}
```

---

## KeycloakBuilder Configuration

### Basic Configuration

```kotlin
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder

val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("admin-password")
    .build()
```

### With All Options

```kotlin
import org.keycloak.OAuth2Constants
import javax.net.ssl.SSLContext

val keycloak = KeycloakBuilder.builder()
    // Server configuration
    .serverUrl("https://keycloak.example.com")
    .realm("master")

    // Authentication
    .clientId("admin-client")
    .clientSecret("client-secret")
    .username("admin")
    .password("admin-password")
    .grantType(OAuth2Constants.PASSWORD)

    // SSL/TLS configuration
    .sslContext(sslContext)
    .hostnameVerification(true)

    // Connection pool
    .connectionPoolSize(10)

    // Timeouts
    .socketTimeout(60000)  // 60 seconds
    .connectTimeout(10000) // 10 seconds

    // Proxy
    .proxy("proxy.example.com:8080")

    .build()
```

### Configuration as Data Class

```kotlin
data class KeycloakConfig(
    val serverUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String? = null,
    val username: String? = null,
    val password: String? = null,
    val grantType: String = OAuth2Constants.PASSWORD,
    val connectionPoolSize: Int = 10,
    val socketTimeout: Int = 60000,
    val connectTimeout: Int = 10000
)

fun createKeycloakClient(config: KeycloakConfig): Keycloak {
    return KeycloakBuilder.builder()
        .serverUrl(config.serverUrl)
        .realm(config.realm)
        .clientId(config.clientId)
        .apply {
            config.clientSecret?.let { clientSecret(it) }
            config.username?.let { username(it) }
            config.password?.let { password(it) }
        }
        .grantType(config.grantType)
        .connectionPoolSize(config.connectionPoolSize)
        .socketTimeout(config.socketTimeout)
        .connectTimeout(config.connectTimeout)
        .build()
}

// Usage
val config = KeycloakConfig(
    serverUrl = "https://keycloak.example.com",
    realm = "master",
    clientId = "admin-client",
    clientSecret = "secret",
    grantType = OAuth2Constants.CLIENT_CREDENTIALS
)

val keycloak = createKeycloakClient(config)
```

---

## Authentication Methods

### 1. Password Grant (User Credentials)

```kotlin
val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("admin-password")
    .grantType(OAuth2Constants.PASSWORD)
    .build()
```

**Use When**: Direct admin user access, development/testing

### 2. Client Credentials Grant (Service Account)

```kotlin
val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-service")
    .clientSecret("service-secret")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
    .build()
```

**Use When**: Service-to-service communication, production environments

### 3. Existing Access Token

```kotlin
val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .authorization("Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    .build()
```

**Use When**: You already have a valid access token

### 4. Token Refresh

```kotlin
// Keycloak client handles token refresh automatically
// Access the token manager for manual control
val tokenManager = keycloak.tokenManager()

// Get current access token
val accessToken = tokenManager.accessTokenString

// Force token refresh
tokenManager.refreshToken()

// Check if token needs refresh
if (tokenManager.isTokenTimeValid()) {
    // Token is valid
} else {
    // Token expired or will expire soon
    tokenManager.refreshToken()
}
```

---

## Resource APIs

### Realms API

```kotlin
// List all realms
val realms = keycloak.realms().findAll()
realms.forEach { realm ->
    println("Realm: ${realm.realm}, Enabled: ${realm.isEnabled}")
}

// Get specific realm
val realm = keycloak.realm("my-realm").toRepresentation()

// Update realm
keycloak.realm("my-realm").update(realm.apply {
    accessTokenLifespan = 900
})

// Delete realm
keycloak.realm("my-realm").remove()
```

### Users API

```kotlin
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.CredentialRepresentation

// Create user
val user = UserRepresentation().apply {
    username = "john.doe"
    email = "john.doe@example.com"
    firstName = "John"
    lastName = "Doe"
    isEnabled = true
    isEmailVerified = true
}

val response = keycloak.realm("my-realm").users().create(user)
val userId = response.location.path.substringAfterLast("/")

// Set password
val credential = CredentialRepresentation().apply {
    type = CredentialRepresentation.PASSWORD
    value = "SecurePassword123!"
    isTemporary = false
}
keycloak.realm("my-realm").users().get(userId).resetPassword(credential)

// Search users
val users = keycloak.realm("my-realm").users().search("john", 0, 10)

// Get user by ID
val user = keycloak.realm("my-realm").users().get(userId).toRepresentation()

// Update user
keycloak.realm("my-realm").users().get(userId).update(user.apply {
    email = "new.email@example.com"
})

// Delete user
keycloak.realm("my-realm").users().get(userId).remove()

// Send verification email
keycloak.realm("my-realm").users().get(userId).sendVerifyEmail()

// Execute actions email (reset password, verify email, etc.)
keycloak.realm("my-realm").users().get(userId)
    .executeActionsEmail(listOf("UPDATE_PASSWORD", "VERIFY_EMAIL"))

// Get user sessions
val sessions = keycloak.realm("my-realm").users().get(userId).userSessions

// Logout user
keycloak.realm("my-realm").users().get(userId).logout()
```

### Clients API

```kotlin
import org.keycloak.representations.idm.ClientRepresentation

// Create client
val client = ClientRepresentation().apply {
    clientId = "my-app"
    isEnabled = true
    isPublicClient = false
    secret = "client-secret"
    redirectUris = listOf("https://app.example.com/*")
    webOrigins = listOf("https://app.example.com")
    protocol = "openid-connect"
}

val response = keycloak.realm("my-realm").clients().create(client)
val clientUuid = response.location.path.substringAfterLast("/")

// Get client by UUID
val client = keycloak.realm("my-realm").clients().get(clientUuid).toRepresentation()

// Find client by client ID
val clients = keycloak.realm("my-realm").clients().findByClientId("my-app")
val clientUuid = clients.firstOrNull()?.id

// Update client
keycloak.realm("my-realm").clients().get(clientUuid).update(client.apply {
    isEnabled = false
})

// Get client secret
val credentials = keycloak.realm("my-realm").clients().get(clientUuid).secret
println("Client secret: ${credentials.value}")

// Regenerate client secret
val newCredentials = keycloak.realm("my-realm").clients().get(clientUuid).generateNewSecret()

// Get service account user
val serviceAccountUser = keycloak.realm("my-realm").clients().get(clientUuid).serviceAccountUser

// Delete client
keycloak.realm("my-realm").clients().get(clientUuid).remove()
```

### Roles API

```kotlin
import org.keycloak.representations.idm.RoleRepresentation

// Create realm role
val role = RoleRepresentation().apply {
    name = "premium-user"
    description = "Premium subscription users"
}
keycloak.realm("my-realm").roles().create(role)

// Get realm role
val role = keycloak.realm("my-realm").roles().get("premium-user").toRepresentation()

// List all realm roles
val roles = keycloak.realm("my-realm").roles().list()

// Create client role
val clientRole = RoleRepresentation().apply {
    name = "app-admin"
    description = "Application administrator"
}
keycloak.realm("my-realm").clients().get(clientUuid).roles().create(clientRole)

// Get client role
val clientRole = keycloak.realm("my-realm").clients().get(clientUuid)
    .roles().get("app-admin").toRepresentation()

// Assign realm role to user
val userResource = keycloak.realm("my-realm").users().get(userId)
val roleToAdd = keycloak.realm("my-realm").roles().get("premium-user").toRepresentation()
userResource.roles().realmLevel().add(listOf(roleToAdd))

// Assign client role to user
val clientRoleToAdd = keycloak.realm("my-realm").clients().get(clientUuid)
    .roles().get("app-admin").toRepresentation()
userResource.roles().clientLevel(clientUuid).add(listOf(clientRoleToAdd))

// Get user's realm roles
val userRealmRoles = userResource.roles().realmLevel().listAll()

// Get user's client roles
val userClientRoles = userResource.roles().clientLevel(clientUuid).listAll()

// Remove role from user
userResource.roles().realmLevel().remove(listOf(roleToAdd))
```

### Groups API

```kotlin
import org.keycloak.representations.idm.GroupRepresentation

// Create group
val group = GroupRepresentation().apply {
    name = "administrators"
}
val response = keycloak.realm("my-realm").groups().add(group)
val groupId = response.location.path.substringAfterLast("/")

// List all groups
val groups = keycloak.realm("my-realm").groups().groups()

// Get group by ID
val group = keycloak.realm("my-realm").groups().group(groupId).toRepresentation()

// Update group
keycloak.realm("my-realm").groups().group(groupId).update(group.apply {
    name = "system-administrators"
})

// Add user to group
keycloak.realm("my-realm").users().get(userId).joinGroup(groupId)

// Remove user from group
keycloak.realm("my-realm").users().get(userId).leaveGroup(groupId)

// Get group members
val members = keycloak.realm("my-realm").groups().group(groupId).members()

// Delete group
keycloak.realm("my-realm").groups().group(groupId).remove()
```

---

## Error Handling

### Exception Types

```kotlin
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.core.Response

// Comprehensive error handling
fun createUser(keycloak: Keycloak, realmName: String, user: UserRepresentation): String? {
    return try {
        val response = keycloak.realm(realmName).users().create(user)

        when (response.status) {
            201 -> {
                // Success - extract user ID
                response.location.path.substringAfterLast("/")
            }
            409 -> {
                // Conflict - user already exists
                println("User already exists: ${user.username}")
                null
            }
            else -> {
                println("Unexpected response: ${response.status}")
                null
            }
        }
    } catch (e: NotFoundException) {
        println("Realm not found: $realmName")
        null
    } catch (e: BadRequestException) {
        println("Invalid user data: ${e.message}")
        null
    } catch (e: NotAuthorizedException) {
        println("Authentication failed: ${e.message}")
        null
    } catch (e: ForbiddenException) {
        println("Insufficient permissions: ${e.message}")
        null
    } catch (e: Exception) {
        println("Unexpected error: ${e.message}")
        e.printStackTrace()
        null
    }
}
```

### Retry Logic

```kotlin
import kotlinx.coroutines.delay

suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            println("Attempt failed, retrying in ${currentDelay}ms...")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // Last attempt
}

// Usage
suspend fun getUserSafely(keycloak: Keycloak, realmName: String, userId: String): UserRepresentation? {
    return retryWithBackoff {
        keycloak.realm(realmName).users().get(userId).toRepresentation()
    }
}
```

---

## Connection Pooling

### Configure Connection Pool

```kotlin
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

val connectionManager = PoolingHttpClientConnectionManager().apply {
    maxTotal = 100
    defaultMaxPerRoute = 20
}

val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-client")
    .clientSecret("secret")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
    .connectionPoolSize(100)
    .build()
```

### Close Client Properly

```kotlin
// Always close the client when done
try {
    val realms = keycloak.realms().findAll()
    // ... process realms
} finally {
    keycloak.close()
}

// Or use Kotlin's use function
KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("password")
    .build().use { keycloak ->
        val realms = keycloak.realms().findAll()
        // ... process realms
    } // Automatically closed
```

---

## Testing Strategies

### Mock Keycloak for Testing

```kotlin
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KeycloakServiceTest {

    @Test
    fun `test create user`() {
        // Mock Keycloak client
        val keycloak = mockk<Keycloak>()
        val realmsResource = mockk<RealmsResource>()
        val realmResource = mockk<RealmResource>()
        val usersResource = mockk<UsersResource>()
        val response = mockk<Response>()

        every { keycloak.realm("test-realm") } returns realmResource
        every { realmResource.users() } returns usersResource
        every { usersResource.create(any()) } returns response
        every { response.status } returns 201
        every { response.location.path } returns "/users/user-id-123"

        // Test your service
        val service = KeycloakService(keycloak)
        val userId = service.createUser("test-realm", UserRepresentation())

        assertEquals("user-id-123", userId)
    }
}
```

### Testcontainers Integration

```kotlin
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class KeycloakIntegrationTest {

    companion object {
        @Container
        val keycloakContainer = GenericContainer<Nothing>("quay.io/keycloak/keycloak:26.4.0").apply {
            withExposedPorts(8080)
            withEnv("KEYCLOAK_ADMIN", "admin")
            withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            withCommand("start-dev")
        }
    }

    @Test
    fun `test real Keycloak instance`() {
        val keycloak = KeycloakBuilder.builder()
            .serverUrl("http://localhost:${keycloakContainer.firstMappedPort}")
            .realm("master")
            .clientId("admin-cli")
            .username("admin")
            .password("admin")
            .build()

        val realms = keycloak.realms().findAll()
        assert(realms.isNotEmpty())
    }
}
```

---

## Spring Boot Integration

### Configuration

```kotlin
import org.keycloak.admin.client.Keycloak
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "keycloak.admin")
data class KeycloakAdminProperties(
    var serverUrl: String = "",
    var realm: String = "master",
    var clientId: String = "",
    var clientSecret: String = "",
    var username: String? = null,
    var password: String? = null
)

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties::class)
class KeycloakAdminConfig {

    @Bean
    fun keycloakAdmin(properties: KeycloakAdminProperties): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(properties.serverUrl)
            .realm(properties.realm)
            .clientId(properties.clientId)
            .apply {
                if (properties.clientSecret.isNotEmpty()) {
                    clientSecret(properties.clientSecret)
                    grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                } else if (properties.username != null && properties.password != null) {
                    username(properties.username)
                    password(properties.password)
                    grantType(OAuth2Constants.PASSWORD)
                }
            }
            .build()
    }
}
```

### application.yml

```yaml
keycloak:
  admin:
    server-url: https://keycloak.example.com
    realm: master
    client-id: admin-service
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
```

### Service Layer

```kotlin
import org.springframework.stereotype.Service

@Service
class KeycloakUserService(
    private val keycloak: Keycloak
) {

    fun createUser(realmName: String, username: String, email: String, password: String): String {
        val user = UserRepresentation().apply {
            this.username = username
            this.email = email
            isEnabled = true
            isEmailVerified = true
        }

        val response = keycloak.realm(realmName).users().create(user)
        require(response.status == 201) { "Failed to create user" }

        val userId = response.location.path.substringAfterLast("/")

        // Set password
        val credential = CredentialRepresentation().apply {
            type = CredentialRepresentation.PASSWORD
            value = password
            isTemporary = false
        }
        keycloak.realm(realmName).users().get(userId).resetPassword(credential)

        return userId
    }

    fun assignRole(realmName: String, userId: String, roleName: String) {
        val role = keycloak.realm(realmName).roles().get(roleName).toRepresentation()
        keycloak.realm(realmName).users().get(userId).roles().realmLevel().add(listOf(role))
    }

    fun deleteUser(realmName: String, userId: String) {
        keycloak.realm(realmName).users().get(userId).remove()
    }
}
```

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Language**: Kotlin
