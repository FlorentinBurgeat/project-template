package features.authentication.service

import exception.InvalidCredentialsException
import exception.InvalidTokenException
import exception.UserAlreadyExistsException
import features.authentication.dto.LoginRequest
import features.authentication.dto.RefreshTokenRequest
import features.authentication.dto.RegisterRequest
import features.authentication.entity.RefreshToken
import features.authentication.entity.User
import features.authentication.repository.RefreshTokenRepository
import features.authentication.repository.UserRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import security.JwtProvider
import java.time.Instant
import java.util.*

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var jwtProvider: JwtProvider
    private lateinit var authService: AuthService
    private lateinit var passwordEncoder: BCryptPasswordEncoder

    private val accessExpiration = 900000L // 15 minutes
    private val refreshExpiration = 604800000L // 7 days

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        refreshTokenRepository = mockk()
        jwtProvider = mockk()
        passwordEncoder = BCryptPasswordEncoder()

        authService = AuthService(
            userRepository,
            refreshTokenRepository,
            jwtProvider,
            passwordEncoder,
            accessExpiration,
            refreshExpiration
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `register should create new user and return tokens`() {
        // Given
        val request = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            name = "Test User"
        )
        val userId = UUID.randomUUID()
        val savedUser = User(
            id = userId,
            email = request.email,
            name = request.name,
            passwordHash = "hashedPassword",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val accessToken = "access-token"
        val refreshToken = "refresh-token"

        every { userRepository.existsByEmail(request.email) } returns false
        every { userRepository.save(any()) } returns savedUser
        every { jwtProvider.generateAccessToken(userId) } returns accessToken
        every { jwtProvider.generateRefreshToken(userId) } returns refreshToken
        every { refreshTokenRepository.save(any()) } returns mockk()

        // When
        val result = authService.register(request)

        // Then
        assertEquals(accessToken, result.accessToken)
        assertEquals(refreshToken, result.refreshToken)
        assertEquals(accessExpiration, result.expiresIn)
        assertEquals("Bearer", result.tokenType)

        verify { userRepository.existsByEmail(request.email) }
        verify { userRepository.save(any()) }
        verify { jwtProvider.generateAccessToken(userId) }
        verify { jwtProvider.generateRefreshToken(userId) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `register should throw UserAlreadyExistsException when email exists`() {
        // Given
        val request = RegisterRequest(
            email = "existing@example.com",
            password = "password123",
            name = "Test User"
        )

        every { userRepository.existsByEmail(request.email) } returns true

        // When & Then
        val exception = assertThrows<UserAlreadyExistsException> {
            authService.register(request)
        }

        assertEquals("User with email ${request.email} already exists", exception.message)
        verify { userRepository.existsByEmail(request.email) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login should return tokens when credentials are valid`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )
        val userId = UUID.randomUUID()
        val hashedPassword = passwordEncoder.encode(request.password)
        val user = User(
            id = userId,
            email = request.email,
            name = "Test User",
            passwordHash = hashedPassword,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val accessToken = "access-token"
        val refreshToken = "refresh-token"

        every { userRepository.findByEmail(request.email) } returns user
        every { jwtProvider.generateAccessToken(userId) } returns accessToken
        every { jwtProvider.generateRefreshToken(userId) } returns refreshToken
        every { refreshTokenRepository.save(any()) } returns mockk()

        // When
        val result = authService.login(request)

        // Then
        assertEquals(accessToken, result.accessToken)
        assertEquals(refreshToken, result.refreshToken)
        assertEquals(accessExpiration, result.expiresIn)

        verify { userRepository.findByEmail(request.email) }
        verify { jwtProvider.generateAccessToken(userId) }
        verify { jwtProvider.generateRefreshToken(userId) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `login should throw InvalidCredentialsException when user not found`() {
        // Given
        val request = LoginRequest(
            email = "nonexistent@example.com",
            password = "password123"
        )

        every { userRepository.findByEmail(request.email) } returns null

        // When & Then
        val exception = assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
        verify { userRepository.findByEmail(request.email) }
    }

    @Test
    fun `login should throw InvalidCredentialsException when password is incorrect`() {
        // Given
        val request = LoginRequest(
            email = "test@example.com",
            password = "wrongpassword"
        )
        val userId = UUID.randomUUID()
        val hashedPassword = passwordEncoder.encode("correctpassword")
        val user = User(
            id = userId,
            email = request.email,
            name = "Test User",
            passwordHash = hashedPassword,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { userRepository.findByEmail(request.email) } returns user

        // When & Then
        val exception = assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        assertEquals("Invalid email or password", exception.message)
        verify { userRepository.findByEmail(request.email) }
        verify(exactly = 0) { jwtProvider.generateAccessToken(any()) }
    }

    @Test
    fun `refreshAccessToken should return new tokens when refresh token is valid`() {
        // Given
        val oldRefreshToken = "old-refresh-token"
        val request = RefreshTokenRequest(refreshToken = oldRefreshToken)
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            email = "test@example.com",
            name = "Test User",
            passwordHash = "hashedPassword",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val storedRefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = oldRefreshToken,
            userId = userId,
            expiresAt = Instant.now().plusMillis(refreshExpiration),
            createdAt = Instant.now()
        )
        val newAccessToken = "new-access-token"
        val newRefreshToken = "new-refresh-token"

        every { jwtProvider.validateToken(oldRefreshToken) } returns true
        every { jwtProvider.getUserIdFromToken(oldRefreshToken) } returns userId
        every { refreshTokenRepository.findByToken(oldRefreshToken) } returns storedRefreshToken
        every { userRepository.findById(userId) } returns user
        every { refreshTokenRepository.deleteByToken(oldRefreshToken) } returns true
        every { jwtProvider.generateAccessToken(userId) } returns newAccessToken
        every { jwtProvider.generateRefreshToken(userId) } returns newRefreshToken
        every { refreshTokenRepository.save(any()) } returns mockk()

        // When
        val result = authService.refreshAccessToken(request)

        // Then
        assertEquals(newAccessToken, result.accessToken)
        assertEquals(newRefreshToken, result.refreshToken)
        assertEquals(accessExpiration, result.expiresIn)

        verify { jwtProvider.validateToken(oldRefreshToken) }
        verify { jwtProvider.getUserIdFromToken(oldRefreshToken) }
        verify { refreshTokenRepository.findByToken(oldRefreshToken) }
        verify { userRepository.findById(userId) }
        verify { refreshTokenRepository.deleteByToken(oldRefreshToken) }
        verify { jwtProvider.generateAccessToken(userId) }
        verify { jwtProvider.generateRefreshToken(userId) }
        verify { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `refreshAccessToken should throw InvalidTokenException when token is invalid`() {
        // Given
        val invalidToken = "invalid-token"
        val request = RefreshTokenRequest(refreshToken = invalidToken)

        every { jwtProvider.validateToken(invalidToken) } returns false

        // When & Then
        val exception = assertThrows<InvalidTokenException> {
            authService.refreshAccessToken(request)
        }

        assertEquals("Invalid or expired refresh token", exception.message)
        verify { jwtProvider.validateToken(invalidToken) }
        verify(exactly = 0) { refreshTokenRepository.findByToken(any()) }
    }

    @Test
    fun `refreshAccessToken should throw InvalidTokenException when token not found in database`() {
        // Given
        val token = "valid-token-but-not-in-db"
        val request = RefreshTokenRequest(refreshToken = token)
        val userId = UUID.randomUUID()

        every { jwtProvider.validateToken(token) } returns true
        every { jwtProvider.getUserIdFromToken(token) } returns userId
        every { refreshTokenRepository.findByToken(token) } returns null

        // When & Then
        val exception = assertThrows<InvalidTokenException> {
            authService.refreshAccessToken(request)
        }

        assertEquals("Refresh token not found", exception.message)
        verify { jwtProvider.validateToken(token) }
        verify { jwtProvider.getUserIdFromToken(token) }
        verify { refreshTokenRepository.findByToken(token) }
    }

    @Test
    fun `refreshAccessToken should throw InvalidTokenException when token is expired`() {
        // Given
        val expiredToken = "expired-token"
        val request = RefreshTokenRequest(refreshToken = expiredToken)
        val userId = UUID.randomUUID()
        val storedRefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = expiredToken,
            userId = userId,
            expiresAt = Instant.now().minusMillis(3600000), // Expired 1 hour ago
            createdAt = Instant.now().minusMillis(refreshExpiration)
        )

        every { jwtProvider.validateToken(expiredToken) } returns true
        every { jwtProvider.getUserIdFromToken(expiredToken) } returns userId
        every { refreshTokenRepository.findByToken(expiredToken) } returns storedRefreshToken
        every { refreshTokenRepository.deleteByToken(expiredToken) } returns true

        // When & Then
        val exception = assertThrows<InvalidTokenException> {
            authService.refreshAccessToken(request)
        }

        assertEquals("Refresh token has expired", exception.message)
        verify { jwtProvider.validateToken(expiredToken) }
        verify { jwtProvider.getUserIdFromToken(expiredToken) }
        verify { refreshTokenRepository.findByToken(expiredToken) }
        verify { refreshTokenRepository.deleteByToken(expiredToken) }
    }

    @Test
    fun `refreshAccessToken should throw InvalidTokenException when user not found`() {
        // Given
        val token = "valid-token"
        val request = RefreshTokenRequest(refreshToken = token)
        val userId = UUID.randomUUID()
        val storedRefreshToken = RefreshToken(
            id = UUID.randomUUID(),
            token = token,
            userId = userId,
            expiresAt = Instant.now().plusMillis(refreshExpiration),
            createdAt = Instant.now()
        )

        every { jwtProvider.validateToken(token) } returns true
        every { jwtProvider.getUserIdFromToken(token) } returns userId
        every { refreshTokenRepository.findByToken(token) } returns storedRefreshToken
        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows<InvalidTokenException> {
            authService.refreshAccessToken(request)
        }

        assertEquals("User not found", exception.message)
        verify { jwtProvider.validateToken(token) }
        verify { jwtProvider.getUserIdFromToken(token) }
        verify { refreshTokenRepository.findByToken(token) }
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getUserById should return user response when user exists`() {
        // Given
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            email = "test@example.com",
            name = "Test User",
            passwordHash = "hashedPassword",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        every { userRepository.findById(userId) } returns user

        // When
        val result = authService.getUserById(userId)

        // Then
        assertEquals(userId, result.id)
        assertEquals(user.email, result.email)
        assertEquals(user.name, result.name)
        assertEquals(user.createdAt, result.createdAt)

        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getUserById should throw InvalidCredentialsException when user not found`() {
        // Given
        val userId = UUID.randomUUID()

        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows<InvalidCredentialsException> {
            authService.getUserById(userId)
        }

        assertEquals("User not found", exception.message)
        verify { userRepository.findById(userId) }
    }
}
