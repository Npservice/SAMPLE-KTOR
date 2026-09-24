package com.example.common.exceptions

import io.ktor.http.HttpStatusCode

/**
 * Exception dengan HTTP status. Lempar dari mana saja (service, route, plugin) —
 * StatusPages akan membungkusnya jadi `{ status: "error", code, message, data: null }`.
 */
open class HttpException(val status: HttpStatusCode, message: String) : RuntimeException(message)

class BadRequestException(message: String = "Bad Request") : HttpException(HttpStatusCode.BadRequest, message)
class UnauthorizedException(message: String = "Unauthorized") : HttpException(HttpStatusCode.Unauthorized, message)
class ForbiddenException(message: String = "Forbidden resource") : HttpException(HttpStatusCode.Forbidden, message)
class NotFoundException(message: String = "Not Found") : HttpException(HttpStatusCode.NotFound, message)
class ConflictException(message: String = "Conflict") : HttpException(HttpStatusCode.Conflict, message)
class UnsupportedMediaTypeException(message: String = "Unsupported Media Type") :
    HttpException(HttpStatusCode.UnsupportedMediaType, message)
