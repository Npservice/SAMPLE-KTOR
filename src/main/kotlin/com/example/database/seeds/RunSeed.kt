package com.example.database.seeds

import com.example.common.security.PasswordHasher
import com.example.config.AppConfig
import com.example.config.Env
import com.example.database.DatabaseFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.system.exitProcess

/** Jalankan: `./gradlew seed`. Aman dijalankan berkali-kali (idempotent). */
fun main() {
    val config = AppConfig.from(Env.load())
    val factory = DatabaseFactory(config.database)

    try {
        val database = factory.connect(createSchema = !config.isProduction)
        println("[seed] koneksi database tersambung")

        val adminPasswordHash = runBlocking { PasswordHasher.hash(ADMIN_PASSWORD) }
        transaction(database) {
            seedPermissions()
            seedRoles()
            seedAdminUser(adminPasswordHash)
        }
        println("[seed] selesai")
    } catch (e: Exception) {
        System.err.println("[seed] gagal: ${e.message}")
        exitProcess(1)
    } finally {
        factory.close()
    }
}
