package com.example.modules.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.example.common.utils.parseDuration
import com.example.config.JwtConfig
import java.util.Date

/** Sign & verifikasi JWT (HS256, satu secret untuk access & refresh token). */
class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)
    private val accessTtl = parseDuration(config.accessExpiresIn)
    private val refreshTtl = parseDuration(config.refreshExpiresIn)

    val verifier: JWTVerifier = JWT.require(algorithm).build()

    fun signAccessToken(userId: String, email: String, roles: List<String>, permissions: List<String>): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withSubject(userId)
            .withClaim("email", email)
            .withClaim("roles", roles)
            .withClaim("permissions", permissions)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + accessTtl.inWholeMilliseconds))
            .sign(algorithm)
    }

    fun signRefreshToken(userId: String): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withSubject(userId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + refreshTtl.inWholeMilliseconds))
            .sign(algorithm)
    }

    /** Null kalau signature salah / token kedaluwarsa. */
    fun verify(token: String): DecodedJWT? = runCatching { verifier.verify(token) }.getOrNull()
}
