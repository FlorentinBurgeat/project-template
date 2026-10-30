package com.template.auth.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

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
    @field:Schema(description = "JWT to send as `Authorization: Bearer <access_token>`; keep it in memory only")
    @JsonProperty("access_token")
    val accessToken: String,

    @field:Schema(description = "Access token lifetime in seconds")
    @JsonProperty("expires_in")
    val expiresIn: Long,

    @field:Schema(description = "Always `Bearer`")
    @JsonProperty("token_type")
    val tokenType: String = "Bearer"
)
