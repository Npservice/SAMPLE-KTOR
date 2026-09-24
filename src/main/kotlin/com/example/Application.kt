package com.example

import com.example.common.plugins.JWT_AUTH
import com.example.common.plugins.configureErrorHandling
import com.example.common.plugins.configureHttp
import com.example.common.plugins.configureSecurity
import com.example.common.plugins.configureVirtualThreads
import com.example.config.AppConfig
import com.example.config.CacheService
import com.example.config.Env
import com.example.database.DatabaseFactory
import com.example.modules.auth.authRoutes
import com.example.modules.auth.rolesRoutes
import com.example.modules.files.filesRoutes
import com.example.modules.files.publicFilesRoutes
import com.example.modules.todos.todosRoutes
import com.example.modules.users.usersRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
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

    routing {
        route("/api/v1") {
            // Public, tanpa login
            authRoutes()
            publicFilesRoutes()

            // Wajib login
            authenticate(JWT_AUTH) {
                rolesRoutes()
                usersRoutes()
                filesRoutes()
                todosRoutes()
            }
        }
    }
}
