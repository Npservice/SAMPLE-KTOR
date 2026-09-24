package com.example.modules.auth

import com.example.common.exceptions.NotFoundException

class RolesService(private val rolesRepository: RolesRepository) {

    suspend fun findAllRoles(): List<Role> = rolesRepository.findAllRoles()

    suspend fun findAllPermissions(): List<Permission> = rolesRepository.findAllPermissions()

    suspend fun findRoleWithPermissions(id: String): Role =
        rolesRepository.findRoleWithPermissions(id) ?: throw NotFoundException("Role #$id not found")

    suspend fun updateRolePermissions(id: String, dto: UpdateRolePermissionsDto): Role {
        findRoleWithPermissions(id)
        rolesRepository.replacePermissions(id, dto.permissionIds)
        return findRoleWithPermissions(id)
    }
}
