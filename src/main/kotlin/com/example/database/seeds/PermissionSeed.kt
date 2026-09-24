package com.example.database.seeds

import com.example.modules.auth.PermissionsTable
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
        if (!PermissionsTable.selectAll().where { PermissionsTable.name eq name }.empty()) {
            println("[seed] permission \"$name\" sudah ada, skip")
            continue
        }

        PermissionsTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[PermissionsTable.name] = name
        }
        println("[seed] permission \"$name\" dibuat")
    }
}
