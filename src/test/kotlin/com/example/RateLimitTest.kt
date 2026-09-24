package com.example

import com.example.config.AppConfig
import com.example.config.Env
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitTest {
    @Test
    fun `should return 429 envelope after limit exceeded`() = testApplication {
        val config = AppConfig.from(Env(mapOf("THROTTLE_LIMIT" to "2", "THROTTLE_TTL" to "60")))
        application { module(config, connectDatabase = false) }

        repeat(2) { assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users").status) }

        val response = client.get("/api/v1/users")
        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertTrue(response.bodyAsText().contains("ThrottlerException: Too Many Requests"))
    }
}
