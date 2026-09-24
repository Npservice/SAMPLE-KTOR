package com.example.modules.users

import com.example.common.utils.nowDateTime
import com.example.database.dbQuery
import com.example.modules.auth.Role
import com.example.modules.auth.RolesTable
import com.example.modules.auth.loadPermissionsByRole
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

class UsersRepository {

    suspend fun findByEmail(email: String): User? = dbQuery {
        findWhere(UsersTable.email eq email)?.toUser()
    }

    /** Termasuk hash password + roles + permissions, dipakai untuk login. */
    suspend fun findByEmailWithPassword(email: String): User? = dbQuery {
        findWhere(UsersTable.email eq email)?.toUser(withPassword = true)?.let { withRelations(listOf(it)).first() }
    }

    suspend fun findOneWithRelations(id: String): User? = dbQuery {
        findWhere(UsersTable.id eq id)?.toUser()?.let { withRelations(listOf(it)).first() }
    }

    suspend fun findAllPaginated(page: Int, size: Int, search: String?): Pair<List<User>, Long> = dbQuery {
        var condition: Op<Boolean> = UsersTable.deletedAt.isNull()
        if (!search.isNullOrEmpty()) {
            val pattern = "%$search%"
            condition = condition and ((UsersTable.name like pattern) or (UsersTable.email like pattern))
        }

        val total = UsersTable.selectAll().where(condition).count()
        val users = UsersTable.selectAll()
            .where(condition)
            .orderBy(UsersTable.createdAt, SortOrder.DESC)
            .limit(size)
            .offset(((page - 1) * size).toLong())
            .map { it.toUser() }

        withRelations(users) to total
    }

    suspend fun create(email: String, name: String, passwordHash: String): User = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = nowDateTime()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.email] = email
            it[UsersTable.name] = name
            it[UsersTable.password] = passwordHash
            it[createdAt] = now
            it[updatedAt] = now
        }
        User(id = id, email = email, name = name, createdAt = now, updatedAt = now, deletedAt = null)
    }

    suspend fun update(id: String, email: String?, name: String?, passwordHash: String?) {
        dbQuery {
            UsersTable.update({ UsersTable.id eq id }) {
                if (email != null) it[UsersTable.email] = email
                if (name != null) it[UsersTable.name] = name
                if (passwordHash != null) it[password] = passwordHash
                it[updatedAt] = nowDateTime()
            }
        }
    }

    /** Soft delete: cuma isi `deletedAt`, datanya tidak dihapus. */
    suspend fun softDelete(id: String) {
        dbQuery {
            UsersTable.update({ UsersTable.id eq id }) { it[deletedAt] = nowDateTime() }
        }
    }

    private fun findWhere(condition: Op<Boolean>): ResultRow? =
        UsersTable.selectAll().where(condition and UsersTable.deletedAt.isNull()).singleOrNull()

    /** Load roles + permissions untuk sekumpulan user (2 query, bukan N+1). Wajib di dalam transaksi. */
    private fun withRelations(users: List<User>): List<User> {
        if (users.isEmpty()) return users

        val roleRows = UserRolesTable
            .join(RolesTable, JoinType.INNER, UserRolesTable.roleId, RolesTable.id)
            .selectAll()
            .where { (UserRolesTable.userId inList users.map { it.id }) and RolesTable.deletedAt.isNull() }
            .map { row ->
                row[UserRolesTable.userId] to Role(
                    id = row[RolesTable.id],
                    name = row[RolesTable.name],
                    deletedAt = row[RolesTable.deletedAt],
                )
            }

        val permissionsByRole = loadPermissionsByRole(roleRows.map { it.second.id }.distinct())
        val rolesByUser = roleRows.groupBy({ it.first }) { (_, role) ->
            role.copy(permissions = permissionsByRole[role.id].orEmpty())
        }

        return users.map { it.copy(roles = rolesByUser[it.id].orEmpty()) }
    }

    private fun ResultRow.toUser(withPassword: Boolean = false) = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        password = if (withPassword) this[UsersTable.password] else null,
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt],
        deletedAt = this[UsersTable.deletedAt],
    )
}
