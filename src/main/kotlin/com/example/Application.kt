package com.example

import com.example.config.AppConfig
import com.example.config.CacheService
import com.example.config.Env
import com.example.core.exception.configureErrorHandling
import com.example.core.plugins.configureHttp
import com.example.core.plugins.configureVirtualThreads
import com.example.core.security.configureSecurity
import com.example.database.DatabaseFactory
import com.example.features.auth.authModule
import com.example.features.file.fileModule
import com.example.features.role.roleModule
import com.example.features.todo.todoModule
import com.example.features.user.userModule
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.module.Module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val config = AppConfig.from(Env.load())
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

/**
 * @param overrides modul Koin tambahan (dipakai test untuk mengganti service dengan mock).
 * @param connectDatabase false di test yang tidak butuh database.
 */
fun Application.module(
    config: AppConfig,
    overrides: List<Module> = emptyList(),
    connectDatabase: Boolean = true,
) {
    install(Koin) {
        slf4jLogger()
        allowOverride(true)
        modules(listOf(appModule(config)) + overrides)
    }

    if (connectDatabase) {
        val database = get<DatabaseFactory>()
        database.connect(createSchema = !config.isProduction)
        monitor.subscribe(ApplicationStopped) {
            database.close()
            get<CacheService>().close()
        }
    }

    configureVirtualThreads()
    configureHttp(config)
    configureErrorHandling()
    configureSecurity(get())

    // Modul per fitur (features/<nama>/<Nama>Routes.kt). Fitur baru cukup tambah satu baris di sini.
    authModule()
    roleModule()
    userModule()
    fileModule()
    todoModule()
}
