package com.example.core.exception

import io.ktor.http.HttpStatusCode

/** Exception dengan status HTTP. Di-throw dari service, ditangani GlobalException (StatusPages). */
open class HttpException(val status: HttpStatusCode, override val message: String) : RuntimeException(message)

class BadRequestException(message: String) : HttpException(HttpStatusCode.BadRequest, message)

class UnauthorizedException(message: String = "Unauthorized") : HttpException(HttpStatusCode.Unauthorized, message)

class ForbiddenException(message: String = "Forbidden") : HttpException(HttpStatusCode.Forbidden, message)

class DataNotFoundException(message: String = "Data tidak ditemukan") : HttpException(HttpStatusCode.NotFound, message)

class UnsupportedMediaTypeException(message: String) : HttpException(HttpStatusCode.UnsupportedMediaType, message)

/** Error validasi per field, mis. `{ "email": "email sudah terdaftar" }`. */
class FieldValidationException(val errors: Map<String, String>) :
    HttpException(HttpStatusCode.BadRequest, "Validation Failed")
