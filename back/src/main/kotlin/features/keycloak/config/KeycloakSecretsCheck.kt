package features.keycloak.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Guards against shipping the template's placeholder Keycloak credentials.
 *
 * With the `prod` profile active, startup fails; otherwise a warning is logged.
 */
@Component
class KeycloakSecretsCheck(
    private val keycloakProperties: KeycloakProperties,
    private val environment: Environment
) {

    private val logger = LoggerFactory.getLogger(KeycloakSecretsCheck::class.java)

    companion object {
        const val PLACEHOLDER_CLIENT_SECRET = "change-this-secret-in-production"
        const val PLACEHOLDER_ADMIN_PASSWORD = "admin"
    }

    @PostConstruct
    fun check() {
        val placeholders = buildList {
            if (keycloakProperties.backend.clientSecret == PLACEHOLDER_CLIENT_SECRET) add("KEYCLOAK_BACKEND_CLIENT_SECRET")
            if (keycloakProperties.admin.password == PLACEHOLDER_ADMIN_PASSWORD) add("KEYCLOAK_ADMIN_PASSWORD")
        }
        if (placeholders.isEmpty()) return

        val message = "Placeholder Keycloak credentials in use: ${placeholders.joinToString()}. Set real values outside local development."
        if (environment.matchesProfiles("prod")) {
            throw IllegalStateException(message)
        }
        logger.warn(message)
    }
}
