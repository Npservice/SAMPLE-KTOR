package com.example.modules.auth

import com.example.common.validation.Rules
import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(
    val email: String,
    val password: String,
) {
    companion object {
        val rules: Rules = {
            isEmail("email")
            isString("password")
        }
    }
}

@Serializable
data class RegisterDto(
    val email: String,
    val name: String,
    val password: String,
) {
    companion object {
        val rules: Rules = {
            isEmail("email")
            isString("name")
            minLength("password", 6)
            isString("password")
        }
    }
}

@Serializable
data class UpdateRolePermissionsDto(
    val permissionIds: List<String>,
) {
    companion object {
        val rules: Rules = {
            eachUuidV4("permission_ids")
            isArray("permission_ids")
        }
    }
}

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val roles: List<String>,
    val permissions: List<String>,
)

/** Body response login/register/refresh. Refresh token tidak dikirim di body, tapi via cookie. */
@Serializable
data class AuthResult(
    val accessToken: String,
    val user: AuthUser,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser,
) {
    fun toResult() = AuthResult(accessToken, user)
}
