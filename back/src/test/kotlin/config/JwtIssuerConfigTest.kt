package config

import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource

/**
 * Keycloak issues tokens with its external hostname (KC_HOSTNAME) as `iss`, while the backend reaches it
 * over the Docker network. The issuer check must use the external URL, the key fetch the internal one,
 * otherwise every Bearer token is rejected with 401.
 */
class JwtIssuerConfigTest {

    @Test
    fun `issuer-uri uses the external Keycloak URL and jwk-set-uri the internal one`() {
        val environment = StandardEnvironment().apply {
            propertySources.addFirst(
                MapPropertySource(
                    "docker",
                    mapOf(
                        "KEYCLOAK_INTERNAL_URL" to "http://keycloak:8080",
                        "KEYCLOAK_EXTERNAL_URL" to "http://localhost:8081",
                        "KEYCLOAK_REALM_NAME" to "template"
                    )
                )
            )
            YamlPropertySourceLoader().load("application", ClassPathResource("application.yml"))
                .forEach { propertySources.addLast(it) }
        }

        assertEquals(
            "http://localhost:8081/realms/template",
            environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")
        )
        assertEquals(
            "http://keycloak:8080/realms/template/protocol/openid-connect/certs",
            environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
        )
    }
}
