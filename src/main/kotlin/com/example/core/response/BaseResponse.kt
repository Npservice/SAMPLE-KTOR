package com.example.core.response

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/** Bentuk response standar semua endpoint. */
@Serializable
data class BaseResponse<T>(
    val status: String,
    val code: Int,
    val message: String?,
    val data: T?,
)

fun <T> T.isSuccess(code: Int = 200, message: String) = BaseResponse("OK", code, message, this)

fun <T> T.isError(code: Int, message: String?) = BaseResponse("FAILED", code, message, this)

/** Response sukses tanpa data (mis. delete, logout). */
fun noData(message: String, code: Int = 200) = (null as String?).isSuccess(code, message)

/** Kirim BaseResponse, status HTTP mengikuti `body.code`. */
suspend inline fun <reified T> ApplicationCall.send(body: BaseResponse<T>) =
    respond(HttpStatusCode.fromValue(body.code), body)
