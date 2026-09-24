package com.example

import com.example.config.AppConfig
import com.example.config.Env
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertEquals

class VirtualThreadsTest {
    @Test
    fun `routes run on virtual threads by default`() = testApplication {
        application {
            module(AppConfig.from(Env(emptyMap())), connectDatabase = false)
            routing {
                get("/thread-check") { call.respondText(Thread.currentThread().isVirtual.toString()) }
            }
        }

        assertEquals("true", client.get("/thread-check").bodyAsText())
    }

    @Test
    fun `request_id is still available in route for sql log`() = testApplication {
        application {
            module(AppConfig.from(Env(emptyMap())), connectDatabase = false)
            routing {
                get("/api/mdc-check") { call.respondText(MDC.get("request_id") ?: "null") }
            }
        }

        val response = client.get("/api/mdc-check") { header(HttpHeaders.XRequestId, "req-abc") }
        assertEquals("req-abc", response.bodyAsText())
    }
}
