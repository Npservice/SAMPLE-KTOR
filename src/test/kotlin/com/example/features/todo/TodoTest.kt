package com.example.features.todo

import com.example.config.AppConfig
import com.example.config.Env
import com.example.core.exception.DataNotFoundException
import com.example.core.response.PageRequest
import com.example.core.response.PageResponse
import com.example.core.response.Pages
import com.example.module
import com.example.core.security.JwtProvider
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val USER_ID = "b6a1f5b0-9c3a-4e8e-8f6a-2f7e6c1d9a11"

private val todo = Todo(
    id = "0f8fad5b-d9cb-469f-a165-70867728950e",
    userId = USER_ID,
    title = "Belajar Ktor",
    description = null,
    isCompleted = false,
    completedAt = null,
    createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
)

/** Unit test service: repository di-mock, tidak butuh database. */
class TodoServiceTest {
    private val repository = mockk<TodoRepository>(relaxUnitFun = true)
    private val service = TodoService(repository)

    @Test
    fun `findById todo milik user lain - DataNotFoundException`() {
        runBlocking {
            coEvery { repository.findByIdAndUserId(any(), any()) } returns null

            assertFailsWith<DataNotFoundException> { service.findById(USER_ID, todo.id) }
        }
    }

    @Test
    fun `update isCompleted meneruskan status ke repository`() {
        runBlocking {
            coEvery { repository.findByIdAndUserId(todo.id, USER_ID) } returns todo

            service.update(USER_ID, todo.id, TodoUpdateRequest(isCompleted = true))

            coVerify { repository.update(todo.id, null, null, true) }
        }
    }

    @Test
    fun `create - title di-trim`() {
        runBlocking {
            coEvery { repository.create(any(), any(), any()) } returns todo

            service.create(USER_ID, TodoRequest(title = "  Belajar Ktor  "))

            coVerify { repository.create(USER_ID, "Belajar Ktor", null) }
        }
    }
}

/** Test route: service di-mock. */
class TodoRoutesTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret")))
    private val token = JwtProvider(config.jwt).signAccessToken(USER_ID, "jane@example.com", emptyList(), emptyList())

    private val service = mockk<TodoService> {
        coEvery { findAll(any(), any(), any(), any()) } returns PageResponse(emptyList(), Pages(10, 0, 0, 0))
        coEvery { findById(any(), any()) } throws DataNotFoundException("Todo tidak ditemukan")
    }

    private fun todoTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            module(config, overrides = listOf(module { single { service } }), connectDatabase = false)
        }
        block()
    }

    private fun String.json() = Json.parseToJsonElement(this).jsonObject

    @Test
    fun `wajib login`() = todoTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/todos").status)
    }

    @Test
    fun `list - paging dan filter diteruskan ke service`() = todoTest {
        val response = client.get("/api/v1/todos?page=1&size=5&search=ktor&is_completed=false") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText().json()["status"]!!.jsonPrimitive.content)
        coVerify { service.findAll(USER_ID, PageRequest(1, 5), "ktor", false) }
    }

    @Test
    fun `list - is_completed tidak valid - 400`() = todoTest {
        val response = client.get("/api/v1/todos?is_completed=abc") { bearerAuth(token) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `create - validasi gagal - 400 dengan error per field`() = todoTest {
        val response = client.post("/api/v1/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText().json()
        assertEquals("Validation Failed", body["message"]!!.jsonPrimitive.content)
        assertEquals("wajib diisi", body["data"]!!.jsonObject["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun `detail tidak ditemukan - 404 FAILED`() = todoTest {
        val response = client.get("/api/v1/todos/${todo.id}") { bearerAuth(token) }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = response.bodyAsText().json()
        assertEquals("FAILED", body["status"]!!.jsonPrimitive.content)
        assertEquals("Todo tidak ditemukan", body["message"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, body["data"])
    }
}

