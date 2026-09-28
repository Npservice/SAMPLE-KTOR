package com.example.core.logging

import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

/** Body response yang lebih panjang dari ini dipotong di log. */
private const val MAX_BODY_CHARS = 10_000

private val StartTimeKey = AttributeKey<Long>("HttpLoggingStartTime")
private val RequestBodyKey = AttributeKey<String>("HttpLoggingRequestBody")
private val ResponseBodyKey = AttributeKey<String>("HttpLoggingResponseBody")

/**
 * Catat setiap request & response ke logger `http` (file `logs/http.log`).
 * Data sensitif (password, token, header Authorization/Cookie, dst) disamarkan lewat [Redactor].
 * Body file (upload multipart & download binary) tidak dicatat isinya, hanya jenis & ukurannya.
 */
val HttpLogging = createApplicationPlugin("HttpLogging") {
    val log = LoggerFactory.getLogger("http")

    on(CallSetup) { call ->
        call.attributes.put(StartTimeKey, System.nanoTime())
    }

    on(ResponseBodyReadyForSend) { call, content ->
        call.attributes.put(ResponseBodyKey, describe(content))

        // Body request JSON dibaca ulang dari cache DoubleReceive (sudah dibaca route lewat call.receive).
        if (call.request.contentType().match(ContentType.Application.Json)) {
            runCatching { call.receiveText() }.getOrNull()?.takeIf { it.isNotBlank() }
                ?.let { call.attributes.put(RequestBodyKey, Redactor.redactJson(it)) }
        }
    }

    on(ResponseSent) { call ->
        if (!log.isInfoEnabled) return@on

        val durationMs = call.attributes.getOrNull(StartTimeKey)?.let { (System.nanoTime() - it) / 1_000_000 }
        val request = call.request
        val query = Redactor.redactQuery(request.queryParameters).let { if (it.isEmpty()) "" else "?$it" }
        val requestBody = call.attributes.getOrNull(RequestBodyKey)
            ?: when {
                request.contentType().match(ContentType.MultiPart.Any) -> "[multipart, isi file tidak dicatat]"
                else -> "-"
            }

        log.info(
            buildString {
                append("${request.httpMethod.value} ${request.path()}$query -> ${call.response.status()?.value} (${durationMs} ms)")
                append("\n  request_id      : ${call.callId ?: "-"}")
                append("\n  request headers : ${Redactor.redactHeaders(request.headers)}")
                append("\n  request body    : $requestBody")
                append("\n  response headers: ${Redactor.redactHeaders(call.response.headers.allValues())}")
                append("\n  response body   : ${call.attributes.getOrNull(ResponseBodyKey) ?: "-"}")
            },
        )
    }
}

private fun describe(content: OutgoingContent): String {
    val type = content.contentType
    val isText = type != null && (type.match(ContentType.Application.Json) || type.contentType == "text")

    val text = when {
        content is TextContent -> content.text
        content is OutgoingContent.ByteArrayContent && isText -> content.bytes().decodeToString()
        content is OutgoingContent.ByteArrayContent -> return "[binary ${content.bytes().size} bytes, $type]"
        content is OutgoingContent.NoContent -> return "-"
        else -> return "[stream, $type]"
    }

    val redacted = if (type?.match(ContentType.Application.Json) == true) Redactor.redactJson(text) else text
    return if (redacted.length > MAX_BODY_CHARS) redacted.take(MAX_BODY_CHARS) + "... [dipotong]" else redacted
}
