package com.example.features.user

import com.example.core.response.PageRequest
import com.example.database.dbQuery
import com.example.features.role.RoleTable
import com.example.features.role.toRole
import com.example.features.role.withPermissions
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Semua query otomatis mengabaikan user yang sudah di-soft delete. User selalu di-load beserta role-nya. */
class UserRepository {

    suspend fun search(search: String?, page: PageRequest): Pair<List<User>, Long> = dbQuery {
        var condition: Op<Boolean> = UserTable.deletedAt.isNull()
        if (!search.isNullOrBlank()) {
            condition = condition and ((UserTable.name like "%$search%") or (UserTable.email like "%$search%"))
        }

        val total = UserTable.selectAll().where(condition).count()
        val users = UserTable.selectAll()
            .where(condition)
            .orderBy(UserTable.createdAt, SortOrder.DESC)
            .limit(page.size)
            .offset(page.offset)
            .map { it.toUser() }
        withRoles(users) to total
    }

    suspend fun findById(id: String): User? = findActive(UserTable.id eq id)

    /** Termasuk hash password, dipakai untuk login. */
    suspend fun findByEmail(email: String): User? = findActive(UserTable.email eq email)

    /** Email unik di tabel (termasuk user yang sudah dihapus). [exceptId] untuk cek saat update. */
    suspend fun existsByEmail(email: String, exceptId: String? = null): Boolean = dbQuery {
        var condition: Op<Boolean> = UserTable.email eq email
        if (exceptId != null) condition = condition and (UserTable.id neq exceptId)
        UserTable.selectAll().where(condition).count() > 0
    }

    suspend fun create(email: String, name: String, passwordHash: String): User = dbQuery {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val user = User(UUID.randomUUID().toString(), email, name, passwordHash, now, now)
        UserTable.insert {
            it[id] = user.id
            it[UserTable.email] = email
            it[UserTable.name] = name
            it[password] = passwordHash
            it[createdAt] = now
            it[updatedAt] = now
        }
        user
    }

    /** Field yang null tidak diubah. */
    suspend fun update(id: String, email: String?, name: String?, passwordHash: String?) {
        dbQuery {
            UserTable.update({ UserTable.id eq id }) {
                if (email != null) it[UserTable.email] = email
                if (name != null) it[UserTable.name] = name
                if (passwordHash != null) it[password] = passwordHash
                it[updatedAt] = LocalDateTime.now(ZoneOffset.UTC)
            }
        }
    }

    /** Soft delete: cuma isi `deleted_at`, datanya tidak dihapus. */
    suspend fun softDelete(id: String) {
        dbQuery { UserTable.update({ UserTable.id eq id }) { it[deletedAt] = LocalDateTime.now(ZoneOffset.UTC) } }
    }

    private suspend fun findActive(condition: Op<Boolean>): User? = dbQuery {
        UserTable.selectAll()
            .where(condition and UserTable.deletedAt.isNull())
            .singleOrNull()
            ?.let { withRoles(listOf(it.toUser())).first() }
    }

    /** Lengkapi user dengan role (yang belum dihapus) & permission-nya, tanpa N+1. Wajib di dalam transaksi. */
    private fun withRoles(users: List<User>): List<User> {
        if (users.isEmpty()) return users

        val rows = UserRoleTable
            .join(RoleTable, JoinType.INNER, UserRoleTable.roleId, RoleTable.id)
            .selectAll()
            .where { (UserRoleTable.userId inList users.map { it.id }) and RoleTable.deletedAt.isNull() }
            .map { it[UserRoleTable.userId] to it.toRole() }

        val roles = withPermissions(rows.map { it.second }.distinctBy { it.id }).associateBy { it.id }
        val rolesByUser = rows.groupBy({ it.first }) { roles.getValue(it.second.id) }
        return users.map { it.copy(roles = rolesByUser[it.id].orEmpty()) }
    }

    private fun ResultRow.toUser() = User(
        id = this[UserTable.id],
        email = this[UserTable.email],
        name = this[UserTable.name],
        password = this[UserTable.password],
        createdAt = this[UserTable.createdAt],
        updatedAt = this[UserTable.updatedAt],
    )
}
