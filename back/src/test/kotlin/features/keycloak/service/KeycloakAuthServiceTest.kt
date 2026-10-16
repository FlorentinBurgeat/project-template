package features.keycloak.service

import exception.InvalidTokenException
import exception.KeycloakUnavailableException
import features.keycloak.config.KeycloakProperties
import features.keycloak.dto.TokenResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

class KeycloakAuthServiceTest {

    private val restTemplate = mockk<RestTemplate>()
    private val service = KeycloakAuthService(
        KeycloakProperties(
            url = "http://keycloak:8080",
            externalUrl = "http://localhost:8081",
            realm = KeycloakProperties.RealmConfig(name = "template"),
            backend = KeycloakProperties.BackendConfig(clientId = "backend-service", clientSecret = "secret")
        ),
        restTemplate
    )

    private val redirectUri = "http://localhost:3000/en/auth/callback"

    @Test
    fun `login URL targets the external Keycloak URL with state and S256 PKCE challenge`() {
        val authorization = service.startLogin(redirectUri)
        val uri = UriComponentsBuilder.fromUriString(authorization.url).build()
        val params = uri.queryParams

        assertEquals("http://localhost:8081/realms/template/protocol/openid-connect/auth", authorization.url.substringBefore('?'))
        assertEquals("code", params.getFirst("response_type"))
        assertEquals("backend-service", params.getFirst("client_id"))
        assertEquals(redirectUri, params.getFirst("redirect_uri"))
        assertEquals("openid%20profile%20email", params.getFirst("scope"))
        assertEquals(authorization.state, params.getFirst("state"))
        assertEquals("S256", params.getFirst("code_challenge_method"))
        assertEquals(s256(authorization.codeVerifier), params.getFirst("code_challenge"))
    }

    @Test
    fun `ui_locales is sent only when a locale is given`() {
        val withLocale = UriComponentsBuilder.fromUriString(service.startLogin(redirectUri, "fr").url).build()
        val withoutLocale = UriComponentsBuilder.fromUriString(service.startLogin(redirectUri).url).build()

        assertEquals("fr", withLocale.queryParams.getFirst("ui_locales"))
        assertEquals(null, withoutLocale.queryParams.getFirst("ui_locales"))
    }

    @Test
    fun `registration URL targets the registrations endpoint`() {
        val authorization = service.startRegistration(redirectUri)

        assertEquals(
            "http://localhost:8081/realms/template/protocol/openid-connect/registrations",
            authorization.url.substringBefore('?')
        )
    }

    @Test
    fun `each authorization gets a fresh state and verifier`() {
        val first = service.startLogin(redirectUri)
        val second = service.startLogin(redirectUri)

        assertNotEquals(first.state, second.state)
        assertNotEquals(first.codeVerifier, second.codeVerifier)
        assertTrue(first.codeVerifier.length >= 43, "PKCE verifier must be at least 43 chars")
    }

    @Test
    fun `code exchange posts the PKCE verifier to the internal token endpoint`() {
        val request = slot<HttpEntity<MultiValueMap<String, String>>>()
        every {
            restTemplate.postForEntity(
                "http://keycloak:8080/realms/template/protocol/openid-connect/token",
                capture(request),
                TokenResponse::class.java
            )
        } returns ResponseEntity.ok(TokenResponse(accessToken = "access", expiresIn = 300))

        val tokens = service.exchangeCodeForTokens("the-code", redirectUri, "the-verifier")

        assertEquals("access", tokens.accessToken)
        val body = request.captured.body!!
        assertEquals("authorization_code", body.getFirst("grant_type"))
        assertEquals("the-code", body.getFirst("code"))
        assertEquals("the-verifier", body.getFirst("code_verifier"))
        assertEquals(redirectUri, body.getFirst("redirect_uri"))
    }

    @Test
    fun `Keycloak 4xx is reported as an invalid token`() {
        every { restTemplate.postForEntity(any<String>(), any(), TokenResponse::class.java) } throws
            HttpClientErrorException(HttpStatus.BAD_REQUEST)

        assertFailsWith<InvalidTokenException> { service.refreshAccessToken("refresh") }
    }

    @Test
    fun `Keycloak 5xx is reported as unavailable`() {
        every { restTemplate.postForEntity(any<String>(), any(), TokenResponse::class.java) } throws
            HttpServerErrorException(HttpStatus.BAD_GATEWAY)

        assertFailsWith<KeycloakUnavailableException> { service.refreshAccessToken("refresh") }
    }

    @Test
    fun `network failure is reported as unavailable`() {
        every { restTemplate.postForEntity(any<String>(), any(), TokenResponse::class.java) } throws
            ResourceAccessException("Connection refused")

        assertFailsWith<KeycloakUnavailableException> { service.refreshAccessToken("refresh") }
    }

    private fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
