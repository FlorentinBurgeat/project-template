package com.template.shared

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Name of the bearer security scheme: protected operations declare it with `@SecurityRequirement(name = BEARER_AUTH)`. */
const val BEARER_AUTH = "bearerAuth"

/**
 * The parts of the OpenAPI contract that belong to the whole API rather than to one module: its title and the
 * bearer JWT scheme (the access token Keycloak issues, validated by the resource server). Each controller opts in
 * to the scheme; the public /api/auth operations declare none.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun apiContract() = OpenApiCustomizer { openApi ->
        openApi.info = Info().title("Template API").version("v1")
        val components = openApi.components ?: Components().also { openApi.components = it }
        components.addSecuritySchemes(
            BEARER_AUTH,
            SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Keycloak access token returned by handleAuthCallback or refreshToken, sent as `Authorization: Bearer <token>`")
        )
    }
}
