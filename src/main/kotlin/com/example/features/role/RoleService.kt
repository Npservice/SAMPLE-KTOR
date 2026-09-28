package com.example.features.role

import com.example.core.exception.DataNotFoundException

class RoleService(private val repository: RoleRepository) {

    suspend fun findAll(): List<RoleResponse> = repository.findAll().map { it.toResponse() }

    suspend fun findAllPermissions(): List<PermissionResponse> = repository.findAllPermissions().map { it.toResponse() }

    suspend fun findById(id: String): RoleResponse = getRole(id).toResponse()

    suspend fun updatePermissions(id: String, request: RolePermissionRequest): RoleResponse {
        getRole(id)
        repository.replacePermissions(id, request.permissionIds.orEmpty())
        return findById(id)
    }

    private suspend fun getRole(id: String): Role =
        repository.findById(id) ?: throw DataNotFoundException("Role tidak ditemukan")
}
