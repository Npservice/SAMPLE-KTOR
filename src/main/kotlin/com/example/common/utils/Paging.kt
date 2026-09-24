package com.example.common.utils

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.max

@Serializable
data class PagingMeta(
    val currentPage: Int,
    val perPage: Int,
    val total: Long,
    val lastPage: Int,
)

@Serializable
data class Paging<T>(
    val items: List<T>,
    val meta: PagingMeta,
)

fun <T> toPaging(items: List<T>, size: Int, page: Int, total: Long) = Paging(
    items = items,
    meta = PagingMeta(
        currentPage = page,
        perPage = size,
        total = total,
        lastPage = max(1, ceil(total.toDouble() / size).toInt()),
    ),
)
