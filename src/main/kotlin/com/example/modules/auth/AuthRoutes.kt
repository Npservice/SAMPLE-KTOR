package com.example.modules.auth

import com.example.common.exceptions.UnauthorizedException
import com.example.common.response.success
import com.example.common.response.successNoData
import com.example.common.validation.receiveValid
import com.example.config.AppConfig
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

private const val REFRESH_TOKEN_COOKIE = "refresh_token"
private const val REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 60 * 60

/** Semua route auth bersifat public, jadi dipasang di luar `authenticate`. */
fun Route.authRoutes() {
    val authService by inject<AuthService>()
    val config by inject<AppConfig>()

    fun ApplicationCall.setRefreshTokenCookie(token: String) {
        response.cookies.append(
            Cookie(
                name = REFRESH_TOKEN_COOKIE,
                value = token,
                maxAge = REFRESH_TOKEN_MAX_AGE_SECONDS,
                path = "/",
                httpOnly = true,
                secure = config.isProduction,
                extensions = mapOf("SameSite" to "Strict"),
            ),
        )
    }

    route("/auth") {
        post("/register") {
            val dto = call.receiveValid<RegisterDto>(RegisterDto.rules)
            val result = authService.register(dto)
            call.setRefreshTokenCookie(result.refreshToken)
            call.success(result.toResult(), "Registered successfully", HttpStatusCode.Created)
        }

        post("/login") {
            val dto = call.receiveValid<LoginDto>(LoginDto.rules)
            val result = authService.login(dto)
            call.setRefreshTokenCookie(result.refreshToken)
            call.success(result.toResult(), "Login success", HttpStatusCode.Created)
        }

        patch("/refresh") {
            val token = call.request.cookies[REFRESH_TOKEN_COOKIE]
                ?: throw UnauthorizedException("Refresh token not found")

            val result = authService.refreshToken(token)
            call.setRefreshTokenCookie(result.refreshToken)
            call.success(result.toResult(), "Refresh success")
        }

        post("/logout") {
            call.response.cookies.append(
                Cookie(
                    name = REFRESH_TOKEN_COOKIE,
                    value = "",
                    maxAge = 0,
                    path = "/",
                    httpOnly = true,
                    extensions = mapOf("SameSite" to "Strict"),
                ),
            )
            call.successNoData("Logout success", HttpStatusCode.Created)
        }
    }
}
