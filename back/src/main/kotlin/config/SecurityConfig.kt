package config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security Configuration with Keycloak OAuth2 Integration
 *
 * Configures security using Spring Security OAuth2 Resource Server to validate
 * JWT tokens issued by Keycloak. Replaces the previous custom JWT filter approach.
 *
 * Key features:
 * - OAuth2 Resource Server with JWT validation
 * - CORS configuration for frontend origins
 * - Stateless session management
 * - Public endpoints for authentication flow
 * - HTTP-only cookies for refresh tokens
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf ->
                // Disable CSRF for stateless API
                // When using HTTP-only cookies, you may want to enable CSRF protection
                csrf.disable()
            }
            .cors { cors ->
                cors.configurationSource(corsConfigurationSource())
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { authz ->
                authz
                    // Public endpoints - authentication flow
                    .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/callback",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/health"
                    ).permitAll()

                    // Public endpoints - health checks and errors
                    .requestMatchers("/actuator/health", "/error").permitAll()

                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    // JWT validation is configured via application.yml
                    // Spring will automatically validate tokens against Keycloak's JWK Set
                }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allow frontend origins
        configuration.allowedOrigins = listOf(
            "http://localhost:5173",
            "http://localhost:3000"
        )

        // Allow common HTTP methods
        configuration.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        )

        // Allow all headers
        configuration.allowedHeaders = listOf("*")

        // Allow credentials (cookies, authorization headers)
        configuration.allowCredentials = true

        // Cache preflight requests for 1 hour
        configuration.maxAge = 3600

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    /**
     * BCrypt password encoder bean.
     * Still needed for any custom password operations outside of Keycloak.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}
