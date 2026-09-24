package com.example.common.validation

import com.example.common.exceptions.BadRequestException
import com.example.common.logging.rememberRequestBody
import com.example.common.plugins.AppJson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.serializer

private val EMAIL_PATTERN = Regex("""^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$""")
private val UUID_V4_PATTERN = Regex("""^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$""", RegexOption.IGNORE_CASE)

typealias Rules = ValidationRules.() -> Unit

/**
 * Kumpulan aturan validasi untuk body/query. Tiap aturan yang gagal menambah satu pesan error
 * (`email must be an email`, `password must be longer than or equal to 6 characters`, dst).
 */
class ValidationRules internal constructor(private val values: Map<String, JsonElement>) {
    internal val errors = mutableListOf<String>()

    private fun value(field: String): JsonElement? = values[field]?.takeUnless { it is JsonNull }

    private fun stringOf(field: String): String? =
        (value(field) as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    /** Jalankan [rules] hanya kalau field ada dan tidak null. */
    fun optional(field: String, rules: ValidationRules.() -> Unit) {
        if (value(field) != null) rules()
    }

    /** Field wajib berupa string. */
    fun isString(field: String) {
        if (stringOf(field) == null) errors += "$field must be a string"
    }

    /** Field wajib berupa email yang valid. */
    fun isEmail(field: String) {
        val value = stringOf(field)
        if (value == null || !EMAIL_PATTERN.matches(value)) errors += "$field must be an email"
    }

    /** Field wajib string dengan panjang minimal [min] karakter. */
    fun minLength(field: String, min: Int) {
        val value = stringOf(field)
        if (value == null || value.length < min) {
            errors += "$field must be longer than or equal to $min characters"
        }
    }

    /** Field wajib string dengan panjang maksimal [max] karakter. */
    fun maxLength(field: String, max: Int) {
        val value = stringOf(field)
        if (value == null || value.length > max) {
            errors += "$field must be shorter than or equal to $max characters"
        }
    }

    /** Field wajib boolean. Dari query string, "true"/"false" juga diterima. */
    fun isBoolean(field: String) {
        val value = value(field) as? JsonPrimitive
        val valid = value != null && (value.booleanOrNull != null || value.content in setOf("true", "false"))
        if (!valid) errors += "$field must be a boolean value"
    }

    /** Field wajib berupa array. */
    fun isArray(field: String) {
        if (value(field) !is JsonArray) errors += "$field must be an array"
    }

    /** Semua isi array wajib UUID v4. */
    fun eachUuidV4(field: String) {
        val array = value(field) as? JsonArray
        val valid = array != null && array.all { item ->
            (item as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(UUID_V4_PATTERN::matches) == true
        }
        if (!valid) errors += "each value in $field must be a UUID"
    }

    /** Nilai (boleh string angka, mis. dari query) wajib bilangan bulat. */
    fun isInt(field: String) {
        val number = numberOf(field)
        if (number == null || number % 1.0 != 0.0) errors += "$field must be an integer number"
    }

    /** Nilai (boleh string angka, mis. dari query) wajib >= [min]. */
    fun min(field: String, min: Int) {
        val number = numberOf(field)
        if (number == null || number < min) errors += "$field must not be less than $min"
    }

    private fun numberOf(field: String): Double? =
        (value(field) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}

/**
 * Property yang tidak dikenal DTO langsung ditolak, lalu aturan dijalankan.
 * Semua error digabung dengan ", " dan dilempar sebagai 400 Bad Request.
 */
@PublishedApi
internal fun validate(values: Map<String, JsonElement>, allowed: Set<String>, rules: Rules) {
    val result = ValidationRules(values)
    val unknown = values.keys.filterNot { it in allowed }.map { "property $it should not exist" }
    result.rules()
    val errors = unknown + result.errors
    if (errors.isNotEmpty()) throw BadRequestException(errors.joinToString(", "))
}

/** Terima body JSON, validasi, lalu decode ke DTO. */
suspend inline fun <reified T> ApplicationCall.receiveValid(noinline rules: Rules): T {
    val body = receive<JsonElement>() as? JsonObject ?: JsonObject(emptyMap())
    rememberRequestBody(body)
    val serializer = serializer<T>()
    validate(body, jsonFieldNames(serializer.descriptor), rules)
    return AppJson.decodeFromJsonElement(serializer, body)
}

/** Nama field DTO seperti yang tertulis di JSON (sudah melewati naming strategy, mis. snake_case). */
@OptIn(ExperimentalSerializationApi::class)
@PublishedApi
internal fun jsonFieldNames(descriptor: SerialDescriptor): Set<String> {
    val naming = AppJson.configuration.namingStrategy
    return (0 until descriptor.elementsCount).map { index ->
        val name = descriptor.getElementName(index)
        naming?.serialNameForJson(descriptor, index, name) ?: name
    }.toSet()
}

/** Validasi query string. Semua nilai query diperlakukan sebagai string. */
fun ApplicationCall.validateQuery(allowed: Set<String>, rules: Rules): Map<String, String> {
    val query = request.queryParameters.entries().associate { (key, values) -> key to values.first() }
    validate(query.mapValues { JsonPrimitive(it.value) }, allowed, rules)
    return query
}
