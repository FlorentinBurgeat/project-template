package features.keycloak.config

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import jakarta.annotation.PreDestroy

/**
 * Keycloak Admin Client Configuration
 *
 * Provides a configured Keycloak admin client bean for managing realms, clients, users, and roles.
 * Uses the master realm for admin operations.
 *
 * The client connects to Keycloak using admin credentials and provides access to the Admin REST API.
 */
@Configuration
class KeycloakConfig(
    private val keycloakProperties: KeycloakProperties
) {

    private val logger = LoggerFactory.getLogger(KeycloakConfig::class.java)
    private var keycloakInstance: Keycloak? = null

    /**
     * Creates and configures the Keycloak admin client bean.
     *
     * The client uses password grant type to authenticate with the master realm's admin user.
     * Connection pooling is enabled for performance.
     *
     * @return Configured Keycloak admin client instance
     */
    @Bean
    fun keycloakAdmin(): Keycloak {
        logger.info("Initializing Keycloak admin client for URL: ${keycloakProperties.url}")

        keycloakInstance = KeycloakBuilder.builder()
            .serverUrl(keycloakProperties.url)
            .realm(keycloakProperties.admin.realm)
            .grantType(OAuth2Constants.PASSWORD)
            .clientId(keycloakProperties.admin.clientId)
            .username(keycloakProperties.admin.username)
            .password(keycloakProperties.admin.password)
            .build()

        logger.info("Keycloak admin client initialized successfully")
        return keycloakInstance!!
    }

    /**
     * Closes the Keycloak admin client when the application context is destroyed.
     * This ensures proper cleanup of HTTP connections.
     */
    @PreDestroy
    fun cleanup() {
        logger.info("Closing Keycloak admin client")
        keycloakInstance?.close()
    }
}
