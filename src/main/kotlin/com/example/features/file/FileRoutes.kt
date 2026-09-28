package com.example.features.file

import com.example.core.exception.BadRequestException
import com.example.core.exception.ForbiddenException
import com.example.core.response.isSuccess
import com.example.core.response.send
import com.example.core.security.JWT_AUTH
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject
import java.net.URLEncoder

private const val MAX_UPLOAD_BYTES = 100L * 1024 * 1024

/** Modul fitur file: `/api/v1/files`. Sebagian public (`/public/...`, `/signed/...`), sisanya wajib login. */
fun Application.fileModule() {
    val fileService by inject<FileService>()

    /** Kirim isi file `inline` (ditampilkan di browser). Nama file non-ASCII aman di-encode. */
    suspend fun ApplicationCall.respondFile(file: FileEntity) {
        val escaped = file.nameFile.replace("\\", "\\\\").replace("\"", "\\\"")
        val encoded = URLEncoder.encode(file.nameFile, Charsets.UTF_8).replace("+", "%20")
        response.header(HttpHeaders.ContentDisposition, "inline; filename=\"$escaped\"; filename*=UTF-8''$encoded")

        val contentType = file.mimeType?.let { runCatching { ContentType.parse(it) }.getOrNull() }
            ?: ContentType.Application.OctetStream
        respondBytes(fileService.readFile(file), contentType)
    }

    routing {
        route("/api/v1/files") {
            // ---------- Public ----------
            get("/public/{id}/data") {
                val data = fileService.generateSignedUrlPublic(call.parameters["id"]!!)
                call.send(data.isSuccess(message = "Signed URL berhasil dibuat"))
            }
            get("/public/{id}") {
                call.respondFile(fileService.findPublicById(call.parameters["id"]!!))
            }
            get("/signed/{id}") {
                if (!fileService.isValidSignedRequest(call.request.uri)) throw ForbiddenException("URL tidak valid atau kedaluwarsa")
                call.respondFile(fileService.findById(call.parameters["id"]!!))
            }

            // ---------- Wajib login ----------
            authenticate(JWT_AUTH) {
                post("/store") {
                    // Kumpulkan semua file dulu, supaya field yang salah ditolak sebelum ada yang tersimpan.
                    val uploads = mutableListOf<UploadedFile>()
                    call.receiveMultipart(formFieldLimit = MAX_UPLOAD_BYTES).forEachPart { part ->
                        try {
                            if (part is PartData.FileItem) {
                                if (part.name != "files") throw BadRequestException("Field tidak dikenal: ${part.name}")
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
                    call.send(fileService.uploadTemp(uploads).isSuccess(201, "Upload berhasil"))
                }

                get("/{id}/signed-url") {
                    val data = fileService.generateSignedUrl(call.parameters["id"]!!)
                    call.send(data.isSuccess(message = "Signed URL berhasil dibuat"))
                }

                get("/{id}") {
                    call.respondFile(fileService.findById(call.parameters["id"]!!))
                }
            }
        }
    }
}
