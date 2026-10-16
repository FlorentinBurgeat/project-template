package features.keycloak.controller

import exception.InvalidTokenException
import features.keycloak.config.KeycloakProperties
import features.keycloak.dto.AuthorizationRequest
import features.keycloak.dto.CallbackRequest
import features.keycloak.dto.LoginRedirectResponse
import features.keycloak.dto.PublicTokenResponse
import features.keycloak.dto.TokenResponse
import features.keycloak.service.KeycloakAuthService
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
    @GetMapping("/login")
    fun getLoginUrl(
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
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
    @GetMapping("/register")
    fun getRegistrationUrl(
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
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
    @PostMapping("/callback")
    fun handleCallback(
        @RequestBody body: CallbackRequest,
        @CookieValue(AUTH_FLOW_COOKIE_NAME, required = false) authFlow: String?
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
    @PostMapping("/refresh")
    fun refreshToken(
        @CookieValue(REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?
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
    @PostMapping("/logout")
    fun logout(
        @CookieValue(REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?
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
