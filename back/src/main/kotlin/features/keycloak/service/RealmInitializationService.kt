package features.keycloak.service

import jakarta.annotation.PostConstruct
import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.Response

/**
 * Realm Initialization Service
 *
 * Automatically creates and configures the Keycloak realm on application startup if it doesn't exist.
 * This allows each project fork to have its own isolated realm on a shared Keycloak instance.
 *
 * Key responsibilities:
 * - Create realm if missing
 * - Configure clients (frontend SPA, backend service)
 * - Set up authentication flows
 * - Configure token lifespans
 * - Set up role mappings
 * - Enable user registration
 */
@Service
class RealmInitializationService(
    private val keycloak: Keycloak,
    private val keycloakProperties: features.keycloak.config.KeycloakProperties
) {

    private val realmName: String get() = keycloakProperties.realm.name
    private val frontendClientId: String get() = keycloakProperties.frontend.clientId
    private val backendClientId: String get() = keycloakProperties.backend.clientId
    private val backendClientSecret: String get() = keycloakProperties.backend.clientSecret
    private val frontendRedirectUris: List<String> get() = keycloakProperties.frontend.redirectUris
    private val frontendWebOrigins: List<String> get() = keycloakProperties.frontend.webOrigins

    private val logger = LoggerFactory.getLogger(RealmInitializationService::class.java)

    /**
     * Initializes the realm on application startup.
     * Creates the realm and configures all necessary components if it doesn't exist.
     */
    @PostConstruct
    fun initializeRealm() {
        logger.info("Starting realm initialization for realm: $realmName")

        try {
            if (realmExists()) {
                logger.info("Realm '$realmName' already exists, skipping initialization")
                return
            }

            logger.info("Realm '$realmName' does not exist, creating...")
            createRealm()
            configureClients()
            logger.info("Realm '$realmName' initialized successfully")

        } catch (e: Exception) {
            logger.error("Failed to initialize realm '$realmName'", e)
            throw RuntimeException("Realm initialization failed", e)
        }
    }

    /**
     * Checks if the realm already exists.
     */
    private fun realmExists(): Boolean {
        return try {
            keycloak.realm(realmName).toRepresentation()
            true
        } catch (e: NotFoundException) {
            false
        }
    }

    /**
     * Creates the realm with base configuration.
     */
    private fun createRealm() {
        val realm = RealmRepresentation().apply {
            realm = realmName
            isEnabled = true
            displayName = realmName.split("-").joinToString(" ") { it.capitalize() }

            // User registration and management
            isRegistrationAllowed = true
            isRegistrationEmailAsUsername = true
            isVerifyEmail = false // Set to true in production with SMTP configured
            isResetPasswordAllowed = true
            isEditUsernameAllowed = false

            // Login settings
            isLoginWithEmailAllowed = true
            isDuplicateEmailsAllowed = false

            // Token settings (align with previous JWT config)
            accessTokenLifespan = 900 // 15 minutes
            ssoSessionIdleTimeout = 1800 // 30 minutes
            ssoSessionMaxLifespan = 36000 // 10 hours
            accessTokenLifespanForImplicitFlow = 900

            // Refresh token settings
            refreshTokenMaxReuse = 0
            revokeRefreshToken = false

            // Themes
            loginTheme = "keycloak"
            accountTheme = "keycloak"
            adminTheme = "keycloak"
            emailTheme = "keycloak"

            // Security settings
            isBruteForceProtected = true
            failureFactor = 5
            waitIncrementSeconds = 60
            maxFailureWaitSeconds = 900
            maxDeltaTimeSeconds = 43200
        }

        keycloak.realms().create(realm)
        logger.info("Realm '$realmName' created successfully")
    }

    /**
     * Configures clients for the realm.
     * Creates:
     * 1. Frontend client (public SPA client)
     * 2. Backend service client (confidential client with client credentials)
     */
    private fun configureClients() {
        createFrontendClient()
        createBackendServiceClient()
    }

    /**
     * Creates the frontend SPA client (public client for browsers).
     */
    private fun createFrontendClient() {
        val client = ClientRepresentation().apply {
            clientId = frontendClientId
            isEnabled = true
            isPublicClient = true // Public client for SPA
            isStandardFlowEnabled = true // Authorization Code Flow with PKCE
            isDirectAccessGrantsEnabled = false // Disable Resource Owner Password Credentials
            isImplicitFlowEnabled = false // Disable implicit flow (deprecated)

            // Redirect URIs and web origins
            redirectUris = frontendRedirectUris.toMutableList()
            webOrigins = frontendWebOrigins.toMutableList()

            // Root URL and base URL
            rootUrl = frontendWebOrigins.firstOrNull() ?: ""
            baseUrl = "/"

            protocol = "openid-connect"

            // Protocol mappers for user info
            protocolMappers = createStandardProtocolMappers()
        }

        createClient(client, "Frontend SPA")
    }

    /**
     * Creates the backend service client (confidential client).
     * Used for service-to-service communication and admin operations.
     */
    private fun createBackendServiceClient() {
        val client = ClientRepresentation().apply {
            clientId = backendClientId
            isEnabled = true
            isPublicClient = false // Confidential client
            secret = backendClientSecret

            // Enable service account for client credentials grant
            isServiceAccountsEnabled = true
            isStandardFlowEnabled = true
            isDirectAccessGrantsEnabled = false

            protocol = "openid-connect"

            // No redirect URIs needed for service account
            redirectUris = mutableListOf()
            webOrigins = mutableListOf()
        }

        createClient(client, "Backend Service")
    }

    /**
     * Creates a client in the realm.
     */
    private fun createClient(client: ClientRepresentation, clientName: String) {
        val response: Response = keycloak.realm(realmName).clients().create(client)

        if (response.status == 201) {
            logger.info("$clientName client '${client.clientId}' created successfully")
        } else {
            logger.error("Failed to create $clientName client '${client.clientId}': ${response.statusInfo}")
        }

        response.close()
    }

    /**
     * Creates standard OIDC protocol mappers for user information.
     */
    private fun createStandardProtocolMappers(): MutableList<ProtocolMapperRepresentation> {
        return mutableListOf(
            // User ID mapper
            ProtocolMapperRepresentation().apply {
                name = "user-id"
                protocol = "openid-connect"
                protocolMapper = "oidc-usermodel-attribute-mapper"
                config = mutableMapOf(
                    "user.attribute" to "id",
                    "claim.name" to "sub",
                    "jsonType.label" to "String",
                    "id.token.claim" to "true",
                    "access.token.claim" to "true",
                    "userinfo.token.claim" to "true"
                )
            },
            // Email mapper
            ProtocolMapperRepresentation().apply {
                name = "email"
                protocol = "openid-connect"
                protocolMapper = "oidc-usermodel-property-mapper"
                config = mutableMapOf(
                    "user.attribute" to "email",
                    "claim.name" to "email",
                    "jsonType.label" to "String",
                    "id.token.claim" to "true",
                    "access.token.claim" to "true",
                    "userinfo.token.claim" to "true"
                )
            },
            // Full name mapper
            ProtocolMapperRepresentation().apply {
                name = "full-name"
                protocol = "openid-connect"
                protocolMapper = "oidc-full-name-mapper"
                config = mutableMapOf(
                    "id.token.claim" to "true",
                    "access.token.claim" to "true",
                    "userinfo.token.claim" to "true"
                )
            }
        )
    }
}
