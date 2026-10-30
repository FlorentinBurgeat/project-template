package com.template.auth.web

import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Name of the 503 body schema, referenced by the operations of [KeycloakAuthController]. */
internal const val AUTH_ERROR_SCHEMA = "AuthErrorResponse"

/**
 * Publishes the body [KeycloakAuthExceptionHandler] answers a Keycloak outage with. It is a [com.template.shared.ErrorResponse]
 * on the wire, but a bounded context may narrow the shared `ErrorResponse` schema (e.g. `errors` as a required map of
 * error codes), whereas here `errors` is always null. Hence a schema of its own rather than a wrong reuse.
 */
@Configuration
class AuthOpenApiConfig {

    @Bean
    fun authErrorContract() = OpenApiCustomizer { openApi ->
        val schemas = openApi.components?.schemas ?: return@OpenApiCustomizer

        schemas[AUTH_ERROR_SCHEMA] = JsonSchema().apply {
            types = setOf("object")
            description = "Error body of the auth endpoints when Keycloak is unreachable"
            properties = linkedMapOf<String, Schema<*>>(
                "timestamp" to JsonSchema().apply { types = setOf("string"); format = "date-time" },
                "status" to JsonSchema().apply { types = setOf("integer"); format = "int32" },
                "error" to JsonSchema().apply { types = setOf("string") },
                "message" to JsonSchema().apply { types = setOf("string") },
                "errors" to JsonSchema().apply { types = setOf("null"); description = "Always null: there is no field error" }
            )
            required = listOf("timestamp", "status", "error", "message", "errors")
        }
    }
}
