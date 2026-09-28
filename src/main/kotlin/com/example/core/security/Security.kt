package com.example.core.security

import com.example.core.exception.sendError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

const val JWT_AUTH = "jwt"

/** Autentikasi JWT: `Authorization: Bearer <access token>`. Refresh token ditolak di sini. */
fun Application.configureSecurity(jwtProvider: JwtProvider) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            verifier(jwtProvider.accessVerifier)
            validate { credential ->
                val payload = credential.payload
                payload.subject?.let { userId ->
                    AuthenticatedUser(
                        userId = userId,
                        email = payload.getClaim("email").asString().orEmpty(),
                        roles = payload.getClaim("roles").asList(String::class.java).orEmpty(),
                        permissions = payload.getClaim("permissions").asList(String::class.java).orEmpty(),
                    )
                }
            }
            challenge { _, _ ->
                call.sendError(HttpStatusCode.Unauthorized, "Unauthorized")
            }
        }
    }
}
