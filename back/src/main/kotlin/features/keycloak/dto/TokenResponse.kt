package features.keycloak.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Token Response DTO
 *
 * Represents the response from Keycloak's token endpoint.
 * Contains access token, refresh token, and expiration information.
 */
data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @JsonProperty("expires_in")
    val expiresIn: Long,

    @JsonProperty("refresh_expires_in")
    val refreshExpiresIn: Long? = null,

    @JsonProperty("token_type")
    val tokenType: String = "Bearer"
)

/**
 * Public Token Response DTO
 *
 * Response sent to the frontend.
 * Only contains the access token (refresh token is in HTTP-only cookie).
 */
data class PublicTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("expires_in")
    val expiresIn: Long,

    @JsonProperty("token_type")
    val tokenType: String = "Bearer"
)
