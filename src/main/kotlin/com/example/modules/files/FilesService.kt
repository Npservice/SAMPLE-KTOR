package com.example.modules.files

import com.example.common.exceptions.NotFoundException
import com.example.common.exceptions.UnsupportedMediaTypeException
import com.example.common.utils.SignedUrl
import com.example.config.FilesConfig
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension

private val ALLOWED_EXTENSIONS = setOf(".pdf", ".jpg", ".jpeg", ".png", ".gif", ".xlsx", ".docx")

private const val SIGNED_URL_TTL_MS = 2 * 60 * 1000L

private val DOCUMENT_TYPE_BY_EXT = mapOf(
    "doc" to "word", "docx" to "word", "docm" to "word", "dot" to "word",
    "dotx" to "word", "odt" to "word", "rtf" to "word", "txt" to "word",
    "xls" to "cell", "xlsx" to "cell", "xlsm" to "cell", "xlt" to "cell",
    "xltx" to "cell", "csv" to "cell", "ods" to "cell",
    "ppt" to "slide", "pptx" to "slide", "pps" to "slide", "ppsx" to "slide", "odp" to "slide",
    "pdf" to "pdf",
)

/** File hasil upload multipart yang sudah dibaca ke memori. */
class UploadedFile(
    val originalName: String,
    val mimeType: String?,
    val bytes: ByteArray,
)

@Serializable
data class UploadedFileResult(
    val id: String,
    val filename: String,
)

@Serializable
data class SignedUrlResult(
    val url: String,
    val key: String,
    val title: String,
    val fileType: String,
    val documentType: String,
)

class FilesService(
    private val filesRepository: FilesRepository,
    private val config: FilesConfig,
) {
    private val tempDir: Path = Paths.get(config.tempDir).normalize()
    private val storageDir: Path = Paths.get(config.storageDir).normalize()

    suspend fun uploadTemp(files: List<UploadedFile>): List<UploadedFileResult> {
        Files.createDirectories(tempDir)

        return files.map { file ->
            val ext = extensionOf(file.originalName)

            if (ext !in ALLOWED_EXTENSIONS) {
                throw UnsupportedMediaTypeException("unsupported file type")
            }

            val id = UUID.randomUUID().toString()
            val path = tempDir.resolve("${UUID.randomUUID()}$ext")
            Files.write(path, file.bytes)

            filesRepository.create(
                FileEntity(
                    id = id,
                    nameFile = file.originalName,
                    path = path.toString(),
                    ext = ext,
                    size = file.bytes.size.toString(),
                    mimeType = file.mimeType,
                    isTemp = true,
                    status = FileStatus.PRIVATE,
                ),
            )

            UploadedFileResult(id = id, filename = file.originalName)
        }
    }

    suspend fun findById(id: String): FileEntity =
        filesRepository.findById(id) ?: throw NotFoundException("File tidak ditemukan")

    suspend fun findPublicById(id: String): FileEntity =
        filesRepository.findPublicById(id) ?: throw NotFoundException("File tidak ditemukan")

    fun getFileBytes(file: FileEntity): ByteArray = Files.readAllBytes(Paths.get(file.path))

    suspend fun deleteFile(id: String) {
        val file = filesRepository.findById(id) ?: return
        Paths.get(file.path).deleteIfExists()
        filesRepository.delete(id)
    }

    /** Pindahkan file dari storage kembali ke temp. */
    suspend fun rollbackFile(id: String, status: FileStatus) = moveFile(id, toStorage = false, status)

    /** Pindahkan file dari temp ke storage permanen (dipanggil saat data pemilik file disimpan). */
    suspend fun moveToStorage(id: String, status: FileStatus) = moveFile(id, toStorage = true, status)

    private suspend fun moveFile(id: String, toStorage: Boolean, status: FileStatus): FileEntity {
        val file = findById(id)
        val destDir = if (toStorage) storageDir else tempDir
        val source = Paths.get(file.path)
        val ext = source.extension.let { if (it.isEmpty()) "" else ".$it" }
        val newPath = destDir.resolve("${UUID.randomUUID()}$ext")

        Files.createDirectories(destDir)
        Files.move(source, newPath)

        filesRepository.updateLocation(id, newPath.toString(), isTemp = !toStorage, status)
        return file.copy(path = newPath.toString(), isTemp = !toStorage, status = status)
    }

    suspend fun generateSignedUrl(id: String): SignedUrlResult = buildSignedResult(id, findById(id))

    suspend fun generateSignedUrlPublic(id: String): SignedUrlResult = buildSignedResult(id, findPublicById(id))

    /** Cek signature & masa berlaku untuk `GET /files/signed/{id}`. */
    fun isValidSignedRequest(requestUri: String): Boolean {
        if (config.signedUrlSecret.isEmpty()) return false
        return SignedUrl.validate("${config.appUrl.trimEnd('/')}$requestUri", config.signedUrlSecret)
    }

    private fun buildSignedResult(id: String, file: FileEntity): SignedUrlResult {
        check(config.signedUrlSecret.isNotEmpty()) { "SIGNED_URL_SECRET belum di-set" }

        val fileUrl = "${config.appUrl.trimEnd('/')}/api/v1/files/signed/$id"
        val url = SignedUrl.generate(fileUrl, SIGNED_URL_TTL_MS, config.signedUrlSecret)
        val ext = file.ext?.removePrefix(".")?.lowercase().orEmpty()

        return SignedUrlResult(
            url = url,
            key = "doc-$id-${System.currentTimeMillis()}",
            title = file.nameFile,
            fileType = ext,
            documentType = DOCUMENT_TYPE_BY_EXT[ext] ?: "word",
        )
    }

    private fun extensionOf(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot <= 0) "" else filename.substring(dot).lowercase()
    }
}
