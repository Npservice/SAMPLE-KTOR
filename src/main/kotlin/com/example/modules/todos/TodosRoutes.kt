package com.example.modules.todos

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

/**
 * Semua route di sini dipasang di dalam `authenticate("jwt")`.
 * Cukup login, tanpa permission khusus: tiap user hanya bisa mengakses todo miliknya sendiri.
 */
fun Route.todosRoutes() {
    val todosService by inject<TodosService>()

    route("/todos") {
        post {
            val dto = call.receiveValid<CreateTodoDto>(CreateTodoDto.rules)
            call.success(
                todosService.create(call.currentUser().userId, dto),
                "Created data successfully",
                HttpStatusCode.Created,
            )
        }

        get {
            val query = call.validateQuery(ListTodosQuery.allowed, ListTodosQuery.rules)
            call.success(todosService.findAll(call.currentUser().userId, ListTodosQuery.from(query)))
        }

        get("{id}") {
            call.success(todosService.findOne(call.currentUser().userId, call.parameters["id"]!!))
        }

        patch("{id}") {
            val dto = call.receiveValid<UpdateTodoDto>(UpdateTodoDto.rules)
            call.success(
                todosService.update(call.currentUser().userId, call.parameters["id"]!!, dto),
                "Updated data successfully",
            )
        }

        delete("{id}") {
            todosService.remove(call.currentUser().userId, call.parameters["id"]!!)
            call.successNoData("Deleted data successfully")
        }
    }
}
