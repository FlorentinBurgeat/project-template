package features.keycloak.controller

import features.keycloak.dto.LoginRedirectResponse
import features.keycloak.dto.PublicTokenResponse
import features.keycloak.service.KeycloakAuthService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Keycloak Authentication Controller
 *
 * Handles the OAuth2 authorization code flow with HTTP-only cookies for refresh tokens.
 *
 * Endpoints:
 * - GET  /api/auth/login - Returns Keycloak login URL
 * - GET  /api/auth/register - Returns Keycloak registration URL
 * - POST /api/auth/callback - Exchanges authorization code for tokens
 * - POST /api/auth/refresh - Refreshes access token using HTTP-only cookie
 * - POST /api/auth/logout - Revokes tokens and clears cookies
 */
@RestController
@RequestMapping("/api/auth")
class KeycloakAuthController(
    private val authService: KeycloakAuthService,
    @Value("\${keycloak.frontend.redirect-uris[0]}") private val frontendRedirectUri: String
) {

    private val logger = LoggerFactory.getLogger(KeycloakAuthController::class.java)

    companion object {
        private const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
        private const val REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60 // 7 days in seconds
    }

    /**
     * Returns the Keycloak login URL for the frontend to redirect to.
     *
     * GET /api/auth/login?redirect_uri=http://localhost:5173/callback
     */
    @GetMapping("/login")
    fun getLoginUrl(
        @RequestParam("redirect_uri", required = false) redirectUri: String?
    ): ResponseEntity<LoginRedirectResponse> {
        val uri = redirectUri ?: frontendRedirectUri
        val loginUrl = authService.getLoginUrl(uri)

        logger.info("Generated login URL for redirect_uri: $uri")

        return ResponseEntity.ok(LoginRedirectResponse(loginUrl))
    }

    /**
     * Returns the Keycloak registration URL for the frontend to redirect to.
     *
     * GET /api/auth/register?redirect_uri=http://localhost:5173/callback
     */
    @GetMapping("/register")
    fun getRegistrationUrl(
        @RequestParam("redirect_uri", required = false) redirectUri: String?
    ): ResponseEntity<LoginRedirectResponse> {
        val uri = redirectUri ?: frontendRedirectUri
        val registrationUrl = authService.getRegistrationUrl(uri)

        logger.info("Generated registration URL for redirect_uri: $uri")

        return ResponseEntity.ok(LoginRedirectResponse(registrationUrl))
    }

    /**
     * Exchanges authorization code for tokens.
     * Sets refresh token as HTTP-only cookie, returns access token in response body.
     *
     * POST /api/auth/callback
     * Body: { "code": "auth-code-here", "redirect_uri": "http://localhost:5173/callback" }
     */
    @PostMapping("/callback")
    fun handleCallback(
        @RequestParam("code") code: String,
        @RequestParam("redirect_uri", required = false) redirectUri: String?,
        response: HttpServletResponse
    ): ResponseEntity<PublicTokenResponse> {
        try {
            val uri = redirectUri ?: frontendRedirectUri
            val tokenResponse = authService.exchangeCodeForTokens(code, uri)

            // Set refresh token as HTTP-only cookie
            tokenResponse.refreshToken?.let { refreshToken ->
                val cookie = createRefreshTokenCookie(refreshToken, REFRESH_TOKEN_MAX_AGE)
                response.addCookie(cookie)
                logger.info("Refresh token cookie set successfully")
            }

            // Return only access token to frontend
            val publicResponse = PublicTokenResponse(
                accessToken = tokenResponse.accessToken,
                expiresIn = tokenResponse.expiresIn,
                tokenType = tokenResponse.tokenType
            )

            logger.info("Authorization code exchanged successfully")
            return ResponseEntity.ok(publicResponse)

        } catch (e: Exception) {
            logger.error("Failed to exchange authorization code", e)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    /**
     * Refreshes the access token using the refresh token from HTTP-only cookie.
     * Sets new refresh token cookie, returns new access token.
     *
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    fun refreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<PublicTokenResponse> {
        try {
            // Get refresh token from HTTP-only cookie
            val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE_NAME }?.value

            if (refreshToken.isNullOrBlank()) {
                logger.warn("Refresh token cookie not found")
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }

            // Exchange refresh token for new tokens
            val tokenResponse = authService.refreshAccessToken(refreshToken)

            // Set new refresh token cookie
            tokenResponse.refreshToken?.let { newRefreshToken ->
                val cookie = createRefreshTokenCookie(newRefreshToken, REFRESH_TOKEN_MAX_AGE)
                response.addCookie(cookie)
                logger.info("Refresh token refreshed successfully")
            }

            // Return new access token
            val publicResponse = PublicTokenResponse(
                accessToken = tokenResponse.accessToken,
                expiresIn = tokenResponse.expiresIn,
                tokenType = tokenResponse.tokenType
            )

            logger.info("Access token refreshed successfully")
            return ResponseEntity.ok(publicResponse)

        } catch (e: Exception) {
            logger.error("Failed to refresh access token", e)

            // Clear invalid refresh token cookie
            val expiredCookie = createRefreshTokenCookie("", 0)
            response.addCookie(expiredCookie)

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    /**
     * Logs out the user by revoking tokens and clearing cookies.
     *
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        try {
            // Get refresh token from cookie
            val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE_NAME }?.value

            if (!refreshToken.isNullOrBlank()) {
                // Revoke the refresh token
                authService.revokeToken(refreshToken)
            }

            // Clear refresh token cookie
            val expiredCookie = createRefreshTokenCookie("", 0)
            response.addCookie(expiredCookie)

            logger.info("User logged out successfully")
            return ResponseEntity.noContent().build()

        } catch (e: Exception) {
            logger.error("Error during logout", e)

            // Still clear the cookie even if revocation fails
            val expiredCookie = createRefreshTokenCookie("", 0)
            response.addCookie(expiredCookie)

            return ResponseEntity.noContent().build()
        }
    }

    /**
     * Creates an HTTP-only cookie for the refresh token.
     *
     * Security features:
     * - httpOnly: Prevents JavaScript access (XSS protection)
     * - secure: Only sent over HTTPS in production
     * - sameSite: Prevents CSRF attacks
     * - path: Only sent to /api/auth endpoints
     */
    private fun createRefreshTokenCookie(value: String, maxAge: Int): Cookie {
        return Cookie(REFRESH_TOKEN_COOKIE_NAME, value).apply {
            isHttpOnly = true
            secure = false // Set to true in production with HTTPS
            path = "/api/auth"
            this.maxAge = maxAge
            // sameSite = "Strict" // Add when Cookie API supports it
        }
    }
}
