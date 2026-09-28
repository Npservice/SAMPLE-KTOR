package com.example.features.todo

import com.example.core.validation.rules
import kotlinx.serialization.Serializable

@Serializable
data class TodoRequest(
    val title: String = "",
    val description: String? = null,
)

/** Semua field opsional, yang tidak dikirim (null) tidak diubah. */
@Serializable
data class TodoUpdateRequest(
    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = null,
)

/** Waktu dalam ISO UTC, mis. `2026-09-28T05:59:21.953Z`. */
@Serializable
data class TodoResponse(
    val id: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

// Aturan validasi, dijalankan otomatis oleh RequestValidation saat call.receive<...>().

fun TodoRequest.validate() = rules {
    check("title", title.isNotBlank(), "wajib diisi")
    check("title", title.length <= 255, "maksimal 255 karakter")
}

fun TodoUpdateRequest.validate() = rules {
    title?.let {
        check("title", it.isNotBlank(), "tidak boleh kosong")
        check("title", it.length <= 255, "maksimal 255 karakter")
    }
}
