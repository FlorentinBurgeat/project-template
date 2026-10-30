package com.template.shared

/** The authenticated user, i.e. the Keycloak `sub`. Shared by every bounded context. */
@JvmInline
value class UserId(val value: String)
