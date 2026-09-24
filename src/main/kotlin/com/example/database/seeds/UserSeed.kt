package com.example.database.seeds

import com.example.common.utils.nowDateTime
import com.example.modules.auth.RolesTable
import com.example.modules.users.UserRolesTable
import com.example.modules.users.UsersTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

private const val ADMIN_EMAIL = "admin@example.com"
private const val ADMIN_NAME = "Admin"
const val ADMIN_PASSWORD = "admin12345"

/** Wajib dipanggil di dalam transaksi, setelah [seedRoles]. [passwordHash] = hash dari [ADMIN_PASSWORD]. */
fun seedAdminUser(passwordHash: String) {
    if (!UsersTable.selectAll().where { UsersTable.email eq ADMIN_EMAIL }.empty()) {
        println("[seed] user \"$ADMIN_EMAIL\" sudah ada, skip")
        return
    }

    val adminRoleId = RolesTable.selectAll().where { RolesTable.name eq "admin" }.singleOrNull()?.get(RolesTable.id)
        ?: error("Role \"admin\" belum ada — jalankan seedRoles() dulu")

    val userId = UUID.randomUUID().toString()
    val now = nowDateTime()
    UsersTable.insert {
        it[id] = userId
        it[email] = ADMIN_EMAIL
        it[name] = ADMIN_NAME
        it[password] = passwordHash
        it[createdAt] = now
        it[updatedAt] = now
    }
    UserRolesTable.insert {
        it[UserRolesTable.userId] = userId
        it[roleId] = adminRoleId
    }

    println("[seed] user admin dibuat: $ADMIN_EMAIL / $ADMIN_PASSWORD")
}
