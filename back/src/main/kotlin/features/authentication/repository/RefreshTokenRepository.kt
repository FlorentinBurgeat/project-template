package features.authentication.repository

import features.authentication.entity.RefreshToken
import features.authentication.entity.RefreshTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class RefreshTokenRepository {

    fun save(refreshToken: RefreshToken): RefreshToken {
        return transaction {
            RefreshTokens.insert {
                it[id] = refreshToken.id
                it[token] = refreshToken.token
                it[userId] = refreshToken.userId
                it[expiresAt] = refreshToken.expiresAt
                it[createdAt] = refreshToken.createdAt
            }

            findById(refreshToken.id)!!
        }
    }

    fun findById(id: UUID): RefreshToken? {
        return transaction {
            RefreshTokens.selectAll().where { RefreshTokens.id eq id }
                .map { it.toRefreshToken() }
                .firstOrNull()
        }
    }

    fun findByToken(token: String): RefreshToken? {
        return transaction {
            RefreshTokens.selectAll().where { RefreshTokens.token eq token }
                .map { it.toRefreshToken() }
                .firstOrNull()
        }
    }

    fun findByUserId(userId: UUID): List<RefreshToken> {
        return transaction {
            RefreshTokens.selectAll().where { RefreshTokens.userId eq userId }
                .map { it.toRefreshToken() }
        }
    }

    fun deleteByToken(token: String): Boolean {
        return transaction {
            RefreshTokens.deleteWhere { RefreshTokens.token eq token } > 0
        }
    }

    fun deleteByUserId(userId: UUID): Int {
        return transaction {
            RefreshTokens.deleteWhere { RefreshTokens.userId eq userId }
        }
    }

    fun deleteExpiredTokens(): Int {
        return transaction {
            RefreshTokens.deleteWhere { expiresAt less Instant.now() }
        }
    }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        id = this[RefreshTokens.id],
        token = this[RefreshTokens.token],
        userId = this[RefreshTokens.userId],
        expiresAt = this[RefreshTokens.expiresAt],
        createdAt = this[RefreshTokens.createdAt]
    )
}
