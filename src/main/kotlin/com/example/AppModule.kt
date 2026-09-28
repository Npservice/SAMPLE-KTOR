package com.example

import com.example.config.AppConfig
import com.example.config.CacheService
import com.example.core.security.JwtProvider
import com.example.database.DatabaseFactory
import com.example.features.auth.AuthService
import com.example.features.file.FileRepository
import com.example.features.file.FileService
import com.example.features.role.RoleRepository
import com.example.features.role.RoleService
import com.example.features.todo.TodoRepository
import com.example.features.todo.TodoService
import com.example.features.user.UserRepository
import com.example.features.user.UserService
import org.koin.dsl.module

/** Daftar dependency aplikasi (Koin). Modul fitur cukup `inject<...>()` service-nya. */
fun appModule(config: AppConfig) = module {
    single { config }
    single { DatabaseFactory(config.database) }
    single { CacheService(config.redis) }
    single { JwtProvider(config.jwt) }

    single { RoleRepository() }
    single { RoleService(get()) }

    single { UserRepository() }
    single { UserService(get()) }

    single { AuthService(get(), get()) }

    single { FileRepository() }
    single { FileService(get(), config.files) }

    single { TodoRepository() }
    single { TodoService(get()) }
}
