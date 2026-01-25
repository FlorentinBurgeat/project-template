package features.keycloak.service

import org.keycloak.admin.client.Keycloak
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.ws.rs.core.Response

/**
 * User Management Service (Option B - Custom Registration Example)
 *
 * Demonstrates how to use the Keycloak Admin API for custom user management operations.
 * This is an ALTERNATIVE to Keycloak-managed registration (Option A).
 *
 * Use this approach when you need:
 * - Custom registration UI/UX
 * - Additional business logic during registration
 * - Custom validation beyond Keycloak's built-in features
 * - Integration with other systems during user creation
 *
 * NOTE: For most use cases, Keycloak-managed registration (Option A) is recommended.
 *
 * Operations:
 * - Create user with password
 * - Update user profile
 * - Change password
 * - Delete user
 * - Send verification email
 */
@Service
class UserManagementService(
    private val keycloak: Keycloak,
    @Value("\${keycloak.realm.name}") private val realmName: String
) {

    private val logger = LoggerFactory.getLogger(UserManagementService::class.java)

    /**
     * Creates a new user in Keycloak (Custom Registration - Option B).
     *
     * @param email User's email address (also used as username)
     * @param password User's password
     * @param firstName User's first name (optional)
     * @param lastName User's last name (optional)
     * @param emailVerified Whether the email is already verified (default: false)
     * @return User ID (UUID) of the created user
     */
    fun createUser(
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        emailVerified: Boolean = false
    ): String {
        logger.info("Creating user with email: $email")

        // Check if user already exists
        val existingUsers = keycloak.realm(realmName).users().search(email, true)
        if (existingUsers.isNotEmpty()) {
            logger.warn("User with email $email already exists")
            throw IllegalArgumentException("User with email $email already exists")
        }

        // Create user representation
        val user = UserRepresentation().apply {
            username = email
            this.email = email
            this.firstName = firstName
            this.lastName = lastName
            isEnabled = true
            isEmailVerified = emailVerified
        }

        // Create user via Admin API
        val response: Response = keycloak.realm(realmName).users().create(user)

        if (response.status != 201) {
            logger.error("Failed to create user: ${response.statusInfo}")
            throw RuntimeException("Failed to create user: ${response.statusInfo}")
        }

        // Extract user ID from location header
        val userId = response.location.path.substringAfterLast("/")
        response.close()

        logger.info("User created successfully with ID: $userId")

        // Set password
        setUserPassword(userId, password, temporary = false)

        // Send verification email if not already verified
        if (!emailVerified) {
            sendVerificationEmail(userId)
        }

        return userId
    }

    /**
     * Sets or updates a user's password.
     *
     * @param userId Keycloak user ID
     * @param password New password
     * @param temporary Whether the password is temporary (user must change on next login)
     */
    fun setUserPassword(userId: String, password: String, temporary: Boolean = false) {
        logger.info("Setting password for user: $userId")

        val credential = CredentialRepresentation().apply {
            type = CredentialRepresentation.PASSWORD
            value = password
            isTemporary = temporary
        }

        keycloak.realm(realmName).users().get(userId).resetPassword(credential)
        logger.info("Password set successfully for user: $userId")
    }

    /**
     * Updates a user's profile information.
     *
     * @param userId Keycloak user ID
     * @param email New email (optional)
     * @param firstName New first name (optional)
     * @param lastName New last name (optional)
     */
    fun updateUserProfile(
        userId: String,
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ) {
        logger.info("Updating profile for user: $userId")

        val userResource = keycloak.realm(realmName).users().get(userId)
        val user = userResource.toRepresentation()

        email?.let { user.email = it }
        firstName?.let { user.firstName = it }
        lastName?.let { user.lastName = it }

        userResource.update(user)
        logger.info("User profile updated successfully: $userId")
    }

    /**
     * Deletes a user from Keycloak.
     *
     * @param userId Keycloak user ID
     */
    fun deleteUser(userId: String) {
        logger.info("Deleting user: $userId")

        keycloak.realm(realmName).users().get(userId).remove()
        logger.info("User deleted successfully: $userId")
    }

    /**
     * Sends a verification email to the user.
     *
     * @param userId Keycloak user ID
     */
    fun sendVerificationEmail(userId: String) {
        logger.info("Sending verification email to user: $userId")

        try {
            keycloak.realm(realmName).users().get(userId).sendVerifyEmail()
            logger.info("Verification email sent successfully to user: $userId")
        } catch (e: Exception) {
            logger.warn("Failed to send verification email (SMTP may not be configured): ${e.message}")
            // Don't throw exception - email sending is optional in dev environment
        }
    }

    /**
     * Gets a user by their ID.
     *
     * @param userId Keycloak user ID
     * @return UserRepresentation
     */
    fun getUserById(userId: String): UserRepresentation {
        return keycloak.realm(realmName).users().get(userId).toRepresentation()
    }

    /**
     * Searches for users by email.
     *
     * @param email Email to search for
     * @param exact Whether to perform exact match (default: true)
     * @return List of matching users
     */
    fun searchUsersByEmail(email: String, exact: Boolean = true): List<UserRepresentation> {
        return keycloak.realm(realmName).users().search(email, exact)
    }

    /**
     * Enables or disables a user.
     *
     * @param userId Keycloak user ID
     * @param enabled Whether the user should be enabled
     */
    fun setUserEnabled(userId: String, enabled: Boolean) {
        logger.info("Setting user $userId enabled status to: $enabled")

        val userResource = keycloak.realm(realmName).users().get(userId)
        val user = userResource.toRepresentation()

        user.isEnabled = enabled
        userResource.update(user)

        logger.info("User enabled status updated: $userId -> $enabled")
    }
}
