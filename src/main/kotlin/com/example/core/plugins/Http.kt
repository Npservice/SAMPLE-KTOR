package com.example.core.plugins

import com.example.core.logging.HttpLogging
import com.example.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.request.path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.slf4j.event.Level
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalSerializationApi::class)
val AppJson = Json {
    // Semua field JSON (request & response) memakai snake_case: createdAt -> created_at.
    namingStrategy = JsonNamingStrategy.SnakeCase
    // Field nullable tanpa nilai default (mis. deleted_at) tetap ditulis sebagai null,
    // field opsional dengan default null (mis. relasi yang tidak di-load) tidak ditulis.
    explicitNulls = true
    encodeDefaults = false
}

fun Application.configureHttp(config: AppConfig) {
    // Setiap request diberi request_id (dikirim balik di header X-Request-Id) supaya
    // log request di http.log bisa dicocokkan dengan query-nya di sql.log.
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() && it.length <= 64 }
        replyToHeader(HttpHeaders.XRequestId)
    }

    // Ringkasan satu baris per request di console.
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("request_id")
        filter { call -> call.request.path().startsWith("/api") }
    }

    // Detail request & response (sudah di-redact) di logs/http.log.
    // Body request boleh dibaca ulang (dari cache), supaya HttpLogging bisa mencatatnya.
    install(DoubleReceive)
    install(HttpLogging)

    install(ContentNegotiation) {
        json(AppJson)
    }

    // CORS untuk semua origin, cookie ikut dikirim (credentials).
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    // Rate limit global: THROTTLE_LIMIT request per THROTTLE_TTL detik per IP.
    install(RateLimit) {
        global {
            rateLimiter(limit = config.throttle.limit, refillPeriod = config.throttle.ttlSeconds.seconds)
            requestKey { call -> call.request.origin.remoteAddress }
        }
    }
}
