package com.example.database.seeds

import com.example.features.role.RoleTable
import com.example.features.user.UserRoleTable
import com.example.features.user.UserTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

private const val ADMIN_EMAIL = "admin@example.com"
private const val ADMIN_NAME = "Admin"
const val ADMIN_PASSWORD = "admin12345"

/** Wajib dipanggil di dalam transaksi, setelah [seedRoles]. [passwordHash] = hash dari [ADMIN_PASSWORD]. */
fun seedAdminUser(passwordHash: String) {
    if (!UserTable.selectAll().where { UserTable.email eq ADMIN_EMAIL }.empty()) {
        println("[seed] user \"$ADMIN_EMAIL\" sudah ada, skip")
        return
    }

    val adminRoleId = RoleTable.selectAll().where { RoleTable.name eq "admin" }.singleOrNull()?.get(RoleTable.id)
        ?: error("Role \"admin\" belum ada — jalankan seedRoles() dulu")

    val userId = UUID.randomUUID().toString()
    val now = LocalDateTime.now(ZoneOffset.UTC)
    UserTable.insert {
        it[id] = userId
        it[email] = ADMIN_EMAIL
        it[name] = ADMIN_NAME
        it[password] = passwordHash
        it[createdAt] = now
        it[updatedAt] = now
    }
    UserRoleTable.insert {
        it[UserRoleTable.userId] = userId
        it[roleId] = adminRoleId
    }

    println("[seed] user admin dibuat: $ADMIN_EMAIL / $ADMIN_PASSWORD")
}
