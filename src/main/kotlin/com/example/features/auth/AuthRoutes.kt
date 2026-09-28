package com.example.features.auth

import com.example.config.AppConfig
import com.example.core.exception.UnauthorizedException
import com.example.core.response.isSuccess
import com.example.core.response.noData
import com.example.core.response.send
import com.example.core.security.JwtProvider
import io.ktor.http.Cookie
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.request.receive
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

private const val REFRESH_TOKEN_COOKIE = "refresh_token"

/** Modul fitur auth: `/api/v1/auth`, public (tanpa login). */
fun Application.authModule() {
    val authService by inject<AuthService>()
    val jwtProvider by inject<JwtProvider>()
    val config by inject<AppConfig>()

    /** Refresh token lewat cookie httpOnly, umurnya sama dengan refresh token (JWT_REFRESH_EXPIRES_IN). */
    fun ApplicationCall.setRefreshCookie(token: String, maxAgeSeconds: Long) {
        response.cookies.append(
            Cookie(
                name = REFRESH_TOKEN_COOKIE,
                value = token,
                maxAge = maxAgeSeconds.toInt(),
                path = "/",
                httpOnly = true,
                secure = config.isProduction,
                extensions = mapOf("SameSite" to "Strict"),
            ),
        )
    }

    suspend fun ApplicationCall.sendAuth(result: AuthResult, message: String, code: Int = 200) {
        setRefreshCookie(result.refreshToken, jwtProvider.refreshTtl.inWholeSeconds)
        send(AuthResponse(result.accessToken, result.user).isSuccess(code, message))
    }

    routing {
        route("/api/v1/auth") {
            install(RequestValidation) {
                validate<LoginRequest> { it.validate() }
                validate<RegisterRequest> { it.validate() }
            }

            post("/register") {
                call.sendAuth(authService.register(call.receive<RegisterRequest>()), "Registrasi berhasil", 201)
            }

            post("/login") {
                call.sendAuth(authService.login(call.receive<LoginRequest>()), "Login berhasil")
            }

            patch("/refresh") {
                val token = call.request.cookies[REFRESH_TOKEN_COOKIE]
                    ?: throw UnauthorizedException("Refresh token tidak ditemukan")
                call.sendAuth(authService.refresh(token), "Refresh token berhasil")
            }

            post("/logout") {
                call.setRefreshCookie("", 0)
                call.send(noData("Logout berhasil"))
            }
        }
    }
}
