package features.keycloak.service

import features.keycloak.dto.TokenResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException

/**
 * Keycloak Authentication Service
 *
 * Handles OAuth2 authorization code flow with Keycloak:
 * - Exchange authorization code for tokens
 * - Refresh access tokens using refresh tokens
 * - Revoke tokens on logout
 *
 * Uses Spring RestTemplate to communicate with Keycloak's token endpoint.
 */
@Service
class KeycloakAuthService(
    @Value("\${keycloak.url}") private val keycloakUrl: String,
    @Value("\${keycloak.realm.name}") private val realmName: String,
    @Value("\${keycloak.backend.client-id}") private val clientId: String,
    @Value("\${keycloak.backend.client-secret}") private val clientSecret: String,
    @Value("\${keycloak.frontend.redirect-uris[0]}") private val redirectUri: String
) {

    private val logger = LoggerFactory.getLogger(KeycloakAuthService::class.java)
    private val restTemplate = RestTemplate()

    private val tokenEndpoint: String
        get() = "$keycloakUrl/realms/$realmName/protocol/openid-connect/token"

    private val logoutEndpoint: String
        get() = "$keycloakUrl/realms/$realmName/protocol/openid-connect/logout"

    /**
     * Exchanges an authorization code for access and refresh tokens.
     *
     * @param code Authorization code from Keycloak redirect
     * @param redirectUri The redirect URI used in the authorization request
     * @return TokenResponse containing access and refresh tokens
     */
    fun exchangeCodeForTokens(code: String, redirectUri: String): TokenResponse {
        logger.info("Exchanging authorization code for tokens")

        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("client_id", clientId)
            add("client_secret", clientSecret)
        }

        return requestTokens(body, "code exchange")
    }

    /**
     * Refreshes the access token using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return TokenResponse containing new access and refresh tokens
     */
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        logger.info("Refreshing access token")

        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
            add("client_id", clientId)
            add("client_secret", clientSecret)
        }

        return requestTokens(body, "token refresh")
    }

    /**
     * Revokes a refresh token (logout).
     *
     * @param refreshToken The refresh token to revoke
     */
    fun revokeToken(refreshToken: String) {
        logger.info("Revoking refresh token")

        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("refresh_token", refreshToken)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val request = HttpEntity(body, headers)

        try {
            restTemplate.postForEntity(logoutEndpoint, request, String::class.java)
            logger.info("Token revoked successfully")
        } catch (e: HttpClientErrorException) {
            logger.error("Failed to revoke token: ${e.statusCode} - ${e.responseBodyAsString}")
            throw RuntimeException("Token revocation failed", e)
        }
    }

    /**
     * Makes a token request to Keycloak's token endpoint.
     */
    private fun requestTokens(body: MultiValueMap<String, String>, operation: String): TokenResponse {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val request = HttpEntity(body, headers)

        try {
            val response = restTemplate.postForEntity(tokenEndpoint, request, TokenResponse::class.java)

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                logger.info("$operation successful")
                return response.body!!
            } else {
                logger.error("$operation failed: ${response.statusCode}")
                throw RuntimeException("$operation failed with status: ${response.statusCode}")
            }
        } catch (e: HttpClientErrorException) {
            logger.error("$operation failed: ${e.statusCode} - ${e.responseBodyAsString}")
            throw RuntimeException("$operation failed", e)
        }
    }

    /**
     * Generates the Keycloak login URL for the authorization code flow.
     *
     * @param redirectUri The redirect URI to return to after login
     * @return The complete login URL
     */
    fun getLoginUrl(redirectUri: String): String {
        val authEndpoint = "$keycloakUrl/realms/$realmName/protocol/openid-connect/auth"

        return buildString {
            append(authEndpoint)
            append("?response_type=code")
            append("&client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            append("&scope=openid profile email")
        }
    }

    /**
     * Generates the Keycloak registration URL.
     *
     * @param redirectUri The redirect URI to return to after registration
     * @return The complete registration URL
     */
    fun getRegistrationUrl(redirectUri: String): String {
        val authEndpoint = "$keycloakUrl/realms/$realmName/protocol/openid-connect/registrations"

        return buildString {
            append(authEndpoint)
            append("?client_id=$clientId")
            append("&redirect_uri=$redirectUri")
            append("&response_type=code")
            append("&scope=openid profile email")
        }
    }
}
