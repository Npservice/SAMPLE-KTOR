package com.example.features.role

import com.example.core.response.isSuccess
import com.example.core.response.send
import com.example.core.security.JWT_AUTH
import com.example.core.security.authorize
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/** Modul fitur role: `/api/v1/roles` & `/api/v1/permissions`, wajib login. */
fun Application.roleModule() {
    val roleService by inject<RoleService>()

    routing {
        authenticate(JWT_AUTH) {
            route("/api/v1/roles") {
                install(RequestValidation) {
                    validate<RolePermissionRequest> { it.validate() }
                }

                authorize(permissions = listOf("roles:read")) {
                    get {
                        call.send(roleService.findAll().isSuccess(message = "Berhasil ambil data role"))
                    }
                    get("{id}") {
                        call.send(roleService.findById(call.parameters["id"]!!).isSuccess(message = "Berhasil ambil data role"))
                    }
                }

                authorize(permissions = listOf("roles:update")) {
                    patch("{id}/permissions") {
                        val request = call.receive<RolePermissionRequest>()
                        val data = roleService.updatePermissions(call.parameters["id"]!!, request)
                        call.send(data.isSuccess(message = "Permission role berhasil diperbarui"))
                    }
                }
            }

            route("/api/v1/permissions") {
                authorize(permissions = listOf("roles:read")) {
                    get {
                        call.send(roleService.findAllPermissions().isSuccess(message = "Berhasil ambil data permission"))
                    }
                }
            }
        }
    }
}
