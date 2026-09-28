package com.example.features.auth

import com.example.core.exception.FieldValidationException
import com.example.core.exception.UnauthorizedException
import com.example.core.security.JwtProvider
import com.example.core.security.PasswordHasher
import com.example.features.user.User
import com.example.features.user.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
) {

    suspend fun register(request: RegisterRequest): AuthResult {
        if (userRepository.existsByEmail(request.email)) throw FieldValidationException(mapOf("email" to "email sudah terdaftar"))

        val user = userRepository.create(request.email, request.name.trim(), PasswordHasher.hash(request.password))
        return toResult(user)
    }

    suspend fun login(request: LoginRequest): AuthResult {
        val user = userRepository.findByEmail(request.email)
        if (user == null || !PasswordHasher.verify(request.password, user.password)) {
            throw UnauthorizedException("Email atau password salah")
        }
        return toResult(user)
    }

    suspend fun refresh(refreshToken: String): AuthResult {
        val user = jwtProvider.verifyRefreshToken(refreshToken)?.subject?.let { userRepository.findById(it) }
            ?: throw UnauthorizedException("Refresh token tidak valid")
        return toResult(user)
    }

    /** Buat access + refresh token beserta role & permission user. */
    private fun toResult(user: User): AuthResult {
        val roles = user.roles.map { it.name }
        val permissions = user.roles.flatMap { role -> role.permissions.map { it.name } }.distinct()

        return AuthResult(
            accessToken = jwtProvider.signAccessToken(user.id, user.email, roles, permissions),
            refreshToken = jwtProvider.signRefreshToken(user.id),
            user = AuthUserResponse(user.id, user.email, user.name, roles, permissions),
        )
    }
}
