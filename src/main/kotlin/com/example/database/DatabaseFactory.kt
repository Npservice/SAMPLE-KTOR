package com.example.database

import com.example.config.DatabaseConfig
import com.example.features.file.FileTable
import com.example.features.role.PermissionTable
import com.example.features.role.RoleHasPermissionTable
import com.example.features.role.RoleTable
import com.example.features.todo.TodoTable
import com.example.features.user.UserRoleTable
import com.example.features.user.UserTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.example.core.logging.SqlFileLogger
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val ALL_TABLES = arrayOf(
    UserTable,
    RoleTable,
    PermissionTable,
    UserRoleTable,
    RoleHasPermissionTable,
    FileTable,
    TodoTable,
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
