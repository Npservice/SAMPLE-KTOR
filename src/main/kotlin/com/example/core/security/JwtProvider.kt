package com.example.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.config.JwtConfig
import java.util.Date
import kotlin.time.Duration

/**
 * Sign & verifikasi JWT (HS256, satu secret untuk access & refresh token).
 * Setiap token membawa claim `type` supaya access token tidak bisa dipakai sebagai refresh token (dan sebaliknya).
 */
class JwtProvider(config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    // Format durasi bawaan Kotlin: "15m", "7d", "1h 30m".
    private val accessTtl = Duration.parse(config.accessExpiresIn)
    val refreshTtl = Duration.parse(config.refreshExpiresIn)

    /** Verifier untuk plugin Authentication: hanya menerima access token. */
    val accessVerifier: JWTVerifier = JWT.require(algorithm).withClaim(TYPE, ACCESS).build()
    private val refreshVerifier: JWTVerifier = JWT.require(algorithm).withClaim(TYPE, REFRESH).build()

    fun signAccessToken(userId: String, email: String, roles: List<String>, permissions: List<String>): String =
        JWT.create()
            .withSubject(userId)
            .withClaim(TYPE, ACCESS)
            .withClaim("email", email)
            .withClaim("roles", roles)
            .withClaim("permissions", permissions)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTtl.inWholeMilliseconds))
            .sign(algorithm)

    fun signRefreshToken(userId: String): String =
        JWT.create()
            .withSubject(userId)
            .withClaim(TYPE, REFRESH)
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTtl.inWholeMilliseconds))
            .sign(algorithm)

    /** Null kalau signature salah, kedaluwarsa, atau bukan refresh token. */
    fun verifyRefreshToken(token: String): DecodedJWT? = runCatching { refreshVerifier.verify(token) }.getOrNull()

    private companion object {
        const val TYPE = "type"
        const val ACCESS = "access"
        const val REFRESH = "refresh"
    }
}
