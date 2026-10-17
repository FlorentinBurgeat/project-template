package features.keycloak.controller

import exception.ErrorResponse
import exception.KeycloakUnavailableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Maps Keycloak outages to 503 so the frontend can tell them apart from rejected credentials (401).
 */
@RestControllerAdvice(assignableTypes = [KeycloakAuthController::class])
class KeycloakAuthExceptionHandler {

    @ExceptionHandler(KeycloakUnavailableException::class)
    fun handleKeycloakUnavailable(ex: KeycloakUnavailableException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE.value(),
            error = "Service Unavailable",
            message = "Authentication server unavailable, please retry later"
        )

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse)
    }
}
