package com.example.features.user

import com.example.core.exception.DataNotFoundException
import com.example.core.exception.FieldValidationException
import com.example.core.response.PageRequest
import com.example.core.response.PageResponse
import com.example.core.response.toPageResponse
import com.example.core.security.PasswordHasher
import com.example.features.role.toResponse
import java.time.ZoneOffset

class UserService(private val repository: UserRepository) {

    suspend fun findAll(page: PageRequest, search: String?): PageResponse<UserResponse> {
        val (users, total) = repository.search(search, page)
        return users.map { it.toResponse() }.toPageResponse(total, page)
    }

    suspend fun findById(id: String): UserResponse = getUser(id).toResponse()

    suspend fun create(request: UserRequest): UserResponse {
        if (repository.existsByEmail(request.email)) throw FieldValidationException(mapOf("email" to "email sudah terdaftar"))

        val user = repository.create(request.email, request.name.trim(), PasswordHasher.hash(request.password))
        return user.toResponse()
    }

    suspend fun update(id: String, request: UserUpdateRequest): UserResponse {
        getUser(id)
        if (request.email != null && repository.existsByEmail(request.email, exceptId = id)) {
            throw FieldValidationException(mapOf("email" to "email sudah terdaftar"))
        }

        repository.update(id, request.email, request.name?.trim(), request.password?.let { PasswordHasher.hash(it) })
        return findById(id)
    }

    suspend fun softDelete(id: String) {
        getUser(id)
        repository.softDelete(id)
    }

    private suspend fun getUser(id: String): User =
        repository.findById(id) ?: throw DataNotFoundException("User tidak ditemukan")

    private fun User.toResponse() = UserResponse(
        id = id,
        email = email,
        name = name,
        roles = roles.map { it.toResponse() },
        createdAt = createdAt.toInstant(ZoneOffset.UTC).toString(),
        updatedAt = updatedAt.toInstant(ZoneOffset.UTC).toString(),
    )
}
