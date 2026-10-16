package features.keycloak.service

import exception.InvalidTokenException
import exception.KeycloakUnavailableException
import features.keycloak.config.KeycloakProperties
import features.keycloak.dto.AuthorizationRequest
import features.keycloak.dto.TokenResponse
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

/**
 * Keycloak Authentication Service
 *
 * Handles OAuth2 authorization code flow with Keycloak:
 * - Build the login/registration URL (with `state` and PKCE S256 challenge)
 * - Exchange authorization code for tokens
 * - Refresh access tokens using refresh tokens
 * - Revoke tokens on logout
 *
 * Token calls use the internal Keycloak URL (Docker network); browser-facing URLs use the external one.
 *
 * Errors: Keycloak rejecting a code/token (4xx) raises [InvalidTokenException];
 * Keycloak being unreachable or failing (5xx, I/O) raises [KeycloakUnavailableException].
 */
@Service
class KeycloakAuthService(
    keycloakProperties: KeycloakProperties,
    private val restTemplate: RestTemplate = RestTemplate()
) {

    private val logger = LoggerFactory.getLogger(KeycloakAuthService::class.java)
    private val secureRandom = SecureRandom()

    private val keycloakUrl = keycloakProperties.url
    private val keycloakExternalUrl = keycloakProperties.externalUrl
    private val realmName = keycloakProperties.realm.name
    private val clientId = keycloakProperties.backend.clientId
    private val clientSecret = keycloakProperties.backend.clientSecret

    private val tokenEndpoint: String
        get() = "$keycloakUrl/realms/$realmName/protocol/openid-connect/token"

    private val logoutEndpoint: String
        get() = "$keycloakUrl/realms/$realmName/protocol/openid-connect/logout"

    /**
     * Exchanges an authorization code for access and refresh tokens.
     *
     * @param code Authorization code from Keycloak redirect
     * @param redirectUri The redirect URI used in the authorization request
     * @param codeVerifier The PKCE verifier matching the challenge sent in the authorization request
     * @return TokenResponse containing access and refresh tokens
     */
    fun exchangeCodeForTokens(code: String, redirectUri: String, codeVerifier: String): TokenResponse {
        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("code_verifier", codeVerifier)
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
        val body: MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("refresh_token", refreshToken)
        }

        callKeycloak("token revocation") {
            restTemplate.postForEntity(logoutEndpoint, formRequest(body), String::class.java)
        }
    }

    /**
     * Starts an authorization code flow against Keycloak's login page.
     *
     * @param redirectUri The (already validated) redirect URI to return to after login
     */
    fun startLogin(redirectUri: String): AuthorizationRequest = startAuthorization("auth", redirectUri)

    /**
     * Starts an authorization code flow against Keycloak's registration page.
     *
     * @param redirectUri The (already validated) redirect URI to return to after registration
     */
    fun startRegistration(redirectUri: String): AuthorizationRequest = startAuthorization("registrations", redirectUri)

    private fun startAuthorization(endpoint: String, redirectUri: String): AuthorizationRequest {
        val state = randomToken()
        val codeVerifier = randomToken()

        val url = UriComponentsBuilder
            .fromUriString("$keycloakExternalUrl/realms/$realmName/protocol/openid-connect/$endpoint")
            .queryParam("response_type", "code")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", "openid profile email")
            .queryParam("state", state)
            .queryParam("code_challenge", codeChallenge(codeVerifier))
            .queryParam("code_challenge_method", "S256")
            .encode()
            .toUriString()

        return AuthorizationRequest(url, state, codeVerifier)
    }

    /** 32 random bytes, base64url without padding (43 chars, valid as `state` and PKCE verifier). */
    private fun randomToken(): String {
        val bytes = ByteArray(32).also { secureRandom.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun codeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Makes a token request to Keycloak's token endpoint.
     */
    private fun requestTokens(body: MultiValueMap<String, String>, operation: String): TokenResponse {
        val response = callKeycloak(operation) {
            restTemplate.postForEntity(tokenEndpoint, formRequest(body), TokenResponse::class.java)
        }
        return response.body ?: throw KeycloakUnavailableException("$operation returned an empty body")
    }

    private fun formRequest(body: MultiValueMap<String, String>): HttpEntity<MultiValueMap<String, String>> {
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_FORM_URLENCODED }
        return HttpEntity(body, headers)
    }

    /** Translates Keycloak failures into domain exceptions: 4xx = rejected credentials, anything else = unavailable. */
    private fun <T> callKeycloak(operation: String, call: () -> T): T {
        try {
            return call()
        } catch (e: HttpClientErrorException) {
            logger.warn("Keycloak rejected $operation: {}", e.statusCode)
            throw InvalidTokenException("$operation rejected by Keycloak")
        } catch (e: RestClientException) {
            logger.error("Keycloak unavailable during $operation: {}", e.message)
            throw KeycloakUnavailableException("$operation failed: Keycloak unavailable", e)
        }
    }
}
