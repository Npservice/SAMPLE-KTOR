package com.example.features.user

import com.example.core.response.isSuccess
import com.example.core.response.noData
import com.example.core.response.pageRequest
import com.example.core.response.send
import com.example.core.security.JWT_AUTH
import com.example.core.security.authorize
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/** Modul fitur user: `/api/v1/users`, wajib login + role/permission. */
fun Application.userModule() {
    val userService by inject<UserService>()

    routing {
        authenticate(JWT_AUTH) {
            route("/api/v1/users") {
                install(RequestValidation) {
                    validate<UserRequest> { it.validate() }
                    validate<UserUpdateRequest> { it.validate() }
                }

                authorize(permissions = listOf("users:read")) {
                    get {
                        val data = userService.findAll(call.pageRequest(), call.request.queryParameters["search"])
                        call.send(data.isSuccess(message = "Berhasil ambil data user"))
                    }
                    get("{id}") {
                        call.send(userService.findById(call.parameters["id"]!!).isSuccess(message = "Berhasil ambil data user"))
                    }
                }

                authorize(roles = listOf("admin"), permissions = listOf("users:create")) {
                    post {
                        val request = call.receive<UserRequest>()
                        call.send(userService.create(request).isSuccess(201, "User berhasil dibuat"))
                    }
                }

                authorize(roles = listOf("admin"), permissions = listOf("users:update")) {
                    patch("{id}") {
                        val request = call.receive<UserUpdateRequest>()
                        call.send(userService.update(call.parameters["id"]!!, request).isSuccess(message = "User berhasil diperbarui"))
                    }
                }

                authorize(roles = listOf("admin"), permissions = listOf("users:delete")) {
                    delete("{id}") {
                        userService.softDelete(call.parameters["id"]!!)
                        call.send(noData("User berhasil dihapus"))
                    }
                }
            }
        }
    }
}
