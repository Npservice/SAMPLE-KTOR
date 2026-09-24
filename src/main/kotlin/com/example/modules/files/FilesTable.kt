package com.example.modules.files

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

enum class FileStatus(val value: String) {
    PUBLIC("public"),
    PRIVATE("private");

    companion object {
        fun of(value: String) = entries.first { it.value == value }
    }
}

object FilesTable : Table("files") {
    val id = varchar("id", 36)
    val nameFile = varchar("name_file", 255)
    val path = varchar("path", 255)
    val ext = varchar("ext", 255).nullable()
    val size = varchar("size", 255).nullable()
    val mimeType = varchar("mime_type", 255).nullable()
    val isTemp = bool("is_temp").default(true)
    val status = varchar("status", 16).default(FileStatus.PRIVATE.value)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val deletedAt = datetime("deleted_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
