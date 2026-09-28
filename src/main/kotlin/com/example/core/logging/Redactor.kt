package com.example.core.logging

import io.ktor.http.Headers
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Menyamarkan data sensitif sebelum ditulis ke log. */
object Redactor {
    const val MASK = "[REDACTED]"

    /** Nama field/header/query yang nilainya selalu disamarkan (tidak peka huruf besar/kecil). */
    private val SENSITIVE_KEYS = setOf(
        "password",
        "access_token",
        "refresh_token",
        "token",
        "secret",
        "signature",
        "authorization",
        "cookie",
        "set-cookie",
    )

    /** Field yang namanya mengandung kata ini juga dianggap sensitif, mis. `new_password`, `api_secret`. */
    private val SENSITIVE_PARTS = listOf("password", "token", "secret")

    fun isSensitive(key: String): Boolean {
        val lower = key.lowercase()
        return lower in SENSITIVE_KEYS || SENSITIVE_PARTS.any { it in lower }
    }

    /** Samarkan nilai field sensitif di JSON, termasuk yang ada di dalam object/array bersarang. */
    fun redact(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (key, value) -> if (isSensitive(key)) JsonPrimitive(MASK) else redact(value) })
        is JsonArray -> JsonArray(element.map(::redact))
        else -> element
    }

    /** Sama seperti [redact], untuk teks JSON. Teks yang bukan JSON dikembalikan apa adanya. */
    fun redactJson(text: String): String =
        runCatching { redact(Json.parseToJsonElement(text)).toString() }.getOrDefault(text)

    fun redactHeaders(headers: Headers): Map<String, String> =
        headers.entries().associate { (name, values) -> name to if (isSensitive(name)) MASK else values.joinToString(", ") }

    fun redactQuery(parameters: Parameters): String =
        parameters.entries().joinToString("&") { (name, values) ->
            values.joinToString("&") { value -> "$name=${if (isSensitive(name)) MASK else value}" }
        }
}
