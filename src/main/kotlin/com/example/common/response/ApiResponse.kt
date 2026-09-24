package com.example.common.response

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/** Bentuk response standar untuk semua endpoint (sukses maupun error). */
@Serializable
data class ApiResponse<T>(
    val status: String,
    val code: Int,
    val message: String,
    val data: T?,
)

/**
 * Kirim response sukses: `{ status: "success", code, message, data }`.
 *
 * Konvensinya: route POST memakai `HttpStatusCode.Created` (201), selain itu 200.
 */
suspend inline fun <reified T> ApplicationCall.success(
    data: T,
    message: String = "Success",
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(status, ApiResponse("success", status.value, message, data))

/** Response sukses dengan `data: null` (mis. logout, delete). */
suspend fun ApplicationCall.successNoData(
    message: String = "Success",
    status: HttpStatusCode = HttpStatusCode.OK,
) = respond(status, ApiResponse<String>("success", status.value, message, null))

/** Kirim response error: `{ status: "error", code, message, data: null }`. */
suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) =
    respond(status, ApiResponse<String>("error", status.value, message, null))
