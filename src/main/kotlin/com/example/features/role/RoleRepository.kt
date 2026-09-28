package com.example.features.role

import com.example.database.dbQuery
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

/** Semua query otomatis mengabaikan role & permission yang sudah di-soft delete. */
class RoleRepository {

    suspend fun findAll(): List<Role> = dbQuery {
        val roles = RoleTable.selectAll().where { RoleTable.deletedAt.isNull() }.orderBy(RoleTable.name).map { it.toRole() }
        withPermissions(roles)
    }

    suspend fun findById(id: String): Role? = dbQuery {
        RoleTable.selectAll()
            .where { (RoleTable.id eq id) and RoleTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { withPermissions(listOf(it.toRole())).first() }
    }

    suspend fun findAllPermissions(): List<Permission> = dbQuery {
        PermissionTable.selectAll()
            .where { PermissionTable.deletedAt.isNull() }
            .orderBy(PermissionTable.name, SortOrder.ASC)
            .map { it.toPermission() }
    }

    /** Ganti seluruh permission milik role. ID yang tidak ada / sudah dihapus diabaikan. */
    suspend fun replacePermissions(roleId: String, permissionIds: List<String>) {
        dbQuery {
            val validIds = if (permissionIds.isEmpty()) emptyList() else PermissionTable
                .select(PermissionTable.id)
                .where { (PermissionTable.id inList permissionIds) and PermissionTable.deletedAt.isNull() }
                .map { it[PermissionTable.id] }

            RoleHasPermissionTable.deleteWhere { RoleHasPermissionTable.roleId eq roleId }
            RoleHasPermissionTable.batchInsert(validIds) { permissionId ->
                this[RoleHasPermissionTable.roleId] = roleId
                this[RoleHasPermissionTable.permissionId] = permissionId
            }
        }
    }
}

/**
 * Lengkapi role dengan permission-nya (1 query, bukan N+1). Dipakai juga oleh UserRepository.
 * Wajib dipanggil di dalam transaksi (`dbQuery`).
 */
fun withPermissions(roles: List<Role>): List<Role> {
    if (roles.isEmpty()) return roles

    val permissionsByRole = RoleHasPermissionTable
        .join(PermissionTable, JoinType.INNER, RoleHasPermissionTable.permissionId, PermissionTable.id)
        .selectAll()
        .where { (RoleHasPermissionTable.roleId inList roles.map { it.id }) and PermissionTable.deletedAt.isNull() }
        .groupBy({ it[RoleHasPermissionTable.roleId] }) { it.toPermission() }

    return roles.map { it.copy(permissions = permissionsByRole[it.id].orEmpty()) }
}

internal fun ResultRow.toRole() = Role(id = this[RoleTable.id], name = this[RoleTable.name])

private fun ResultRow.toPermission() = Permission(id = this[PermissionTable.id], name = this[PermissionTable.name])
