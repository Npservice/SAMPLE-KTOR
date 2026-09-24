package com.example.modules.auth

import com.example.database.dbQuery
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class RolesRepository {

    suspend fun findAllRoles(): List<Role> = dbQuery {
        RolesTable.selectAll().where { RolesTable.deletedAt.isNull() }.map { it.toRole() }
    }

    suspend fun findAllPermissions(): List<Permission> = dbQuery {
        PermissionsTable.selectAll().where { PermissionsTable.deletedAt.isNull() }.map { it.toPermission() }
    }

    suspend fun findRoleWithPermissions(id: String): Role? = dbQuery {
        RolesTable.selectAll()
            .where { (RolesTable.id eq id) and RolesTable.deletedAt.isNull() }
            .singleOrNull()
            ?.toRole()
            ?.let { it.copy(permissions = loadPermissionsByRole(listOf(it.id))[it.id].orEmpty()) }
    }

    /**
     * Ganti seluruh permission milik role. ID yang tidak ada / sudah dihapus
     * diabaikan tanpa error.
     */
    suspend fun replacePermissions(roleId: String, permissionIds: List<String>) {
        dbQuery {
            val validIds = if (permissionIds.isEmpty()) emptyList() else PermissionsTable
                .select(PermissionsTable.id)
                .where { (PermissionsTable.id inList permissionIds) and PermissionsTable.deletedAt.isNull() }
                .map { it[PermissionsTable.id] }

            RoleHasPermissionTable.deleteWhere { RoleHasPermissionTable.roleId eq roleId }
            RoleHasPermissionTable.batchInsert(validIds) { permissionId ->
                this[RoleHasPermissionTable.roleId] = roleId
                this[RoleHasPermissionTable.permissionId] = permissionId
            }
        }
    }
}

/** Map roleId -> daftar permission (yang belum dihapus). Wajib dipanggil di dalam transaksi. */
fun loadPermissionsByRole(roleIds: List<String>): Map<String, List<Permission>> {
    if (roleIds.isEmpty()) return emptyMap()

    return RoleHasPermissionTable
        .join(PermissionsTable, JoinType.INNER, RoleHasPermissionTable.permissionId, PermissionsTable.id)
        .selectAll()
        .where { (RoleHasPermissionTable.roleId inList roleIds) and PermissionsTable.deletedAt.isNull() }
        .groupBy({ it[RoleHasPermissionTable.roleId] }) { it.toPermission() }
}

internal fun ResultRow.toRole() = Role(
    id = this[RolesTable.id],
    name = this[RolesTable.name],
    deletedAt = this[RolesTable.deletedAt],
)

internal fun ResultRow.toPermission() = Permission(
    id = this[PermissionsTable.id],
    name = this[PermissionsTable.name],
    deletedAt = this[PermissionsTable.deletedAt],
)
