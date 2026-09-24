package com.example.common.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val DURATION_PATTERN = Regex("""^(\d+(?:\.\d+)?)\s*(ms|s|m|h|d|w|y)?$""", RegexOption.IGNORE_CASE)

/**
 * Parse durasi seperti "15m", "7d", "1h", "30s" (satuan: ms, s, m, h, d, w, y).
 * Angka tanpa satuan dianggap milidetik.
 */
fun parseDuration(value: String): Duration {
    val match = DURATION_PATTERN.matchEntire(value.trim())
        ?: throw IllegalArgumentException("Format durasi tidak valid: \"$value\"")
    val amount = match.groupValues[1].toDouble()
    return when (match.groupValues[2].lowercase()) {
        "", "ms" -> amount.milliseconds
        "s" -> amount.seconds
        "m" -> amount.minutes
        "h" -> amount.hours
        "d" -> amount.days
        "w" -> (amount * 7).days
        "y" -> (amount * 365.25).days
        else -> error("unreachable")
    }
}
