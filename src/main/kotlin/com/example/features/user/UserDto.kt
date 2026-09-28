package com.example.features.user

import com.example.core.validation.rules
import com.example.features.role.RoleResponse
import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val email: String = "",
    val name: String = "",
    val password: String = "",
)

/** Semua field opsional, yang tidak dikirim (null) tidak diubah. */
@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val name: String? = null,
    val password: String? = null,
)

/** Password tidak pernah ikut di response. Waktu dalam ISO UTC. */
@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<RoleResponse>,
    val createdAt: String,
    val updatedAt: String,
)

private val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

fun UserRequest.validate() = rules {
    check("email", EMAIL.matches(email), "format email tidak valid")
    check("name", name.isNotBlank(), "wajib diisi")
    check("name", name.length <= 255, "maksimal 255 karakter")
    check("password", password.length >= 6, "minimal 6 karakter")
}

fun UserUpdateRequest.validate() = rules {
    email?.let { check("email", EMAIL.matches(it), "format email tidak valid") }
    name?.let {
        check("name", it.isNotBlank(), "tidak boleh kosong")
        check("name", it.length <= 255, "maksimal 255 karakter")
    }
    password?.let { check("password", it.length >= 6, "minimal 6 karakter") }
}
