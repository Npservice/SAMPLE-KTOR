package com.example.modules.todos

import com.example.common.exceptions.NotFoundException
import com.example.common.utils.Paging
import com.example.common.utils.toPaging

/** Semua operasi dibatasi ke todo milik user yang sedang login ([userId]). */
open class TodosService(private val todosRepository: TodosRepository) {

    open suspend fun create(userId: String, dto: CreateTodoDto): Todo =
        todosRepository.create(userId, dto.title, dto.description)

    open suspend fun findAll(userId: String, query: ListTodosQuery): Paging<Todo> {
        val (items, total) = todosRepository.findAllPaginated(
            userId, query.page, query.size, query.search, query.isCompleted,
        )
        return toPaging(items, query.size, query.page, total)
    }

    open suspend fun findOne(userId: String, id: String): Todo =
        todosRepository.findOwned(id, userId) ?: throw NotFoundException("Todo #$id not found")

    open suspend fun update(userId: String, id: String, dto: UpdateTodoDto): Todo {
        findOne(userId, id)
        todosRepository.update(id, dto.title, dto.description, dto.isCompleted)
        return findOne(userId, id)
    }

    open suspend fun remove(userId: String, id: String) {
        findOne(userId, id)
        todosRepository.softDelete(id)
    }
}
