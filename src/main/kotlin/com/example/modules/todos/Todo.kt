package com.example.modules.todos

import com.example.common.utils.AsiaJakartaTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Todo(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean,
    /** Waktu todo ditandai selesai. Kembali null kalau status selesai dibatalkan. */
    @Serializable(AsiaJakartaTimeSerializer::class) val completedAt: LocalDateTime?,
    @Serializable(AsiaJakartaTimeSerializer::class) val createdAt: LocalDateTime,
    @Serializable(AsiaJakartaTimeSerializer::class) val updatedAt: LocalDateTime,
    @Serializable(AsiaJakartaTimeSerializer::class) val deletedAt: LocalDateTime?,
)
