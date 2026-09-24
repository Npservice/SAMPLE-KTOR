package com.example.modules.auth

import com.example.common.utils.AsiaJakartaTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Permission(
    val id: String,
    val name: String,
    @Serializable(AsiaJakartaTimeSerializer::class) val deletedAt: LocalDateTime?,
)

@Serializable
data class Role(
    val id: String,
    val name: String,
    @Serializable(AsiaJakartaTimeSerializer::class) val deletedAt: LocalDateTime?,
    /** Hanya terisi (dan muncul di JSON) kalau relasinya di-load. */
    val permissions: List<Permission>? = null,
)
