package com.example.features.role

import com.example.core.validation.rules
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RolePermissionRequest(
    val permissionIds: List<String>? = null,
)

@Serializable
data class PermissionResponse(
    val id: String,
    val name: String,
)

@Serializable
data class RoleResponse(
    val id: String,
    val name: String,
    val permissions: List<PermissionResponse>,
)

fun RolePermissionRequest.validate() = rules {
    check("permission_ids", permissionIds != null, "wajib diisi")
    permissionIds?.forEachIndexed { index, id ->
        check("permission_ids.$index", runCatching { UUID.fromString(id) }.isSuccess, "UUID tidak valid")
    }
}
