package com.example.features.todo

import com.example.core.response.PageRequest
import com.example.database.dbQuery
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/** Semua query otomatis mengabaikan todo yang sudah di-soft delete. */
class TodoRepository {

    suspend fun search(userId: String, search: String?, isCompleted: Boolean?, page: PageRequest): Pair<List<Todo>, Long> =
        dbQuery {
            var condition: Op<Boolean> = (TodoTable.userId eq userId) and TodoTable.deletedAt.isNull()
            if (!search.isNullOrBlank()) {
                condition = condition and ((TodoTable.title like "%$search%") or (TodoTable.description like "%$search%"))
            }
            if (isCompleted != null) condition = condition and (TodoTable.isCompleted eq isCompleted)

            val total = TodoTable.selectAll().where(condition).count()
            val todos = TodoTable.selectAll()
                .where(condition)
                .orderBy(TodoTable.createdAt, SortOrder.DESC)
                .limit(page.size)
                .offset(page.offset)
                .map { it.toTodo() }
            todos to total
        }

    suspend fun findByIdAndUserId(id: String, userId: String): Todo? = dbQuery {
        TodoTable.selectAll()
            .where { (TodoTable.id eq id) and (TodoTable.userId eq userId) and TodoTable.deletedAt.isNull() }
            .singleOrNull()
            ?.toTodo()
    }

    suspend fun create(userId: String, title: String, description: String?): Todo = dbQuery {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val todo = Todo(UUID.randomUUID().toString(), userId, title, description, false, null, now, now)
        TodoTable.insert {
            it[id] = todo.id
            it[TodoTable.userId] = userId
            it[TodoTable.title] = title
            it[TodoTable.description] = description
            it[createdAt] = now
            it[updatedAt] = now
        }
        todo
    }

    /** Field yang null tidak diubah. `completedAt` diisi/dikosongkan mengikuti [isCompleted]. */
    suspend fun update(id: String, title: String?, description: String?, isCompleted: Boolean?) {
        dbQuery {
            val now = LocalDateTime.now(ZoneOffset.UTC)
            TodoTable.update({ TodoTable.id eq id }) {
                if (title != null) it[TodoTable.title] = title
                if (description != null) it[TodoTable.description] = description
                if (isCompleted != null) {
                    it[TodoTable.isCompleted] = isCompleted
                    it[completedAt] = if (isCompleted) now else null
                }
                it[updatedAt] = now
            }
        }
    }

    /** Soft delete: cuma isi `deleted_at`, datanya tidak dihapus. */
    suspend fun softDelete(id: String) {
        dbQuery { TodoTable.update({ TodoTable.id eq id }) { it[deletedAt] = LocalDateTime.now(ZoneOffset.UTC) } }
    }

    private fun ResultRow.toTodo() = Todo(
        id = this[TodoTable.id],
        userId = this[TodoTable.userId],
        title = this[TodoTable.title],
        description = this[TodoTable.description],
        isCompleted = this[TodoTable.isCompleted],
        completedAt = this[TodoTable.completedAt],
        createdAt = this[TodoTable.createdAt],
        updatedAt = this[TodoTable.updatedAt],
    )
}
