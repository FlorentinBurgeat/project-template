package features.keycloak.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Login Redirect Response DTO
 *
 * Contains the URL to redirect the user to Keycloak's login page.
 */
data class LoginRedirectResponse(
    @JsonProperty("redirect_url")
    val redirectUrl: String
)
