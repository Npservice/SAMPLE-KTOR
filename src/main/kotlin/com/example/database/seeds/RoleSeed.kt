package com.example.database.seeds

import com.example.features.role.PermissionTable
import com.example.features.role.RoleHasPermissionTable
import com.example.features.role.RoleTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

private val ROLE_PERMISSIONS = mapOf(
    "admin" to listOf("users:create", "users:read", "users:update", "users:delete", "roles:read", "roles:update"),
    "editor" to listOf("users:read", "users:update"),
    "user" to listOf("users:read"),
)

/** Wajib dipanggil di dalam transaksi, setelah [seedPermissions]. */
fun seedRoles() {
    for ((name, permissionNames) in ROLE_PERMISSIONS) {
        val permissions = PermissionTable.selectAll()
            .where { PermissionTable.name inList permissionNames }
            .associate { it[PermissionTable.id] to it[PermissionTable.name] }

        val existing = RoleTable.selectAll().where { RoleTable.name eq name }.singleOrNull()
        val roleId = existing?.get(RoleTable.id) ?: UUID.randomUUID().toString()

        if (existing == null) {
            RoleTable.insert {
                it[id] = roleId
                it[RoleTable.name] = name
            }
        }

        val current = RoleHasPermissionTable.selectAll()
            .where { RoleHasPermissionTable.roleId eq roleId }
            .map { it[RoleHasPermissionTable.permissionId] }
            .toSet()
        val missing = permissions.filterKeys { it !in current }

        RoleHasPermissionTable.batchInsert(missing.keys) { permissionId ->
            this[RoleHasPermissionTable.roleId] = roleId
            this[RoleHasPermissionTable.permissionId] = permissionId
        }

        when {
            existing == null ->
                println("[seed] role \"$name\" dibuat dengan permission: ${permissionNames.joinToString(", ")}")
            missing.isEmpty() ->
                println("[seed] role \"$name\" sudah ada & permission lengkap, skip")
            else ->
                println("[seed] role \"$name\" ditambah permission: ${missing.values.joinToString(", ")}")
        }
    }
}
