package features.keycloak.config

import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.springframework.mock.env.MockEnvironment

class KeycloakSecretsCheckTest {

    private fun properties(clientSecret: String, adminPassword: String) = KeycloakProperties(
        admin = KeycloakProperties.AdminConfig(password = adminPassword),
        backend = KeycloakProperties.BackendConfig(clientSecret = clientSecret)
    )

    private fun environment(vararg profiles: String) = MockEnvironment().apply { setActiveProfiles(*profiles) }

    @Test
    fun `placeholder credentials fail startup with the prod profile`() {
        val check = KeycloakSecretsCheck(
            properties(KeycloakSecretsCheck.PLACEHOLDER_CLIENT_SECRET, "strong-password"),
            environment("prod")
        )

        assertFailsWith<IllegalStateException> { check.check() }
    }

    @Test
    fun `placeholder credentials only warn outside prod`() {
        KeycloakSecretsCheck(
            properties(KeycloakSecretsCheck.PLACEHOLDER_CLIENT_SECRET, KeycloakSecretsCheck.PLACEHOLDER_ADMIN_PASSWORD),
            environment("dev")
        ).check()
    }

    @Test
    fun `real credentials pass with the prod profile`() {
        KeycloakSecretsCheck(properties("a-real-secret", "strong-password"), environment("prod")).check()
    }
}
