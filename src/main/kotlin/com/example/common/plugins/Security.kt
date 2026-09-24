package com.example.common.plugins

import com.example.common.response.respondError
import com.example.common.security.AuthenticatedUser
import com.example.modules.auth.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

const val JWT_AUTH = "jwt"

/** Autentikasi JWT: Bearer token di header `Authorization`. */
fun Application.configureSecurity(jwtService: JwtService) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            verifier(jwtService.verifier)
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
                call.respondError(HttpStatusCode.Unauthorized, "Unauthorized")
            }
        }
    }
}
