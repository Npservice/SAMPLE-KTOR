package com.example.core.response

import com.example.core.exception.BadRequestException
import io.ktor.server.application.ApplicationCall
import kotlinx.serialization.Serializable
import kotlin.math.ceil

/** Query paging `?page=0&size=10` (page mulai dari 0). */
data class PageRequest(val page: Int, val size: Int) {
    val offset: Long get() = page.toLong() * size
}

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val page: Pages,
)

@Serializable
data class Pages(
    val size: Int,
    val number: Int,
    val totalElements: Long,
    val totalPages: Int,
)

fun ApplicationCall.pageRequest(): PageRequest {
    // Tidak dikirim -> default, dikirim tapi bukan angka -> -1 (ditolak di bawah).
    val page = request.queryParameters["page"]?.let { it.toIntOrNull() ?: -1 } ?: 0
    val size = request.queryParameters["size"]?.let { it.toIntOrNull() ?: -1 } ?: 10
    if (page < 0) throw BadRequestException("page tidak valid")
    if (size !in 1..100) throw BadRequestException("size tidak valid")
    return PageRequest(page, size)
}

fun <T> List<T>.toPageResponse(total: Long, request: PageRequest) = PageResponse(
    content = this,
    page = Pages(
        size = request.size,
        number = request.page,
        totalElements = total,
        totalPages = ceil(total.toDouble() / request.size).toInt(),
    ),
)
