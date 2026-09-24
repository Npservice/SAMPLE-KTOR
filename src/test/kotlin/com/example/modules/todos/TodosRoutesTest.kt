package com.example.modules.todos

import com.example.common.exceptions.NotFoundException
import com.example.common.utils.Paging
import com.example.common.utils.PagingMeta
import com.example.config.AppConfig
import com.example.config.Env
import com.example.module
import com.example.modules.auth.JwtService
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.module
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Test route todos. TodosService di-mock, jadi tidak butuh database. */
class TodosRoutesTest {
    private val config = AppConfig.from(Env(mapOf("JWT_SECRET" to "test-secret")))
    private val jwt = JwtService(config.jwt)

    private val userId = "b6a1f5b0-9c3a-4e8e-8f6a-2f7e6c1d9a11"
    private val token = jwt.signAccessToken(userId, "jane@example.com", emptyList(), emptyList())

    private val mockTodo = Todo(
        id = "0f8fad5b-d9cb-469f-a165-70867728950e",
        userId = userId,
        title = "Belajar Ktor",
        description = null,
        isCompleted = false,
        completedAt = null,
        createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
        deletedAt = null,
    )

    private val todosService = mockk<TodosService> {
        coEvery { create(any(), any()) } returns mockTodo
        coEvery { findAll(any(), any()) } returns Paging(listOf(mockTodo), PagingMeta(1, 10, 1, 1))
        coEvery { update(any(), any(), any()) } returns mockTodo.copy(isCompleted = true)
        coEvery { findOne(any(), "not-mine") } throws NotFoundException("Todo #not-mine not found")
    }

    private fun todosTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            module(config, overrides = listOf(module { single { todosService } }), connectDatabase = false)
        }
        block()
    }

    private fun String.json() = Json.parseToJsonElement(this).jsonObject

    @Test
    fun `should create todo for current user`() = todosTest {
        val response = client.post("/api/v1/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Belajar Ktor"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val data = response.bodyAsText().json()["data"]!!.jsonObject
        assertEquals(
            setOf(
                "id", "user_id", "title", "description", "is_completed",
                "completed_at", "created_at", "updated_at", "deleted_at",
            ),
            data.keys,
        )
        coVerify { todosService.create(userId, CreateTodoDto("Belajar Ktor")) }
    }

    @Test
    fun `should list todos filtered by status`() = todosTest {
        val response = client.get("/api/v1/todos?is_completed=false&search=ktor") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { todosService.findAll(userId, ListTodosQuery(search = "ktor", isCompleted = false)) }
    }

    @Test
    fun `should mark todo as completed`() = todosTest {
        val response = client.patch("/api/v1/todos/${mockTodo.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"is_completed":true}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        coVerify { todosService.update(userId, mockTodo.id, UpdateTodoDto(isCompleted = true)) }
    }

    @Test
    fun `should return 404 for todo of another user`() = todosTest {
        val response = client.get("/api/v1/todos/not-mine") { bearerAuth(token) }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Todo #not-mine not found", response.bodyAsText().json()["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `should reject invalid body`() = todosTest {
        val response = client.post("/api/v1/todos") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"","is_completed":"ya"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "property is_completed should not exist, title must be longer than or equal to 1 characters",
            response.bodyAsText().json()["message"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `should require login`() = todosTest {
        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/todos").status)
    }
}
