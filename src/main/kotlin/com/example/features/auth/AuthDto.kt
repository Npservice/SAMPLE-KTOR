package com.example.features.auth

import com.example.core.validation.rules
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class RegisterRequest(
    val email: String = "",
    val name: String = "",
    val password: String = "",
)

@Serializable
data class AuthUserResponse(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String>,
    val permissions: List<String>,
)

/** Dikirim di body. Refresh token dikirim terpisah lewat cookie httpOnly. */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val user: AuthUserResponse,
)

/** Hasil login/register/refresh di service (route yang memisahkan refresh token ke cookie). */
data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserResponse,
)

private val EMAIL = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

fun LoginRequest.validate() = rules {
    check("email", EMAIL.matches(email), "format email tidak valid")
    check("password", password.isNotEmpty(), "wajib diisi")
}

fun RegisterRequest.validate() = rules {
    check("email", EMAIL.matches(email), "format email tidak valid")
    check("name", name.isNotBlank(), "wajib diisi")
    check("name", name.length <= 255, "maksimal 255 karakter")
    check("password", password.length >= 6, "minimal 6 karakter")
}
