package com.example.features.user

import com.example.features.role.Role
import com.example.features.role.RoleTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

/** Tabel `users`. Semua kolom waktu berisi UTC. */
object UserTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val password = varchar("password", 255)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Pivot users <-> roles (M:N). */
object UserRoleTable : Table("user_roles") {
    val userId = varchar("user_id", 36)
        .references(UserTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()
    val roleId = varchar("role_id", 36)
        .references(RoleTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()

    override val primaryKey = PrimaryKey(userId, roleId)
}

/** Satu baris tabel `users` beserta role-nya (data internal, bukan response). */
data class User(
    val id: String,
    val email: String,
    val name: String,
    /** Hash password, hanya dipakai untuk login. */
    val password: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val roles: List<Role> = emptyList(),
)
