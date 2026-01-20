package features.authentication.service

import exception.InvalidCredentialsException
import exception.InvalidTokenException
import exception.UserAlreadyExistsException
import features.authentication.dto.*
import features.authentication.entity.RefreshToken
import features.authentication.mappers.UserMapper
import features.authentication.repository.RefreshTokenRepository
import features.authentication.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import security.JwtProvider
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${jwt.access-expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long
) {

    fun register(request: RegisterRequest): TokenResponse {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            throw UserAlreadyExistsException("User with email ${request.email} already exists")
        }

        // Hash password and create new user
        val passwordHash = passwordEncoder.encode(request.password)
        val user = UserMapper.toEntity(request, passwordHash)

        val savedUser = userRepository.save(user)

        // Generate tokens
        return generateTokenResponse(savedUser.id)
    }

    fun login(request: LoginRequest): TokenResponse {
        // Find user by email
        val user = userRepository.findByEmail(request.email)
            ?: throw InvalidCredentialsException("Invalid email or password")

        // Verify password
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid email or password")
        }

        // Generate tokens
        return generateTokenResponse(user.id)
    }

    fun refreshAccessToken(request: RefreshTokenRequest): TokenResponse {
        // Validate refresh token
        if (!jwtProvider.validateToken(request.refreshToken)) {
            throw InvalidTokenException("Invalid or expired refresh token")
        }

        // Get user ID from token
        val userId = jwtProvider.getUserIdFromToken(request.refreshToken)
            ?: throw InvalidTokenException("Invalid refresh token")

        // Verify refresh token exists in database
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw InvalidTokenException("Refresh token not found")

        // Check if token is expired
        if (refreshToken.expiresAt.isBefore(Instant.now())) {
            refreshTokenRepository.deleteByToken(request.refreshToken)
            throw InvalidTokenException("Refresh token has expired")
        }

        // Verify user exists
        val user = userRepository.findById(userId)
            ?: throw InvalidTokenException("User not found")

        // Delete old refresh token
        refreshTokenRepository.deleteByToken(request.refreshToken)

        // Generate new tokens
        return generateTokenResponse(user.id)
    }

    private fun generateTokenResponse(userId: UUID): TokenResponse {
        val accessToken = jwtProvider.generateAccessToken(userId)
        val refreshToken = jwtProvider.generateRefreshToken(userId)

        // Save refresh token to database
        val refreshTokenEntity = RefreshToken(
            token = refreshToken,
            userId = userId,
            expiresAt = Instant.now().plusMillis(refreshExpiration),
            createdAt = Instant.now()
        )
        refreshTokenRepository.save(refreshTokenEntity)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = accessExpiration
        )
    }

    fun getUserById(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw InvalidCredentialsException("User not found")

        return UserMapper.toResponse(user)
    }
}
