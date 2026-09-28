package com.example.features.todo

import com.example.core.exception.DataNotFoundException
import com.example.core.response.PageRequest
import com.example.core.response.PageResponse
import com.example.core.response.toPageResponse
import java.time.ZoneOffset

/** Todo hanya bisa diakses pemiliknya ([userId]). Todo milik user lain dianggap tidak ada. */
class TodoService(private val repository: TodoRepository) {

    suspend fun findAll(userId: String, page: PageRequest, search: String?, isCompleted: Boolean?): PageResponse<TodoResponse> {
        val (todos, total) = repository.search(userId, search, isCompleted, page)
        return todos.map { it.toResponse() }.toPageResponse(total, page)
    }

    suspend fun findById(userId: String, id: String): TodoResponse = getOwned(userId, id).toResponse()

    suspend fun create(userId: String, request: TodoRequest): TodoResponse =
        repository.create(userId, request.title.trim(), request.description).toResponse()

    suspend fun update(userId: String, id: String, request: TodoUpdateRequest): TodoResponse {
        getOwned(userId, id)
        repository.update(id, request.title?.trim(), request.description, request.isCompleted)
        return findById(userId, id)
    }

    suspend fun softDelete(userId: String, id: String) {
        getOwned(userId, id)
        repository.softDelete(id)
    }

    private suspend fun getOwned(userId: String, id: String): Todo =
        repository.findByIdAndUserId(id, userId) ?: throw DataNotFoundException("Todo tidak ditemukan")

    private fun Todo.toResponse() = TodoResponse(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        completedAt = completedAt?.toInstant(ZoneOffset.UTC)?.toString(),
        createdAt = createdAt.toInstant(ZoneOffset.UTC).toString(),
        updatedAt = updatedAt.toInstant(ZoneOffset.UTC).toString(),
    )
}
