package com.template.auth.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Callback Request DTO
 *
 * Sent by the frontend callback page with the query parameters Keycloak redirected back with.
 */
data class CallbackRequest(
    @field:Schema(description = "Authorization code Keycloak redirected back with")
    val code: String,

    @field:Schema(description = "`state` Keycloak redirected back with; must match the one issued by /login or /register")
    val state: String,

    @field:Schema(description = "Must be one of the allow-listed redirect URIs and the one used to start the flow; the first allow-listed URI when absent")
    @JsonProperty("redirect_uri")
    val redirectUri: String? = null
)
