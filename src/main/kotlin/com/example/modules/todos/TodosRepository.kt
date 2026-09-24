package com.example.modules.todos

import com.example.common.utils.nowDateTime
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
import java.util.UUID

class TodosRepository {

    /** Todo milik [userId]. Todo user lain dianggap tidak ada. */
    suspend fun findOwned(id: String, userId: String): Todo? = dbQuery {
        TodosTable.selectAll()
            .where { (TodosTable.id eq id) and (TodosTable.userId eq userId) and TodosTable.deletedAt.isNull() }
            .singleOrNull()
            ?.toTodo()
    }

    suspend fun findAllPaginated(
        userId: String,
        page: Int,
        size: Int,
        search: String?,
        isCompleted: Boolean?,
    ): Pair<List<Todo>, Long> = dbQuery {
        var condition: Op<Boolean> = (TodosTable.userId eq userId) and TodosTable.deletedAt.isNull()
        if (!search.isNullOrEmpty()) {
            val pattern = "%$search%"
            condition = condition and ((TodosTable.title like pattern) or (TodosTable.description like pattern))
        }
        if (isCompleted != null) {
            condition = condition and (TodosTable.isCompleted eq isCompleted)
        }

        val total = TodosTable.selectAll().where(condition).count()
        val todos = TodosTable.selectAll()
            .where(condition)
            .orderBy(TodosTable.createdAt, SortOrder.DESC)
            .limit(size)
            .offset(((page - 1) * size).toLong())
            .map { it.toTodo() }

        todos to total
    }

    suspend fun create(userId: String, title: String, description: String?): Todo = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = nowDateTime()
        TodosTable.insert {
            it[TodosTable.id] = id
            it[TodosTable.userId] = userId
            it[TodosTable.title] = title
            it[TodosTable.description] = description
            it[createdAt] = now
            it[updatedAt] = now
        }
        Todo(
            id = id,
            userId = userId,
            title = title,
            description = description,
            isCompleted = false,
            completedAt = null,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
    }

    /** Update field yang tidak null saja. `completed_at` diisi/dikosongkan mengikuti [isCompleted]. */
    suspend fun update(id: String, title: String?, description: String?, isCompleted: Boolean?) {
        dbQuery {
            val now = nowDateTime()
            TodosTable.update({ TodosTable.id eq id }) {
                if (title != null) it[TodosTable.title] = title
                if (description != null) it[TodosTable.description] = description
                if (isCompleted != null) {
                    it[TodosTable.isCompleted] = isCompleted
                    it[completedAt] = if (isCompleted) now else null
                }
                it[updatedAt] = now
            }
        }
    }

    /** Soft delete: cuma isi `deleted_at`, datanya tidak dihapus. */
    suspend fun softDelete(id: String) {
        dbQuery {
            TodosTable.update({ TodosTable.id eq id }) { it[deletedAt] = nowDateTime() }
        }
    }

    private fun ResultRow.toTodo() = Todo(
        id = this[TodosTable.id],
        userId = this[TodosTable.userId],
        title = this[TodosTable.title],
        description = this[TodosTable.description],
        isCompleted = this[TodosTable.isCompleted],
        completedAt = this[TodosTable.completedAt],
        createdAt = this[TodosTable.createdAt],
        updatedAt = this[TodosTable.updatedAt],
        deletedAt = this[TodosTable.deletedAt],
    )
}
