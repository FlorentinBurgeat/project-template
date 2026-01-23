---
name: keycloak
description: Keycloak identity and access management expertise. Covers Keycloak server setup, deployment modes (standalone, standalone clustered, domain clustered), realm creation and management, Keycloak Admin REST API, client configuration, authentication flows, Infinispan clustering, KeycloakBuilder admin client, RealmRepresentation, SSO integration, OpenID Connect, OAuth2, user federation, and high availability configuration. Use when working with Keycloak server installation, realm administration, identity provider setup, or IAM solutions.
---

# Keycloak Identity & Access Management

## Purpose

Comprehensive guide for deploying, configuring, and managing Keycloak servers and realms across all deployment modes. Provides expertise on Keycloak's identity and access management capabilities.

## When to Use This Skill

Use this skill when:
- Setting up Keycloak server (development or production)
- Choosing between deployment modes (standalone vs clustered)
- Creating or managing Keycloak realms
- Configuring authentication and authorization
- Integrating Keycloak with applications (SSO, OAuth2, OIDC)
- Working with Keycloak Admin REST API
- Managing clients, users, and roles
- Configuring high availability and clustering
- Troubleshooting Keycloak deployments

---

## Quick Start: Deployment Mode Selection

### Choose Your Deployment Mode

**Standalone Mode** - Single Keycloak instance
- **Use For**: Development, testing, small deployments
- **Pros**: Simple setup, minimal resources
- **Cons**: No high availability, single point of failure
- **When**: Local development, proof-of-concept, < 100 users

**Standalone Clustered Mode** - Multiple instances with Infinispan
- **Use For**: Production deployments requiring HA
- **Pros**: High availability, load balancing, session replication
- **Cons**: More complex configuration, requires load balancer
- **When**: Production systems, > 100 users, uptime critical

**Domain Clustered Mode** - WildFly domain with host controllers
- **Use For**: Enterprise deployments with centralized management
- **Pros**: Centralized configuration, multiple server groups, advanced management
- **Cons**: Most complex setup, requires WildFly expertise
- **When**: Large enterprises, multiple environments, centralized control

See [DEPLOYMENT_MODES.md](DEPLOYMENT_MODES.md) for detailed setup instructions.

---

## Realm Management Essentials

### What is a Realm?

A realm manages a set of users, credentials, roles, and groups. Each application typically has its own realm.

### Creating a Realm via Admin Console

1. Log in to Keycloak Admin Console (`http://localhost:8080/admin`)
2. Click dropdown in top-left corner (shows "master")
3. Click "Create Realm"
4. Enter realm name and click "Create"

### Creating a Realm via REST API

```bash
# Get admin access token
ACCESS_TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

# Create new realm
curl -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "my-app-realm",
    "enabled": true,
    "displayName": "My Application Realm",
    "loginTheme": "keycloak",
    "registrationAllowed": true,
    "resetPasswordAllowed": true
  }'
```

### Creating a Realm via Kotlin Admin Client

```kotlin
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.RealmRepresentation

// Initialize admin client
val keycloak = KeycloakBuilder.builder()
    .serverUrl("http://localhost:8080")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("admin")
    .build()

// Create realm representation
val realm = RealmRepresentation().apply {
    realm = "my-app-realm"
    isEnabled = true
    displayName = "My Application Realm"
    isRegistrationAllowed = true
    isResetPasswordAllowed = true
}

// Create the realm
keycloak.realms().create(realm)
```

See [REALM_MANAGEMENT.md](REALM_MANAGEMENT.md) for comprehensive realm configuration options.

---

## Keycloak Admin Client Setup

### Maven Dependency

```xml
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>26.4.0</version>
</dependency>
```

### Gradle Dependency (Kotlin DSL)

```kotlin
dependencies {
    implementation("org.keycloak:keycloak-admin-client:26.4.0")
}
```

### Basic Configuration

```kotlin
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder

// Create admin client instance
val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-cli")
    .username("admin")
    .password("secure-password")
    .build()

// Access realm resource
val realmResource = keycloak.realm("my-app-realm")

// Access users resource
val usersResource = realmResource.users()

// Access clients resource
val clientsResource = realmResource.clients()
```

### Using Client Credentials

```kotlin
import org.keycloak.OAuth2Constants

val keycloak = KeycloakBuilder.builder()
    .serverUrl("https://keycloak.example.com")
    .realm("master")
    .clientId("admin-client")
    .clientSecret("client-secret-here")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
    .build()
```

See [CLIENT_SETUP.md](CLIENT_SETUP.md) for advanced client configuration.

---

## Common Keycloak Operations

### 1. List All Realms

```bash
curl -X GET http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

```kotlin
val realms = keycloak.realms().findAll()
realms.forEach { realm ->
    println(realm.realm)
}
```

### 2. Get Realm Details

```bash
curl -X GET http://localhost:8080/admin/realms/my-app-realm \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

```kotlin
val realm = keycloak.realm("my-app-realm").toRepresentation()
println("Realm: ${realm.realm}")
println("Enabled: ${realm.isEnabled}")
```

### 3. Update Realm Settings

```bash
curl -X PUT http://localhost:8080/admin/realms/my-app-realm \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "my-app-realm",
    "enabled": true,
    "loginTheme": "custom-theme",
    "accessTokenLifespan": 900
  }'
```

```kotlin
val realm = keycloak.realm("my-app-realm").toRepresentation().apply {
    accessTokenLifespan = 900 // 15 minutes
    loginTheme = "custom-theme"
}
keycloak.realm("my-app-realm").update(realm)
```

### 4. Create a Client

```kotlin
import org.keycloak.representations.idm.ClientRepresentation

val client = ClientRepresentation().apply {
    clientId = "my-application"
    isEnabled = true
    isPublicClient = false
    secret = "client-secret"
    redirectUris = listOf("http://localhost:3000/*")
    webOrigins = listOf("http://localhost:3000")
    protocol = "openid-connect"
}

val response = keycloak.realm("my-app-realm").clients().create(client)
val clientUuid = response.location.path.substringAfterLast("/")
```

### 5. Create a User

```kotlin
import org.keycloak.representations.idm.UserRepresentation
import org.keycloak.representations.idm.CredentialRepresentation

val user = UserRepresentation().apply {
    username = "john.doe"
    email = "john.doe@example.com"
    firstName = "John"
    lastName = "Doe"
    isEnabled = true
}

// Create user
val response = keycloak.realm("my-app-realm").users().create(user)

// Set password
val userId = response.location.path.substringAfterLast("/")
val credential = CredentialRepresentation().apply {
    type = CredentialRepresentation.PASSWORD
    value = "secure-password"
    isTemporary = false
}

keycloak.realm("my-app-realm").users().get(userId).resetPassword(credential)
```

---

## Clustering Configuration

### Standalone Clustered Mode Overview

Keycloak uses **Infinispan** for distributed caching and session replication across cluster nodes.

**Key Components:**
- **Infinispan**: Distributed cache for sessions, realms, users
- **JGroups**: Cluster communication protocol
- **Load Balancer**: Distributes traffic across nodes

### Basic Clustering Setup

**1. Enable clustering in standalone-ha.xml:**

```xml
<subsystem xmlns="urn:jboss:domain:infinispan:14.0">
    <cache-container name="keycloak">
        <transport lock-timeout="60000"/>
        <distributed-cache name="sessions" owners="2"/>
        <distributed-cache name="authenticationSessions" owners="2"/>
        <distributed-cache name="offlineSessions" owners="2"/>
        <distributed-cache name="clientSessions" owners="2"/>
    </cache-container>
</subsystem>
```

**2. Configure JGroups:**

```xml
<subsystem xmlns="urn:jboss:domain:jgroups:9.0">
    <channels default="ee">
        <channel name="ee" stack="tcp"/>
    </channels>
    <stacks>
        <stack name="tcp">
            <transport type="TCP" socket-binding="jgroups-tcp"/>
            <protocol type="TCPPING">
                <property name="initial_hosts">node1[7600],node2[7600]</property>
                <property name="port_range">0</property>
            </protocol>
        </stack>
    </stacks>
</subsystem>
```

**3. Start nodes:**

```bash
# Node 1
./bin/standalone.sh --server-config=standalone-ha.xml \
  -Djboss.node.name=node1 \
  -Djboss.socket.binding.port-offset=0

# Node 2
./bin/standalone.sh --server-config=standalone-ha.xml \
  -Djboss.node.name=node2 \
  -Djboss.socket.binding.port-offset=100
```

See [DEPLOYMENT_MODES.md](DEPLOYMENT_MODES.md) for complete clustering configuration.

---

## Best Practices

### Security

✅ **Use HTTPS in production** - Always configure SSL/TLS certificates
✅ **Strong admin credentials** - Use complex passwords, rotate regularly
✅ **Client secrets** - Generate strong, unique secrets for confidential clients
✅ **Token lifespans** - Configure appropriate access/refresh token lifetimes
✅ **CORS configuration** - Restrict web origins to trusted domains
✅ **Database security** - Use encrypted connections to database

### Performance

✅ **Database connection pooling** - Configure adequate pool sizes
✅ **Cache configuration** - Tune Infinispan cache sizes for your workload
✅ **Session management** - Configure appropriate session timeouts
✅ **Theme caching** - Enable theme caching in production
✅ **Database indexes** - Ensure proper indexing for user lookups

### High Availability

✅ **Multiple nodes** - Run at least 2 nodes in production
✅ **Load balancer** - Use sticky sessions for consistent routing
✅ **Database HA** - Use clustered database (PostgreSQL HA, MySQL Galera)
✅ **Health checks** - Configure load balancer health checks
✅ **Monitoring** - Monitor cluster status, cache hit rates, DB connections

### Realm Design

✅ **One realm per application** - Isolate applications in separate realms
✅ **Consistent naming** - Use clear, consistent realm and client names
✅ **Theme customization** - Create custom themes for branding
✅ **Role mapping** - Use composite roles for complex permissions
✅ **User federation** - Integrate with LDAP/AD for centralized user management

---

## Troubleshooting

### Common Issues

**1. Cluster nodes not discovering each other**
- Check JGroups configuration (TCPPING hosts)
- Verify network connectivity between nodes
- Check firewall rules (port 7600 for JGroups)
- Review logs: `standalone/log/server.log`

**2. Sessions not replicating**
- Verify Infinispan cache configuration
- Check cluster formation in logs
- Ensure `owners="2"` or higher for distributed caches
- Test with sticky sessions disabled

**3. Admin API authentication fails**
- Verify admin credentials
- Check realm name (usually "master" for admin)
- Ensure client_id is correct ("admin-cli")
- Check token expiration

**4. Database connection errors**
- Verify database credentials in standalone.xml
- Check database connectivity from Keycloak host
- Review datasource configuration
- Check database logs for connection issues

**5. Theme not applying**
- Clear browser cache
- Verify theme files in `themes/` directory
- Check theme name in realm settings
- Restart Keycloak to reload themes

---

## Reference Files

For detailed information, see:

### [DEPLOYMENT_MODES.md](DEPLOYMENT_MODES.md)
Complete deployment configuration guide:
- Standalone mode setup (development)
- Standalone clustered configuration (HA)
- Domain clustered setup (enterprise)
- Infinispan clustering details
- JGroups network configuration
- Load balancer setup
- Docker and Kubernetes deployments

### [REALM_MANAGEMENT.md](REALM_MANAGEMENT.md)
Comprehensive realm administration:
- RealmRepresentation complete reference
- All realm configuration options
- Client configuration and protocols
- User and group management
- Role and permission management
- Identity provider configuration
- Authentication flows
- Event listeners and logging

### [CLIENT_SETUP.md](CLIENT_SETUP.md)
Keycloak Admin Client detailed guide:
- KeycloakBuilder configuration options
- SSL/TLS configuration
- Authentication methods (password, client credentials, token)
- Resource APIs (realms, users, clients, roles)
- Error handling patterns
- Connection pooling and performance
- Testing strategies

---

## Quick Command Reference

### Get Access Token
```bash
curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" \
  -d "grant_type=password"
```

### Create Realm
```bash
curl -X POST http://localhost:8080/admin/realms \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"realm":"my-realm","enabled":true}'
```

### List Users
```bash
curl -X GET http://localhost:8080/admin/realms/my-realm/users \
  -H "Authorization: Bearer $TOKEN"
```

### Export Realm Configuration
```bash
./bin/standalone.sh -Dkeycloak.migration.action=export \
  -Dkeycloak.migration.provider=singleFile \
  -Dkeycloak.migration.file=/tmp/realm-export.json \
  -Dkeycloak.migration.realmName=my-realm
```

### Import Realm Configuration
```bash
./bin/standalone.sh -Dkeycloak.migration.action=import \
  -Dkeycloak.migration.provider=singleFile \
  -Dkeycloak.migration.file=/tmp/realm-export.json
```

---

## Next Steps

1. **Choose deployment mode** based on your requirements
2. **Install Keycloak** following [DEPLOYMENT_MODES.md](DEPLOYMENT_MODES.md)
3. **Create your first realm** using Admin Console or API
4. **Configure a client** for your application
5. **Set up users and roles** for authentication
6. **Integrate with your application** using OpenID Connect or SAML
7. **Test authentication flows** thoroughly
8. **Configure clustering** for production (if needed)
9. **Monitor and tune** performance

---

**Skill Version**: 1.0
**Last Updated**: January 2025
**Line Count**: < 500 ✅
**Keycloak Version**: 26.4.0
