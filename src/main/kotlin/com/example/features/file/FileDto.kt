package com.example.features.file

import kotlinx.serialization.Serializable

/** File hasil upload multipart yang sudah dibaca ke memori. */
class UploadedFile(
    val originalName: String,
    val mimeType: String?,
    val bytes: ByteArray,
)

@Serializable
data class FileUploadResponse(
    val id: String,
    val filename: String,
)

/** Data untuk frontend membuka file di document viewer/editor. */
@Serializable
data class SignedUrlResponse(
    val url: String,
    val key: String,
    val title: String,
    val fileType: String,
    val documentType: String,
)
