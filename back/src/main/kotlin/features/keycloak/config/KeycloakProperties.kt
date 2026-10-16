package features.keycloak.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Keycloak configuration properties.
 * Binds to 'keycloak.*' properties from application.yml.
 */
@Component
@ConfigurationProperties(prefix = "keycloak")
data class KeycloakProperties(
    /** Keycloak URL reachable from the backend (Docker network): token, logout and admin calls. */
    var url: String = "",
    /** Keycloak URL reachable from the browser; also the token issuer (KC_HOSTNAME). */
    var externalUrl: String = "",
    var realm: RealmConfig = RealmConfig(),
    var admin: AdminConfig = AdminConfig(),
    var frontend: FrontendConfig = FrontendConfig(),
    var backend: BackendConfig = BackendConfig()
) {
    data class RealmConfig(
        var name: String = "",
        /** Locales offered on Keycloak pages; first one is the default. Must match the frontend locales. */
        var supportedLocales: List<String> = listOf("en", "fr")
    )

    data class AdminConfig(
        var username: String = "",
        var password: String = "",
        var realm: String = "master",
        var clientId: String = "admin-cli"
    )

    data class FrontendConfig(
        var clientId: String = "",
        var redirectUris: List<String> = emptyList(),
        var webOrigins: List<String> = emptyList()
    )

    data class BackendConfig(
        var clientId: String = "",
        var clientSecret: String = ""
    )
}
