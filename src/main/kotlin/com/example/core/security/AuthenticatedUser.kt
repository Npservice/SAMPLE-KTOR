package com.example.core.security

import com.example.core.exception.UnauthorizedException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

/** Data user yang login (dari JWT payload), disimpan sebagai principal Ktor. */
data class AuthenticatedUser(
    val userId: String,
    val email: String,
    val roles: List<String>,
    val permissions: List<String>,
)

/** Ambil user yang sedang login dari token JWT. */
fun ApplicationCall.currentUser(): AuthenticatedUser =
    principal<AuthenticatedUser>() ?: throw UnauthorizedException()
