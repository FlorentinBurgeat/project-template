package features.authentication.mappers

import features.authentication.dto.RegisterRequest
import features.authentication.dto.UserResponse
import features.authentication.entity.User
import java.time.Instant

object UserMapper {

    /**
     * Converts a User entity to a UserResponse DTO
     */
    fun toResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            createdAt = user.createdAt
        )
    }

    /**
     * Converts a RegisterRequest DTO to a User entity
     */
    fun toEntity(request: RegisterRequest, passwordHash: String): User {
        return User(
            email = request.email,
            name = request.name,
            passwordHash = passwordHash,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
}
