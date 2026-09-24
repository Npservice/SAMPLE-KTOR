package com.example.modules.auth

import com.example.common.plugins.authorize
import com.example.common.response.success
import com.example.common.validation.receiveValid
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import org.koin.ktor.ext.inject

/** Semua route di sini dipasang di dalam `authenticate("jwt")`. */
fun Route.rolesRoutes() {
    val rolesService by inject<RolesService>()

    authorize(permissions = listOf("roles:read")) {
        get("/roles") {
            call.success(rolesService.findAllRoles())
        }

        get("/permissions") {
            call.success(rolesService.findAllPermissions())
        }

        get("/roles/{id}") {
            call.success(rolesService.findRoleWithPermissions(call.parameters["id"]!!))
        }
    }

    authorize(permissions = listOf("roles:update")) {
        patch("/roles/{id}/permissions") {
            val dto = call.receiveValid<UpdateRolePermissionsDto>(UpdateRolePermissionsDto.rules)
            call.success(
                rolesService.updateRolePermissions(call.parameters["id"]!!, dto),
                "Permission berhasil di update",
            )
        }
    }
}
