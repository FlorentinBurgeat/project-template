package features.keycloak.dto

/**
 * A started authorization code flow: the Keycloak URL to redirect the browser to,
 * plus the `state` and PKCE `code_verifier` the callback must present.
 */
data class AuthorizationRequest(
    val url: String,
    val state: String,
    val codeVerifier: String
)
