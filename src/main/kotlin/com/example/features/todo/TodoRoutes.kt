package com.example.features.todo

import com.example.core.security.JWT_AUTH
import com.example.core.security.currentUser
import com.example.core.exception.BadRequestException
import com.example.core.response.isSuccess
import com.example.core.response.noData
import com.example.core.response.pageRequest
import com.example.core.response.send
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

/**
 * Modul fitur todo: `/api/v1/todos`, wajib login. Tiap user hanya bisa mengakses todo miliknya sendiri.
 * Service di-inject (Koin), jadi modul ini tidak tahu cara service dibuat.
 */
fun Application.todoModule() {
    val todoService by inject<TodoService>()

    routing {
        authenticate(JWT_AUTH) {
            route("/api/v1/todos") {
                install(RequestValidation) {
                    validate<TodoRequest> { it.validate() }
                    validate<TodoUpdateRequest> { it.validate() }
                }

                get {
                    val query = call.request.queryParameters
                    val data = todoService.findAll(
                        userId = call.currentUser().userId,
                        page = call.pageRequest(),
                        search = query["search"],
                        isCompleted = parseIsCompleted(query["is_completed"]),
                    )
                    call.send(data.isSuccess(message = "Berhasil ambil data todo"))
                }

                get("{id}") {
                    val data = todoService.findById(call.currentUser().userId, call.parameters["id"]!!)
                    call.send(data.isSuccess(message = "Berhasil ambil data todo"))
                }

                post {
                    val request = call.receive<TodoRequest>()
                    val data = todoService.create(call.currentUser().userId, request)
                    call.send(data.isSuccess(201, "Todo berhasil dibuat"))
                }

                patch("{id}") {
                    val request = call.receive<TodoUpdateRequest>()
                    val data = todoService.update(call.currentUser().userId, call.parameters["id"]!!, request)
                    call.send(data.isSuccess(message = "Todo berhasil diperbarui"))
                }

                delete("{id}") {
                    todoService.softDelete(call.currentUser().userId, call.parameters["id"]!!)
                    call.send(noData("Todo berhasil dihapus"))
                }
            }
        }
    }
}

/** Filter status: tidak dikirim = semua, `true` / `false`, selain itu 400. */
private fun parseIsCompleted(value: String?): Boolean? = when (value) {
    null -> null
    "true", "false" -> value.toBooleanStrict()
    else -> throw BadRequestException("is_completed harus true atau false")
}
