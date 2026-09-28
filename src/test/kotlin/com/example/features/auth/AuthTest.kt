package com.example.features.auth

import com.example.config.AppConfig
import com.example.config.Env
import com.example.core.security.JwtProvider
import com.example.features.user.UserRepository
import com.example.module
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret", "JWT_REFRESH_EXPIRES_IN" to "1d")))
    private val jwt = JwtProvider(config.jwt)
    private val userRepository = mockk<UserRepository>()

    private fun authTest(overrides: org.koin.core.module.Module = module { single { userRepository } }, block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application { module(config, overrides = listOf(overrides), connectDatabase = false) }
            block()
        }

    @Test
    fun `refresh token tidak bisa dipakai sebagai access token`() = authTest {
        val response = client.get("/api/v1/todos") { bearerAuth(jwt.signRefreshToken("u1")) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `access token tidak bisa dipakai sebagai refresh token`() = authTest {
        coEvery { userRepository.findById(any()) } returns null
        val accessToken = jwt.signAccessToken("u1", "a@b.com", emptyList(), emptyList())

        val response = client.patch("/api/v1/auth/refresh") { header(HttpHeaders.Cookie, "refresh_token=$accessToken") }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue("Refresh token tidak valid" in response.bodyAsText())
        coVerify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `umur cookie refresh token mengikuti JWT_REFRESH_EXPIRES_IN`() {
        val authService = mockk<AuthService> {
            coEvery { login(any()) } returns AuthResult("access", jwt.signRefreshToken("u1"), AuthUserResponse("u1", "a@b.com", "A", emptyList(), emptyList()))
        }
        authTest(module { single { authService } }) {
            val response = client.post("/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"email":"a@b.com","password":"rahasia123"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue("Max-Age=86400" in response.headers[HttpHeaders.SetCookie].orEmpty(), response.headers[HttpHeaders.SetCookie])
        }
    }
}
