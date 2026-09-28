package com.example.database.seeds

import com.example.features.role.PermissionTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

val PERMISSION_NAMES = listOf(
    "users:create",
    "users:read",
    "users:update",
    "users:delete",
    "roles:read",
    "roles:update",
)

/** Wajib dipanggil di dalam transaksi. */
fun seedPermissions() {
    for (name in PERMISSION_NAMES) {
        if (!PermissionTable.selectAll().where { PermissionTable.name eq name }.empty()) {
            println("[seed] permission \"$name\" sudah ada, skip")
            continue
        }

        PermissionTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[PermissionTable.name] = name
        }
        println("[seed] permission \"$name\" dibuat")
    }
}
