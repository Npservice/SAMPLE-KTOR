package com.example.modules.files

import com.example.common.exceptions.BadRequestException
import com.example.common.exceptions.ForbiddenException
import com.example.common.response.success
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject
import java.net.URLEncoder

private const val MAX_UPLOAD_BYTES = 100L * 1024 * 1024

private fun contentDisposition(filename: String): String {
    val escaped = filename.replace("\\", "\\\\").replace("\"", "\\\"")
    val encoded = URLEncoder.encode(filename, Charsets.UTF_8).replace("+", "%20")
    return "inline; filename=\"$escaped\"; filename*=UTF-8''$encoded"
}

private suspend fun ApplicationCall.respondFile(file: FileEntity, bytes: ByteArray) {
    val contentType = file.mimeType?.let { runCatching { ContentType.parse(it) }.getOrNull() }
        ?: ContentType.Application.OctetStream
    response.header(HttpHeaders.ContentDisposition, contentDisposition(file.nameFile))
    respondBytes(bytes, contentType)
}

/** Route file yang public (tanpa login): `files/public/...` dan `files/signed/...`. */
fun Route.publicFilesRoutes() {
    val filesService by inject<FilesService>()

    route("/files") {
        get("/public/{id}/data") {
            call.success(filesService.generateSignedUrlPublic(call.parameters["id"]!!), "Signed URL berhasil dibuat")
        }

        get("/public/{id}") {
            val file = filesService.findPublicById(call.parameters["id"]!!)
            call.respondFile(file, filesService.getFileBytes(file))
        }

        get("/signed/{id}") {
            if (!filesService.isValidSignedRequest(call.request.uri)) {
                throw ForbiddenException("Invalid or expired URL")
            }
            val file = filesService.findById(call.parameters["id"]!!)
            call.respondFile(file, filesService.getFileBytes(file))
        }
    }
}

/** Route file yang wajib login. Dipasang di dalam `authenticate("jwt")`. */
fun Route.filesRoutes() {
    val filesService by inject<FilesService>()

    route("/files") {
        post("/store") {
            // Kumpulkan semua file dulu baru diproses, supaya field yang salah ditolak sebelum ada yang tersimpan.
            val uploads = mutableListOf<UploadedFile>()
            call.receiveMultipart(formFieldLimit = MAX_UPLOAD_BYTES).forEachPart { part ->
                try {
                    if (part is PartData.FileItem) {
                        if (part.name != "files") throw BadRequestException("Unexpected field")
                        uploads += UploadedFile(
                            originalName = part.originalFileName ?: "file",
                            mimeType = part.contentType?.toString(),
                            bytes = part.provider().readBuffer().readByteArray(),
                        )
                    }
                } finally {
                    part.release()
                }
            }

            call.success(filesService.uploadTemp(uploads), "Upload Success", HttpStatusCode.Created)
        }

        get("/{id}/signed-url") {
            call.success(filesService.generateSignedUrl(call.parameters["id"]!!), "Signed URL berhasil dibuat")
        }

        get("/{id}") {
            val file = filesService.findById(call.parameters["id"]!!)
            call.respondFile(file, filesService.getFileBytes(file))
        }
    }
}
