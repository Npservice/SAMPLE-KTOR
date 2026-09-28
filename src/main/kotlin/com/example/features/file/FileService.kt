package com.example.features.file

import com.example.config.FilesConfig
import com.example.core.exception.DataNotFoundException
import com.example.core.exception.UnsupportedMediaTypeException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

private val ALLOWED_EXTENSIONS = setOf(".pdf", ".jpg", ".jpeg", ".png", ".gif", ".xlsx", ".docx")

private const val SIGNED_URL_TTL_MS = 2 * 60 * 1000L

private val DOCUMENT_TYPE_BY_EXT = mapOf(
    "doc" to "word", "docx" to "word", "odt" to "word", "rtf" to "word", "txt" to "word",
    "xls" to "cell", "xlsx" to "cell", "csv" to "cell", "ods" to "cell",
    "ppt" to "slide", "pptx" to "slide", "odp" to "slide",
    "pdf" to "pdf",
)

class FileService(
    private val repository: FileRepository,
    private val config: FilesConfig,
) {
    private val tempDir = Paths.get(config.tempDir).normalize()
    private val appUrl get() = config.appUrl.trimEnd('/')

    /** Simpan file ke folder temp & catat di tabel files (status private). */
    suspend fun uploadTemp(files: List<UploadedFile>): List<FileUploadResponse> {
        files.forEach { if (extensionOf(it.originalName) !in ALLOWED_EXTENSIONS) throw UnsupportedMediaTypeException("Tipe file tidak didukung") }
        Files.createDirectories(tempDir)

        return files.map { file ->
            val ext = extensionOf(file.originalName)
            val path = tempDir.resolve("${UUID.randomUUID()}$ext")
            Files.write(path, file.bytes)

            val entity = FileEntity(
                id = UUID.randomUUID().toString(),
                nameFile = file.originalName,
                path = path.toString(),
                ext = ext,
                size = file.bytes.size.toString(),
                mimeType = file.mimeType,
                isTemp = true,
                status = FileStatus.PRIVATE,
            )
            repository.create(entity)
            FileUploadResponse(id = entity.id, filename = entity.nameFile)
        }
    }

    suspend fun findById(id: String): FileEntity =
        repository.findById(id) ?: throw DataNotFoundException("File tidak ditemukan")

    suspend fun findPublicById(id: String): FileEntity =
        repository.findPublicById(id) ?: throw DataNotFoundException("File tidak ditemukan")

    fun readFile(file: FileEntity): ByteArray = Files.readAllBytes(Paths.get(file.path))

    suspend fun generateSignedUrl(id: String): SignedUrlResponse = findById(id).toSignedUrl()

    suspend fun generateSignedUrlPublic(id: String): SignedUrlResponse = findPublicById(id).toSignedUrl()

    /** Cek signature & masa berlaku untuk `GET /files/signed/{id}`. [requestUri] = path + query. */
    fun isValidSignedRequest(requestUri: String): Boolean =
        config.signedUrlSecret.isNotEmpty() && SignedUrl.validate("$appUrl$requestUri", config.signedUrlSecret)

    private fun FileEntity.toSignedUrl(): SignedUrlResponse {
        check(config.signedUrlSecret.isNotEmpty()) { "SIGNED_URL_SECRET belum di-set" }

        val ext = ext?.removePrefix(".")?.lowercase().orEmpty()
        return SignedUrlResponse(
            url = SignedUrl.generate("$appUrl/api/v1/files/signed/$id", SIGNED_URL_TTL_MS, config.signedUrlSecret),
            key = "doc-$id-${System.currentTimeMillis()}",
            title = nameFile,
            fileType = ext,
            documentType = DOCUMENT_TYPE_BY_EXT[ext] ?: "word",
        )
    }

    private fun extensionOf(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot <= 0) "" else filename.substring(dot).lowercase()
    }
}
