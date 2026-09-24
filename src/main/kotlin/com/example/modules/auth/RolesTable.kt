package com.example.modules.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object RolesTable : Table("roles") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object PermissionsTable : Table("permissions") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Tabel pivot roles <-> permissions (M:N). */
object RoleHasPermissionTable : Table("role_has_permission") {
    val roleId = varchar("role_id", 36)
        .references(RolesTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()
    val permissionId = varchar("permission_id", 36)
        .references(PermissionsTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()

    override val primaryKey = PrimaryKey(roleId, permissionId)
}
