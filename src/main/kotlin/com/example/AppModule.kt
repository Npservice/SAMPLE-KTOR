package com.example

import com.example.config.AppConfig
import com.example.config.CacheService
import com.example.database.DatabaseFactory
import com.example.modules.auth.AuthService
import com.example.modules.auth.JwtService
import com.example.modules.auth.RolesRepository
import com.example.modules.auth.RolesService
import com.example.modules.files.FilesRepository
import com.example.modules.files.FilesService
import com.example.modules.todos.TodosRepository
import com.example.modules.todos.TodosService
import com.example.modules.users.UsersRepository
import com.example.modules.users.UsersService
import org.koin.dsl.module

/** Daftar dependency aplikasi (Koin). */
fun appModule(config: AppConfig) = module {
    single { config }
    single { DatabaseFactory(config.database) }
    single { CacheService(config.redis) }
    single { JwtService(config.jwt) }

    // users
    single { UsersRepository() }
    single { UsersService(get()) }

    // auth
    single { RolesRepository() }
    single { RolesService(get()) }
    single { AuthService(get(), get()) }

    // files
    single { FilesRepository() }
    single { FilesService(get(), config.files) }

    // todos
    single { TodosRepository() }
    single { TodosService(get()) }
}
