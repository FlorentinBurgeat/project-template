package com.template.auth.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Login Redirect Response DTO
 *
 * Contains the URL to redirect the user to Keycloak's login page.
 */
data class LoginRedirectResponse(
    @field:Schema(description = "Keycloak URL to send the browser to (carries the `state` and the PKCE challenge)")
    @JsonProperty("redirect_url")
    val redirectUrl: String
)
