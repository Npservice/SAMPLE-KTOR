package com.example.modules.users

import com.example.modules.auth.RolesTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val password = varchar("password", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Tabel pivot users <-> roles (M:N). */
object UserRolesTable : Table("user_roles") {
    val userId = varchar("user_id", 36)
        .references(UsersTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()
    val roleId = varchar("role_id", 36)
        .references(RolesTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()

    override val primaryKey = PrimaryKey(userId, roleId)
}
