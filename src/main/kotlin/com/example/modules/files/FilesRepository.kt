package com.example.modules.files

import com.example.common.utils.nowDateTime
import com.example.database.dbQuery
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

data class FileEntity(
    val id: String,
    val nameFile: String,
    val path: String,
    val ext: String?,
    val size: String?,
    val mimeType: String?,
    val isTemp: Boolean,
    val status: FileStatus,
)

class FilesRepository {

    suspend fun create(file: FileEntity) {
        dbQuery {
            val now = nowDateTime()
            FilesTable.insert {
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

    suspend fun findById(id: String): FileEntity? = findWhere(FilesTable.id eq id)

    suspend fun findPublicById(id: String): FileEntity? =
        findWhere((FilesTable.id eq id) and (FilesTable.status eq FileStatus.PUBLIC.value))

    suspend fun updateLocation(id: String, path: String, isTemp: Boolean, status: FileStatus) {
        dbQuery {
            FilesTable.update({ FilesTable.id eq id }) {
                it[FilesTable.path] = path
                it[FilesTable.isTemp] = isTemp
                it[FilesTable.status] = status.value
                it[updatedAt] = nowDateTime()
            }
        }
    }

    /** Hard delete: baris benar-benar dihapus (bukan soft delete). */
    suspend fun delete(id: String) {
        dbQuery { FilesTable.deleteWhere { FilesTable.id eq id } }
    }

    private suspend fun findWhere(condition: Op<Boolean>): FileEntity? = dbQuery {
        FilesTable.selectAll().where(condition and FilesTable.deletedAt.isNull()).singleOrNull()?.toFile()
    }

    private fun ResultRow.toFile() = FileEntity(
        id = this[FilesTable.id],
        nameFile = this[FilesTable.nameFile],
        path = this[FilesTable.path],
        ext = this[FilesTable.ext],
        size = this[FilesTable.size],
        mimeType = this[FilesTable.mimeType],
        isTemp = this[FilesTable.isTemp],
        status = FileStatus.of(this[FilesTable.status]),
    )
}
