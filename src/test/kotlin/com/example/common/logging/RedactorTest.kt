package com.example.common.logging

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactorTest {
    @Test
    fun `should redact sensitive fields in nested json`() {
        val json = """{"email":"a@b.com","password":"x","data":{"access_token":"t","items":[{"new_password":"y","name":"n"}]}}"""

        assertEquals(
            """{"email":"a@b.com","password":"[REDACTED]","data":{"access_token":"[REDACTED]","items":[{"new_password":"[REDACTED]","name":"n"}]}}""",
            Redactor.redactJson(json),
        )
    }

    @Test
    fun `should leave non json text as is`() {
        assertEquals("bukan json", Redactor.redactJson("bukan json"))
    }
}
