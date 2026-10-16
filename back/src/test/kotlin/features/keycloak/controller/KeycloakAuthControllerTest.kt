package features.keycloak.controller

import exception.InvalidTokenException
import exception.KeycloakUnavailableException
import features.keycloak.config.KeycloakProperties
import features.keycloak.dto.AuthorizationRequest
import features.keycloak.dto.TokenResponse
import features.keycloak.service.KeycloakAuthService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.servlet.http.Cookie
import kotlin.test.Test
import kotlin.test.assertTrue
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class KeycloakAuthControllerTest {

    private val authService = mockk<KeycloakAuthService>()
    private val enCallback = "http://localhost:3000/en/auth/callback"
    private val frCallback = "http://localhost:3000/fr/auth/callback"
    private val properties = KeycloakProperties(
        frontend = KeycloakProperties.FrontendConfig(redirectUris = listOf(enCallback, frCallback))
    )

    private fun mockMvc(secureCookies: Boolean = false): MockMvc =
        MockMvcBuilders.standaloneSetup(KeycloakAuthController(authService, properties, secureCookies))
            .setControllerAdvice(KeycloakAuthExceptionHandler())
            .build()

    private val mockMvc = mockMvc()

    private fun ResultActions.setCookies(): List<String> = andReturn().response.getHeaders(HttpHeaders.SET_COOKIE)

    private fun callbackBody(state: String, redirectUri: String = enCallback) =
        """{"code":"the-code","state":"$state","redirect_uri":"$redirectUri"}"""

    // --- login / register ---

    @Test
    fun `login with an allow-listed redirect_uri returns the URL and a strict auth flow cookie`() {
        every { authService.startLogin(frCallback) } returns AuthorizationRequest("https://kc/auth", "st", "ver")

        val cookies = mockMvc.perform(get("/api/auth/login").param("redirect_uri", frCallback))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.redirect_url").value("https://kc/auth"))
            .setCookies()

        val flowCookie = cookies.single { it.startsWith("auth_flow=st.ver") }
        assertTrue("HttpOnly" in flowCookie && "SameSite=Strict" in flowCookie && "Path=/api/auth" in flowCookie, flowCookie)
        assertTrue("Secure" !in flowCookie, flowCookie)
    }

    @Test
    fun `login without redirect_uri uses the first allow-listed URI`() {
        every { authService.startLogin(enCallback) } returns AuthorizationRequest("https://kc/auth", "st", "ver")

        mockMvc.perform(get("/api/auth/login")).andExpect(status().isOk)

        verify { authService.startLogin(enCallback) }
    }

    @Test
    fun `login and register reject a redirect_uri outside the allow-list`() {
        mockMvc.perform(get("/api/auth/login").param("redirect_uri", "https://evil.example/callback"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/api/auth/register").param("redirect_uri", "http://localhost:3000/en/other"))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { authService.startLogin(any()) }
        verify(exactly = 0) { authService.startRegistration(any()) }
    }

    @Test
    fun `cookies carry the Secure flag when enabled`() {
        every { authService.startRegistration(enCallback) } returns AuthorizationRequest("https://kc/reg", "st", "ver")

        val cookies = mockMvc(secureCookies = true).perform(get("/api/auth/register"))
            .andExpect(status().isOk)
            .setCookies()

        assertTrue(cookies.single().contains("Secure"), cookies.single())
    }

    // --- callback ---

    @Test
    fun `callback with matching state exchanges the code with the stored verifier`() {
        every { authService.exchangeCodeForTokens("the-code", enCallback, "ver") } returns
            TokenResponse(accessToken = "access", refreshToken = "refresh", expiresIn = 900, refreshExpiresIn = 1800)

        val cookies = mockMvc.perform(
            post("/api/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody("st"))
                .cookie(Cookie("auth_flow", "st.ver"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access"))
            .andExpect(jsonPath("$.refresh_token").doesNotExist())
            .setCookies()

        val refreshCookie = cookies.single { it.startsWith("refresh_token=refresh") }
        assertTrue("Max-Age=1800" in refreshCookie && "HttpOnly" in refreshCookie && "SameSite=Strict" in refreshCookie, refreshCookie)
        assertTrue(cookies.any { it.startsWith("auth_flow=;") && "Max-Age=0" in it }, cookies.toString())
    }

    @Test
    fun `callback with a mismatched state is rejected without exchanging the code`() {
        mockMvc.perform(
            post("/api/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody("attacker-state"))
                .cookie(Cookie("auth_flow", "st.ver"))
        ).andExpect(status().isUnauthorized)

        verify(exactly = 0) { authService.exchangeCodeForTokens(any(), any(), any()) }
    }

    @Test
    fun `callback without the auth flow cookie is rejected`() {
        mockMvc.perform(
            post("/api/auth/callback").contentType(MediaType.APPLICATION_JSON).content(callbackBody("st"))
        ).andExpect(status().isUnauthorized)

        verify(exactly = 0) { authService.exchangeCodeForTokens(any(), any(), any()) }
    }

    @Test
    fun `callback with a redirect_uri outside the allow-list is rejected`() {
        mockMvc.perform(
            post("/api/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody("st", "https://evil.example/callback"))
                .cookie(Cookie("auth_flow", "st.ver"))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `callback with a code rejected by Keycloak returns 401`() {
        every { authService.exchangeCodeForTokens(any(), any(), any()) } throws InvalidTokenException("rejected")

        mockMvc.perform(
            post("/api/auth/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(callbackBody("st"))
                .cookie(Cookie("auth_flow", "st.ver"))
        ).andExpect(status().isUnauthorized)
    }

    // --- refresh ---

    @Test
    fun `refresh without cookie returns 401`() {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `refresh rotates the refresh token cookie`() {
        every { authService.refreshAccessToken("old") } returns
            TokenResponse(accessToken = "access", refreshToken = "new", expiresIn = 900)

        val cookies = mockMvc.perform(post("/api/auth/refresh").cookie(Cookie("refresh_token", "old")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").value("access"))
            .setCookies()

        assertTrue(cookies.single().startsWith("refresh_token=new"), cookies.toString())
    }

    @Test
    fun `refresh rejected by Keycloak returns 401 and clears the cookie`() {
        every { authService.refreshAccessToken("old") } throws InvalidTokenException("rejected")

        val cookies = mockMvc.perform(post("/api/auth/refresh").cookie(Cookie("refresh_token", "old")))
            .andExpect(status().isUnauthorized)
            .setCookies()

        assertTrue(cookies.single().startsWith("refresh_token=;") && "Max-Age=0" in cookies.single(), cookies.toString())
    }

    @Test
    fun `refresh during a Keycloak outage returns 503 and keeps the cookie`() {
        every { authService.refreshAccessToken("old") } throws KeycloakUnavailableException("down")

        mockMvc.perform(post("/api/auth/refresh").cookie(Cookie("refresh_token", "old")))
            .andExpect(status().isServiceUnavailable)
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
    }

    // --- logout ---

    @Test
    fun `logout revokes the token and clears the cookie`() {
        every { authService.revokeToken("refresh") } just runs

        val cookies = mockMvc.perform(post("/api/auth/logout").cookie(Cookie("refresh_token", "refresh")))
            .andExpect(status().isNoContent)
            .setCookies()

        verify { authService.revokeToken("refresh") }
        assertTrue(cookies.single().startsWith("refresh_token=;"), cookies.toString())
    }

    @Test
    fun `logout clears the cookie even when revocation fails`() {
        every { authService.revokeToken(any()) } throws KeycloakUnavailableException("down")

        val cookies = mockMvc.perform(post("/api/auth/logout").cookie(Cookie("refresh_token", "refresh")))
            .andExpect(status().isNoContent)
            .setCookies()

        assertTrue(cookies.single().startsWith("refresh_token=;"), cookies.toString())
    }
}
