package com.example.common.logging

import com.example.config.AppConfig
import com.example.config.Env
import com.example.module
import com.example.modules.auth.AuthResult
import com.example.modules.auth.AuthService
import com.example.modules.auth.AuthUser
import com.example.modules.auth.LoginDto
import com.example.modules.auth.TokenPair
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpLoggingTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret")))

    private val authService = mockk<AuthService> {
        coEvery { login(LoginDto("a@b.com", "rahasia123")) } returns TokenPair(
            accessToken = "access-token-abc",
            refreshToken = "refresh-token-xyz",
            user = AuthUser("u1", "a@b.com", "A", listOf("admin"), listOf("users:read")),
        )
    }

    @Test
    fun `should log request and response with sensitive data redacted`() {
        val logs = captureLogs("http") {
            testApplication {
                application {
                    module(config, overrides = listOf(module { single { authService } }), connectDatabase = false)
                }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Authorization, "Bearer header-token-123")
                    header(HttpHeaders.Cookie, "refresh_token=cookie-token-456")
                    setBody("""{"email":"a@b.com","password":"rahasia123"}""")
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }

        val message = logs.single().formattedMessage
        assertTrue(message.startsWith("POST /api/v1/auth/login -> 201"), message)
        assertTrue("a@b.com" in message, "data biasa tetap tercatat")
        assertTrue(Redactor.MASK in message)
        for (secret in listOf("rahasia123", "access-token-abc", "refresh-token-xyz", "header-token-123", "cookie-token-456")) {
            assertFalse(secret in message, "\"$secret\" bocor ke log:\n$message")
        }
    }
}
