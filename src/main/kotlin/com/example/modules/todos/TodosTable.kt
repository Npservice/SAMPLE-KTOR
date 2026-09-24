package com.example.modules.todos

import com.example.modules.users.UsersTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object TodosTable : Table("todos") {
    val id = varchar("id", 36)
    /** Pemilik todo. Kalau user dihapus permanen, todo-nya ikut terhapus. */
    val userId = varchar("user_id", 36)
        .references(UsersTable.id, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        .index()
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val isCompleted = bool("is_completed").default(false)
    val completedAt = datetime("completed_at").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
