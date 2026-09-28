package com.example.core.validation

import io.ktor.server.plugins.requestvalidation.ValidationResult

/**
 * Aturan validasi request untuk plugin RequestValidation (bawaan Ktor).
 * Pesan pertama per field yang dipakai; error dikirim sebagai `{ field: pesan }`.
 *
 * ```
 * fun TodoRequest.validate() = rules {
 *     check("title", title.isNotBlank(), "wajib diisi")
 * }
 * ```
 */
class FieldRules {
    internal val errors = linkedMapOf<String, String>()

    fun check(field: String, valid: Boolean, message: String) {
        if (!valid) errors.putIfAbsent(field, message)
    }
}

fun rules(block: FieldRules.() -> Unit): ValidationResult {
    val errors = FieldRules().apply(block).errors
    return if (errors.isEmpty()) ValidationResult.Valid
    else ValidationResult.Invalid(errors.map { (field, message) -> "$field$SEPARATOR$message" })
}

/** Kebalikan dari [rules]: reason `field: pesan` -> map untuk response. */
fun reasonsToErrors(reasons: List<String>): Map<String, String> =
    reasons.associate { reason ->
        val field = reason.substringBefore(SEPARATOR, "request")
        field to reason.substringAfter(SEPARATOR)
    }

private const val SEPARATOR = ": "
