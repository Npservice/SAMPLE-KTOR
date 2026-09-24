package com.example.modules.users

import com.example.common.utils.AsiaJakartaTimeSerializer
import com.example.modules.auth.Role
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime

@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    /** Hash password. Tidak pernah ikut ke JSON response dan hanya di-load saat login. */
    @Transient val password: String? = null,
    @Serializable(AsiaJakartaTimeSerializer::class) val createdAt: LocalDateTime,
    @Serializable(AsiaJakartaTimeSerializer::class) val updatedAt: LocalDateTime,
    @Serializable(AsiaJakartaTimeSerializer::class) val deletedAt: LocalDateTime?,
    /** Hanya terisi (dan muncul di JSON) kalau relasinya di-load. */
    val roles: List<Role>? = null,
)
