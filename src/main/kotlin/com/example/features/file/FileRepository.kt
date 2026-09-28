package com.example.features.file

import com.example.database.dbQuery
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDateTime
import java.time.ZoneOffset

class FileRepository {

    suspend fun create(file: FileEntity) {
        dbQuery {
            val now = LocalDateTime.now(ZoneOffset.UTC)
            FileTable.insert {
                it[id] = file.id
                it[nameFile] = file.nameFile
                it[path] = file.path
                it[ext] = file.ext
                it[size] = file.size
                it[mimeType] = file.mimeType
                it[isTemp] = file.isTemp
                it[status] = file.status.value
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    suspend fun findById(id: String): FileEntity? = findActive(FileTable.id eq id)

    suspend fun findPublicById(id: String): FileEntity? =
        findActive((FileTable.id eq id) and (FileTable.status eq FileStatus.PUBLIC.value))

    private suspend fun findActive(condition: Op<Boolean>): FileEntity? = dbQuery {
        FileTable.selectAll().where(condition and FileTable.deletedAt.isNull()).singleOrNull()?.toFile()
    }

    private fun ResultRow.toFile() = FileEntity(
        id = this[FileTable.id],
        nameFile = this[FileTable.nameFile],
        path = this[FileTable.path],
        ext = this[FileTable.ext],
        size = this[FileTable.size],
        mimeType = this[FileTable.mimeType],
        isTemp = this[FileTable.isTemp],
        status = FileStatus.of(this[FileTable.status]),
    )
}
