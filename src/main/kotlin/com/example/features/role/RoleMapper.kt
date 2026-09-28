package com.example.features.role

// Dipakai RoleService & UserService (user selalu dikirim beserta role-nya).

fun Permission.toResponse() = PermissionResponse(id = id, name = name)

fun Role.toResponse() = RoleResponse(id = id, name = name, permissions = permissions.map { it.toResponse() })
