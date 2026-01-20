package features.authentication.controller

import features.authentication.dto.*
import features.authentication.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.login(request)
        return ResponseEntity.ok(tokenResponse)
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authService.refreshAccessToken(request)
        return ResponseEntity.ok(tokenResponse)
    }

    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val userId = authentication.principal as UUID
        val userResponse = authService.getUserById(userId)
        return ResponseEntity.ok(userResponse)
    }
}
