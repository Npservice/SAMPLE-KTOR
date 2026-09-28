package com.example.features.user

import com.example.config.AppConfig
import com.example.config.Env
import com.example.core.exception.FieldValidationException
import com.example.core.security.JwtProvider
import com.example.module
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Unit test service: repository di-mock, tidak butuh database. */
class UserServiceTest {
    private val repository = mockk<UserRepository>(relaxUnitFun = true)
    private val service = UserService(repository)

    @Test
    fun `create - email sudah dipakai - error di field email`() {
        runBlocking {
            coEvery { repository.existsByEmail("jane@example.com") } returns true

            val error = assertFailsWith<FieldValidationException> {
                service.create(UserRequest("jane@example.com", "Jane", "secret123"))
            }
            assertEquals(mapOf("email" to "email sudah terdaftar"), error.errors)
            coVerify(exactly = 0) { repository.create(any(), any(), any()) }
        }
    }
}

/** Test route: cek login, role & permission, dan validasi. */
class UserRoutesTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret")))
    private val jwt = JwtProvider(config.jwt)
    private val service = mockk<UserService>()

    private fun userTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application { module(config, overrides = listOf(module { single { service } }), connectDatabase = false) }
        block()
    }

    private fun String.json() = Json.parseToJsonElement(this).jsonObject
    private val body = """{"email":"jane@example.com","name":"Jane","password":"secret123"}"""

    @Test
    fun `tanpa token - 401`() = userTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/users").status)
    }

    @Test
    fun `bukan admin - 403`() = userTest {
        val response = client.post("/api/v1/users") {
            bearerAuth(jwt.signAccessToken("u1", "a@b.com", listOf("user"), listOf("users:create")))
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Insufficient role permissions", response.bodyAsText().json()["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `body tidak valid - 400 dengan error per field`() = userTest {
        val response = client.post("/api/v1/users") {
            bearerAuth(jwt.signAccessToken("u1", "a@b.com", listOf("admin"), listOf("users:create")))
            contentType(ContentType.Application.Json)
            setBody("""{"email":"bukan-email","password":"123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val data = response.bodyAsText().json()["data"]!!.jsonObject
        assertEquals(setOf("email", "name", "password"), data.keys)
    }
}
