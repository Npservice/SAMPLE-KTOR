package com.example.modules.users

import com.example.common.utils.Paging
import com.example.common.utils.PagingMeta
import com.example.config.AppConfig
import com.example.config.Env
import com.example.modules.auth.JwtService
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test route users. UsersService di-mock, jadi tidak butuh database. */
class UsersRoutesTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret")))
    private val jwt = JwtService(config.jwt)

    private val mockUser = User(
        id = "b6a1f5b0-9c3a-4e8e-8f6a-2f7e6c1d9a11",
        email = "jane@example.com",
        name = "Jane",
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        deletedAt = null,
        roles = emptyList(),
    )

    private val usersService = mockk<UsersService> {
        coEvery { create(any()) } returns mockUser
        coEvery { findAll(any()) } returns Paging(listOf(mockUser), PagingMeta(1, 10, 1, 1))
        coEvery { findOne(any()) } returns mockUser
    }

    private val adminToken = jwt.signAccessToken(
        mockUser.id, mockUser.email, listOf("admin"), listOf("users:create", "users:read"),
    )

    private fun usersTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            module(config, overrides = listOf(module { single { usersService } }), connectDatabase = false)
        }
        block()
    }

    private fun String.json() = Json.parseToJsonElement(this).jsonObject

    @Test
    fun `should create a user`() = usersTest {
        val response = client.post("/api/v1/users") {
            bearerAuth(adminToken)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"jane@example.com","name":"Jane","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText().json()
        assertEquals("Created data successfully", body["message"]!!.jsonPrimitive.content)
        val data = body["data"]!!.jsonObject
        assertEquals(mockUser.id, data["id"]!!.jsonPrimitive.content)
        assertEquals(setOf("id", "email", "name", "created_at", "updated_at", "deleted_at", "roles"), data.keys)
        coVerify { usersService.create(CreateUserDto("jane@example.com", "Jane", "secret123")) }
    }

    @Test
    fun `should return all users`() = usersTest {
        val response = client.get("/api/v1/users?page=1&size=10") { bearerAuth(adminToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        val meta = response.bodyAsText().json()["data"]!!.jsonObject["meta"]!!.jsonObject
        assertEquals(setOf("current_page", "per_page", "total", "last_page"), meta.keys)
        coVerify { usersService.findAll(ListUsersQuery(page = 1, size = 10)) }
    }

    @Test
    fun `should return a single user`() = usersTest {
        val response = client.get("/api/v1/users/${mockUser.id}") { bearerAuth(adminToken) }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { usersService.findOne(mockUser.id) }
    }

    @Test
    fun `should reject request without token`() = usersTest {
        val response = client.get("/api/v1/users")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("Unauthorized", response.bodyAsText().json()["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `should reject user without permission`() = usersTest {
        val token = jwt.signAccessToken(mockUser.id, mockUser.email, listOf("user"), listOf("users:read"))
        val response = client.post("/api/v1/users") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"jane@example.com","name":"Jane","password":"secret123"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("Insufficient role permissions", response.bodyAsText().json()["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `should reject invalid body`() = usersTest {
        val response = client.post("/api/v1/users") {
            bearerAuth(adminToken)
            contentType(ContentType.Application.Json)
            setBody("""{"email":"not-an-email","password":"123","role":"admin"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText().json()
        assertEquals(400, body["code"]!!.jsonPrimitive.int)
        assertEquals(
            "property role should not exist, email must be an email, name must be a string, " +
                "password must be longer than or equal to 6 characters",
            body["message"]!!.jsonPrimitive.content,
        )
    }
}
