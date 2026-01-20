package features.authentication.entity

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.util.UUID

object RefreshTokens : Table("refresh_tokens") {
    val id = uuid("id")
    val token = varchar("token", 500).uniqueIndex()
    val userId = uuid("user_id").references(Users.id)
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
