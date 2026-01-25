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
    var url: String = "",
    var realm: RealmConfig = RealmConfig(),
    var admin: AdminConfig = AdminConfig(),
    var frontend: FrontendConfig = FrontendConfig(),
    var backend: BackendConfig = BackendConfig()
) {
    data class RealmConfig(
        var name: String = ""
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
