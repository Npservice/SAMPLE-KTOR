package com.example.database

import com.example.config.DatabaseConfig
import com.example.modules.auth.PermissionsTable
import com.example.modules.auth.RoleHasPermissionTable
import com.example.modules.auth.RolesTable
import com.example.modules.files.FilesTable
import com.example.modules.todos.TodosTable
import com.example.modules.users.UserRolesTable
import com.example.modules.users.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.example.common.logging.SqlFileLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val ALL_TABLES = arrayOf(
    UsersTable,
    RolesTable,
    PermissionsTable,
    UserRolesTable,
    RoleHasPermissionTable,
    FilesTable,
    TodosTable,
)

class DatabaseFactory(private val config: DatabaseConfig) {
    private lateinit var dataSource: HikariDataSource

    fun connect(createSchema: Boolean): Database {
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            maximumPoolSize = 10
        })
        val database = Database.connect(dataSource)

        // Bikin tabel yang belum ada (non-production saja). Di production pakai migration.
        if (createSchema) {
            transaction(database) { SchemaUtils.create(*ALL_TABLES) }
        }
        return database
    }

    fun close() {
        if (::dataSource.isInitialized) dataSource.close()
    }
}

/**
 * Jalankan query Exposed dalam satu transaksi. Setiap query dicatat ke `logs/sql.log`.
 *
 * Tidak perlu pindah thread: request sudah berjalan di virtual thread (lihat `configureVirtualThreads`),
 * jadi JDBC yang blocking aman dipanggil langsung.
 */
suspend fun <T> dbQuery(block: () -> T): T = transaction {
    addLogger(SqlFileLogger)
    block()
}
