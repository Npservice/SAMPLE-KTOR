package com.example.common.validation

import com.example.modules.auth.UpdateRolePermissionsDto
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    @Test
    fun `allowed request fields use snake_case`() {
        assertEquals(setOf("permission_ids"), jsonFieldNames(UpdateRolePermissionsDto.serializer().descriptor))
    }
}
