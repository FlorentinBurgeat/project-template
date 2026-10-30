package com.template.auth.web

import com.template.auth.InvalidTokenException
import com.template.auth.config.KeycloakProperties
import com.template.auth.web.dto.AuthorizationRequest
import com.template.auth.web.dto.CallbackRequest
import com.template.auth.web.dto.LoginRedirectResponse
import com.template.auth.web.dto.PublicTokenResponse
import com.template.auth.web.dto.TokenResponse
import com.template.auth.keycloak.KeycloakAuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.security.MessageDigest
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Keycloak Authentication Controller
 *
 * Handles the OAuth2 authorization code flow with HTTP-only cookies.
 *
 * Endpoints:
 * - GET  /api/auth/login - Returns Keycloak login URL
 * - GET  /api/auth/register - Returns Keycloak registration URL
 * - POST /api/auth/callback - Exchanges authorization code for tokens
 * - POST /api/auth/refresh - Refreshes access token using HTTP-only cookie
 * - POST /api/auth/logout - Revokes tokens and clears cookies
 *
 * Login CSRF protection: /login and /register store `state` and the PKCE verifier in a short-lived
 * HTTP-only cookie; /callback only accepts a code whose `state` matches that cookie.
 * Cookie CSRF protection: all cookies are SameSite=Strict, and cross-origin requests from origins
 * outside the CORS allow-list are rejected by the CORS filter (see SecurityConfig).
 */
@Tag(name = "auth")
@RestController
@RequestMapping("/api/auth")
class KeycloakAuthController(
    private val authService: KeycloakAuthService,
    keycloakProperties: KeycloakProperties,
    @Value("\${auth.cookie.secure}") private val secureCookies: Boolean
) {

    private val logger = LoggerFactory.getLogger(KeycloakAuthController::class.java)
    private val allowedRedirectUris: List<String> = keycloakProperties.frontend.redirectUris
    private val supportedLocales: List<String> = keycloakProperties.realm.supportedLocales

    companion object {
        private const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
        private const val AUTH_FLOW_COOKIE_NAME = "auth_flow"
        private const val COOKIE_PATH = "/api/auth"
        private val DEFAULT_REFRESH_TOKEN_MAX_AGE: Duration = Duration.ofDays(7)
        private val AUTH_FLOW_MAX_AGE: Duration = Duration.ofMinutes(10)
    }

    /**
     * Returns the Keycloak login URL for the frontend to redirect to.
     *
     * GET /api/auth/login?redirect_uri=http://localhost:3000/en/auth/callback&locale=en
     */
    @Operation(
        operationId = "getLoginUrl",
        summary = "Start the login flow",
        description = "Returns the Keycloak login URL to redirect the browser to. Sets the short-lived HTTP-only " +
            "`auth_flow` cookie holding the `state` and the PKCE verifier, which `handleAuthCallback` checks."
    )
    @ApiResponse(responseCode = "200", description = "The Keycloak login URL")
    @ApiResponse(responseCode = "400", description = "`redirect_uri` is not in the allow-list", content = [Content()])
    @GetMapping("/login")
    fun getLoginUrl(
        @Parameter(description = "Where Keycloak redirects back; must be allow-listed (`keycloak.frontend.redirect-uris`). The first allow-listed URI when absent.")
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        @Parameter(description = "Language of the Keycloak page; an unsupported locale is ignored and Keycloak uses its default.")
        @RequestParam("locale", required = false) locale: String?
    ): ResponseEntity<LoginRedirectResponse> {
        val uri = resolveRedirectUri(redirectUri) ?: return ResponseEntity.badRequest().build()
        return authorizationResponse(authService.startLogin(uri, resolveLocale(locale)))
    }

    /**
     * Returns the Keycloak registration URL for the frontend to redirect to.
     *
     * GET /api/auth/register?redirect_uri=http://localhost:3000/en/auth/callback&locale=en
     */
    @Operation(
        operationId = "getRegistrationUrl",
        summary = "Start the registration flow",
        description = "Returns the Keycloak registration URL to redirect the browser to. Sets the short-lived " +
            "HTTP-only `auth_flow` cookie holding the `state` and the PKCE verifier, which `handleAuthCallback` checks."
    )
    @ApiResponse(responseCode = "200", description = "The Keycloak registration URL")
    @ApiResponse(responseCode = "400", description = "`redirect_uri` is not in the allow-list", content = [Content()])
    @GetMapping("/register")
    fun getRegistrationUrl(
        @Parameter(description = "Where Keycloak redirects back; must be allow-listed (`keycloak.frontend.redirect-uris`). The first allow-listed URI when absent.")
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        @Parameter(description = "Language of the Keycloak page; an unsupported locale is ignored and Keycloak uses its default.")
        @RequestParam("locale", required = false) locale: String?
    ): ResponseEntity<LoginRedirectResponse> {
        val uri = resolveRedirectUri(redirectUri) ?: return ResponseEntity.badRequest().build()
        return authorizationResponse(authService.startRegistration(uri, resolveLocale(locale)))
    }

    /**
     * Exchanges authorization code for tokens.
     * Sets refresh token as HTTP-only cookie, returns access token in response body.
     *
     * POST /api/auth/callback
     * Body: { "code": "...", "state": "...", "redirect_uri": "http://localhost:3000/en/auth/callback" }
     */
    @Operation(
        operationId = "handleAuthCallback",
        summary = "Exchange the authorization code for tokens",
        description = "Checks `state` against the HTTP-only `auth_flow` cookie, then exchanges the code (with the PKCE " +
            "verifier). The access token is returned in the body; the refresh token is set as an HTTP-only cookie. " +
            "The `auth_flow` cookie is cleared."
    )
    @ApiResponse(responseCode = "200", description = "The access token; the refresh token is in an HTTP-only cookie")
    @ApiResponse(
        responseCode = "400",
        description = "`redirect_uri` is not in the allow-list, or the body is unreadable",
        content = [Content()]
    )
    @ApiResponse(
        responseCode = "401",
        description = "Missing `auth_flow` cookie, `state` mismatch, or code rejected by Keycloak",
        content = [Content()]
    )
    @ApiResponse(
        responseCode = "503",
        description = "Keycloak is unreachable",
        content = [Content(schema = Schema(ref = "#/components/schemas/$AUTH_ERROR_SCHEMA"))]
    )
    @PostMapping("/callback")
    fun handleCallback(
        @RequestBody body: CallbackRequest,
        @Parameter(hidden = true) @CookieValue(AUTH_FLOW_COOKIE_NAME, required = false) authFlow: String?
    ): ResponseEntity<PublicTokenResponse> {
        val uri = resolveRedirectUri(body.redirectUri) ?: return ResponseEntity.badRequest().build()
        val clearAuthFlow = cookie(AUTH_FLOW_COOKIE_NAME, "", Duration.ZERO)

        val (expectedState, codeVerifier) = authFlow?.split('.', limit = 2)?.takeIf { it.size == 2 }
            ?: return unauthorized(clearAuthFlow)
        if (!MessageDigest.isEqual(expectedState.toByteArray(), body.state.toByteArray())) {
            logger.warn("Authorization callback rejected: state mismatch")
            return unauthorized(clearAuthFlow)
        }

        val tokenResponse = try {
            authService.exchangeCodeForTokens(body.code, uri, codeVerifier)
        } catch (e: InvalidTokenException) {
            return unauthorized(clearAuthFlow)
        }

        return tokenResponseEntity(tokenResponse, clearAuthFlow)
    }

    /**
     * Refreshes the access token using the refresh token from HTTP-only cookie.
     * Sets new refresh token cookie, returns new access token.
     * The cookie is only cleared when Keycloak rejects it; an outage returns 503 and keeps it.
     *
     * POST /api/auth/refresh
     */
    @Operation(
        operationId = "refreshToken",
        summary = "Refresh the access token",
        description = "Uses the HTTP-only refresh token cookie and sets the rotated one. The cookie is cleared " +
            "only when Keycloak rejects it; on an outage (503) it is kept."
    )
    @ApiResponse(responseCode = "200", description = "A new access token; the new refresh token is in an HTTP-only cookie")
    @ApiResponse(
        responseCode = "401",
        description = "Missing refresh token cookie, or refresh token rejected by Keycloak (cookie cleared)",
        content = [Content()]
    )
    @ApiResponse(
        responseCode = "503",
        description = "Keycloak is unreachable",
        content = [Content(schema = Schema(ref = "#/components/schemas/$AUTH_ERROR_SCHEMA"))]
    )
    @PostMapping("/refresh")
    fun refreshToken(
        @Parameter(hidden = true) @CookieValue(REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?
    ): ResponseEntity<PublicTokenResponse> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val tokenResponse = try {
            authService.refreshAccessToken(refreshToken)
        } catch (e: InvalidTokenException) {
            return unauthorized(cookie(REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO))
        }

        return tokenResponseEntity(tokenResponse)
    }

    /**
     * Logs out the user by revoking tokens and clearing cookies.
     * The cookie is cleared even if revocation fails.
     *
     * POST /api/auth/logout
     */
    @Operation(
        operationId = "logout",
        summary = "Log out",
        description = "Revokes the refresh token when present and clears the HTTP-only refresh token cookie, " +
            "even if the revocation fails."
    )
    @ApiResponse(responseCode = "204", description = "Logged out; the refresh token cookie is cleared")
    @PostMapping("/logout")
    fun logout(
        @Parameter(hidden = true) @CookieValue(REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?
    ): ResponseEntity<Void> {
        if (!refreshToken.isNullOrBlank()) {
            try {
                authService.revokeToken(refreshToken)
            } catch (e: RuntimeException) {
                logger.warn("Refresh token revocation failed during logout: {}", e.message)
            }
        }

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookie(REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO).toString())
            .build()
    }

    /** Returns the requested redirect URI if allow-listed (or the default when absent), null otherwise. */
    private fun resolveRedirectUri(requested: String?): String? {
        if (requested == null) return allowedRedirectUris.firstOrNull()
        if (requested in allowedRedirectUris) return requested
        logger.warn("Rejected redirect_uri not in keycloak.frontend.redirect-uris")
        return null
    }

    /** Returns the requested locale if supported by the realm, null otherwise (Keycloak then uses its default). */
    private fun resolveLocale(requested: String?): String? = requested?.takeIf { it in supportedLocales }

    private fun authorizationResponse(authorization: AuthorizationRequest): ResponseEntity<LoginRedirectResponse> {
        val flowCookie = cookie(AUTH_FLOW_COOKIE_NAME, "${authorization.state}.${authorization.codeVerifier}", AUTH_FLOW_MAX_AGE)
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, flowCookie.toString())
            .body(LoginRedirectResponse(authorization.url))
    }

    private fun tokenResponseEntity(
        tokenResponse: TokenResponse,
        vararg extraCookies: ResponseCookie
    ): ResponseEntity<PublicTokenResponse> {
        val cookies = extraCookies.toMutableList()
        tokenResponse.refreshToken?.let { refreshToken ->
            val maxAge = tokenResponse.refreshExpiresIn?.takeIf { it > 0 }?.let(Duration::ofSeconds)
                ?: DEFAULT_REFRESH_TOKEN_MAX_AGE
            cookies += cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, maxAge)
        }

        val publicResponse = PublicTokenResponse(
            accessToken = tokenResponse.accessToken,
            expiresIn = tokenResponse.expiresIn,
            tokenType = tokenResponse.tokenType
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, *cookies.map { it.toString() }.toTypedArray())
            .body(publicResponse)
    }

    private fun <T> unauthorized(vararg cookies: ResponseCookie): ResponseEntity<T> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.SET_COOKIE, *cookies.map { it.toString() }.toTypedArray())
            .build()

    /**
     * Creates an HTTP-only cookie scoped to /api/auth.
     *
     * - httpOnly: Prevents JavaScript access (XSS protection)
     * - secure: Only sent over HTTPS (auth.cookie.secure, enable in production)
     * - sameSite=Strict: Never sent on cross-site requests (CSRF protection)
     * - path: Only sent to /api/auth endpoints
     */
    private fun cookie(name: String, value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Strict")
            .path(COOKIE_PATH)
            .maxAge(maxAge)
            .build()
}
