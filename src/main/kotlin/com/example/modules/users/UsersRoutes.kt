package com.example.modules.users

import com.example.common.plugins.authorize
import com.example.common.response.success
import com.example.common.response.successNoData
import com.example.common.security.currentUser
import com.example.common.validation.receiveValid
import com.example.common.validation.validateQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/** Semua route di sini dipasang di dalam `authenticate("jwt")`. */
fun Route.usersRoutes() {
    val usersService by inject<UsersService>()

    route("/users") {
        authorize(roles = listOf("admin"), permissions = listOf("users:create")) {
            post {
                val dto = call.receiveValid<CreateUserDto>(CreateUserDto.rules)
                call.success(usersService.create(dto), "Created data successfully", HttpStatusCode.Created)
            }
        }

        authorize(permissions = listOf("users:read")) {
            get {
                val query = call.validateQuery(ListUsersQuery.allowed, ListUsersQuery.rules)
                call.success(usersService.findAll(ListUsersQuery.from(query)))
            }

            get("{id}") {
                call.success(usersService.findOne(call.parameters["id"]!!))
            }
        }

        authorize(roles = listOf("admin"), permissions = listOf("users:update")) {
            patch("{id}") {
                val dto = call.receiveValid<UpdateUserDto>(UpdateUserDto.rules)
                call.success(usersService.update(call.parameters["id"]!!, dto), "Updated data successfully")
            }
        }

        authorize(roles = listOf("admin"), permissions = listOf("users:delete")) {
            delete("{id}") {
                usersService.remove(call.parameters["id"]!!)
                call.successNoData("Deleted data successfully")
            }
        }
    }
}
