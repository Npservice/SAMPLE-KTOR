package com.example.common.plugins

import com.example.common.exceptions.HttpException
import com.example.common.response.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri

/** Semua error dibungkus jadi `{ status: "error", code, message, data: null }`. */
fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<HttpException> { call, cause ->
            call.respondError(cause.status, cause.message ?: cause.status.description)
        }

        // Body JSON rusak / tidak bisa dibaca.
        exception<BadRequestException> { call, _ ->
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }

        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respondError(HttpStatusCode.UnsupportedMediaType, "Unsupported Media Type")
        }

        // Error tak terduga: log detailnya, tapi jangan bocorkan ke client.
        exception<Throwable> { call, cause ->
            call.application.log.error("${call.request.httpMethod.value} ${call.request.uri} - ${cause.message}", cause)
            call.respondError(HttpStatusCode.InternalServerError, "Internal server error")
        }

        // Route tidak ada. Ktor membalas 405 kalau path-nya ada tapi method-nya beda;
        // di sini keduanya diseragamkan jadi 404.
        status(HttpStatusCode.NotFound, HttpStatusCode.MethodNotAllowed) { call, _ ->
            call.respondError(
                HttpStatusCode.NotFound,
                "Cannot ${call.request.httpMethod.value} ${call.request.uri}",
            )
        }

        // Dari plugin RateLimit.
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respondError(status, "ThrottlerException: Too Many Requests")
        }
    }
}
