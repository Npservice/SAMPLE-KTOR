package com.example.core.exception

import com.example.core.response.isError
import com.example.core.response.send
import com.example.core.validation.reasonsToErrors
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException as KtorUnsupportedMediaTypeException

/** Semua error berakhir di sini dan dibungkus jadi BaseResponse `{ status: "FAILED", code, message, data }`. */
fun Application.configureErrorHandling() {
    install(StatusPages) {
        // Error validasi per field: data = { field: pesan }.
        exception<FieldValidationException> { call, cause ->
            call.send(cause.errors.isError(cause.status.value, cause.message))
        }
        exception<RequestValidationException> { call, cause ->
            call.send(reasonsToErrors(cause.reasons).isError(HttpStatusCode.BadRequest.value, "Validation Failed"))
        }

        exception<HttpException> { call, cause ->
            call.sendError(cause.status, cause.message)
        }

        // Body JSON rusak / tipe field salah.
        exception<KtorBadRequestException> { call, _ ->
            call.sendError(HttpStatusCode.BadRequest, "Request body tidak valid")
        }
        exception<KtorUnsupportedMediaTypeException> { call, _ ->
            call.sendError(HttpStatusCode.UnsupportedMediaType, "Unsupported Media Type")
        }

        // Error tak terduga: detail dicatat di log, tidak dikirim ke client.
        exception<Throwable> { call, cause ->
            call.application.log.error("${call.request.httpMethod.value} ${call.request.uri} - ${cause.message}", cause)
            call.sendError(HttpStatusCode.InternalServerError, "Internal server error")
        }

        // Route tidak ada. Ktor membalas 405 kalau path-nya ada tapi method-nya beda; keduanya jadi 404.
        status(HttpStatusCode.NotFound, HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.sendError(HttpStatusCode.NotFound, "Endpoint ${call.request.httpMethod.value} ${call.request.uri} tidak ditemukan")
        }

        // Dari plugin RateLimit.
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.sendError(status, "ThrottlerException: Too Many Requests")
        }
    }
}

suspend fun ApplicationCall.sendError(status: HttpStatusCode, message: String?) =
    send((null as String?).isError(status.value, message))
