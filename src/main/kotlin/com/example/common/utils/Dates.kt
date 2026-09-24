package com.example.common.utils

import com.example.config.AppConfig
import com.example.config.Env
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val ISO_MILLIS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

/** Config aplikasi, dibaca di sini untuk mengetahui zona waktu (`APP_TIME`). */
val config: AppConfig =  AppConfig.from(Env.load())

/**
 * Waktu sekarang menurut zona `APP_TIME`.
 *
 * Nilai ini yang disimpan ke kolom `created_at`, `updated_at`, dan `deleted_at`,
 * jadi database menyimpan **jam lokal** (mis. WIB), bukan UTC.
 *
 * `APP_TIME` wajib berupa offset, contoh: `+07:00` (WIB), `+08:00` (WITA), `+09:00` (WIT),
 * atau `Z` (UTC). Nama zona seperti `UTC` atau `Asia/Jakarta` tidak diterima `ZoneOffset.of`
 * dan akan membuat penyimpanan data gagal, termasuk default `"UTC"` di `AppConfig`,
 * jadi pastikan `APP_TIME` selalu diisi di `.env`.
 */
fun nowDateTime(): LocalDateTime = LocalDateTime.now(ZoneOffset.of(config.appTime)).truncatedTo(ChronoUnit.MICROS)

/**
 * Format timestamp untuk response JSON: `2026-09-24T10:15:00.000Z`.
 *
 * Jam yang ditulis adalah jam lokal dari database (sesuai `APP_TIME`), apa adanya.
 * Akhiran `Z` hanya bagian dari format, bukan tanda bahwa jamnya UTC. Client sebaiknya
 * menampilkan nilainya langsung tanpa konversi zona waktu, karena kalau dikonversi
 * (mis. `new Date(...)` di JavaScript) jamnya akan bergeser sebesar offset `APP_TIME`.
 */
object AsiaJakartaTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("AsiaJakartaDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) = encoder.encodeString(value.format(ISO_MILLIS))

    override fun deserialize(decoder: Decoder): LocalDateTime = LocalDateTime.parse(decoder.decodeString(), ISO_MILLIS)
}
