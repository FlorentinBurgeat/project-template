package com.template.shared

import java.time.LocalDateTime

/** Error body shared by every module's exception handlers. */
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val errors: Map<String, String>? = null
)
