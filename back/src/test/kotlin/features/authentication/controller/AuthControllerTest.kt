package features.authentication.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import config.TestApplication
import features.authentication.dto.*
import features.authentication.service.AuthService
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.*

@WebMvcTest(controllers = [AuthController::class])
@ContextConfiguration(classes = [TestApplication::class])
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var authService: AuthService

    @Test
    fun `register should return 201 and token response when registration is successful`() {
        // Given
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            name = "Test User"
        )
        val tokenResponse = TokenResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 900000L
        )

        every { authService.register(request) } returns tokenResponse

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.expiresIn").value(900000))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `register should return 400 when email is invalid`() {
        // Given
        val request = RegisterRequest(
            email = "invalid-email",
            password = "password123",
            name = "Test User"
        )

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `register should return 400 when password is too short`() {
        // Given
        val request = RegisterRequest(
            email = "test@example.com",
            password = "12345", // Less than 6 characters
            name = "Test User"
        )

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `register should return 400 when name is blank`() {
        // Given
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            name = ""
        )

        // When & Then
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `login should return 200 and token response when credentials are valid`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )
        val tokenResponse = TokenResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 900000L
        )

        every { authService.login(request) } returns tokenResponse

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.expiresIn").value(900000))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `login should return 400 when email is invalid`() {
        // Given
        val request = LoginRequest(
            email = "invalid-email",
            password = "password123"
        )

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `login should return 400 when password is blank`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = ""
        )

        // When & Then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    @Test
    fun `refreshToken should return 200 and new token response when refresh token is valid`() {
        // Given
        val request = RefreshTokenRequest(refreshToken = "valid-refresh-token")
        val tokenResponse = TokenResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 900000L
        )

        every { authService.refreshAccessToken(request) } returns tokenResponse

        // When & Then
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.expiresIn").value(900000))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
    }

    @Test
    fun `refreshToken should return 400 when refresh token is blank`() {
        // Given
        val request = RefreshTokenRequest(refreshToken = "")

        // When & Then
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Validation Error"))
    }

    // Note: Authentication tests are skipped in unit tests since security filters are disabled
    // These should be tested in integration tests with full security context
}
