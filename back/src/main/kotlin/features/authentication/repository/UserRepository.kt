package features.authentication.repository

import features.authentication.entity.User
import features.authentication.entity.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class UserRepository {

    fun save(user: User): User {
        return transaction {
            Users.insert {
                it[id] = user.id
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[createdAt] = user.createdAt
                it[updatedAt] = user.updatedAt
            }

            findById(user.id)!!
        }
    }

    fun findById(id: UUID): User? {
        return transaction {
            Users.selectAll().where { Users.id eq id }
                .map { it.toUser() }
                .firstOrNull()
        }
    }

    fun findByEmail(email: String): User? {
        return transaction {
            Users.selectAll().where { Users.email eq email }
                .map { it.toUser() }
                .firstOrNull()
        }
    }

    fun existsByEmail(email: String): Boolean {
        return transaction {
            Users.selectAll().where { Users.email eq email }
                .count() > 0
        }
    }

    fun update(user: User): User? {
        return transaction {
            Users.update({ Users.id eq user.id }) {
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[updatedAt] = Instant.now()
            }
            findById(user.id)
        }
    }

    fun deleteById(id: UUID): Boolean {
        return transaction {
            Users.deleteWhere { Users.id eq id } > 0
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[Users.id],
        email = this[Users.email],
        name = this[Users.name],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt],
        updatedAt = this[Users.updatedAt]
    )
}
