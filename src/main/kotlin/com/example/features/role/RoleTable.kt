package com.example.features.role

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object RoleTable : Table("roles") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object PermissionTable : Table("permissions") {
    val id = varchar("id", 36)
    val name = varchar("name", 255).uniqueIndex()
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Pivot roles <-> permissions (M:N). */
object RoleHasPermissionTable : Table("role_has_permission") {
    val roleId = varchar("role_id", 36)
        .references(RoleTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()
    val permissionId = varchar("permission_id", 36)
        .references(PermissionTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()

    override val primaryKey = PrimaryKey(roleId, permissionId)
}

data class Permission(val id: String, val name: String)

data class Role(val id: String, val name: String, val permissions: List<Permission> = emptyList())
