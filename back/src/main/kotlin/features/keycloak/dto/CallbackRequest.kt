package features.keycloak.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Callback Request DTO
 *
 * Sent by the frontend callback page with the query parameters Keycloak redirected back with.
 */
data class CallbackRequest(
    val code: String,
    val state: String,
    @JsonProperty("redirect_uri")
    val redirectUri: String? = null
)
