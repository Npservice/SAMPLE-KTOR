package com.example.core.logging

import com.example.database.dbQuery
import com.example.features.user.UserTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.MDC
import java.time.LocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pakai H2 in-memory, jadi tidak butuh MySQL. */
class SqlLoggingTest {
    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:sql_logging;MODE=MySQL;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction { SchemaUtils.create(UserTable) }
    }

    @Test
    fun `should log queries with values but redact password on write`() {
        val logs = captureLogs("sql") {
            runBlocking {
                MDC.put("request_id", "req-123")
                try {
                    dbQuery {
                        val now = LocalDateTime.now()
                        UserTable.insert {
                            it[id] = "u1"
                            it[email] = "a@b.com"
                            it[name] = "A"
                            it[password] = "hash-rahasia"
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        UserTable.selectAll().where { UserTable.email eq "a@b.com" }.toList()
                    }
                } finally {
                    MDC.remove("request_id")
                }
            }
        }

        val messages = logs.map { it.formattedMessage }
        val insert = messages.single { it.startsWith("INSERT") }
        val select = messages.single { it.startsWith("SELECT") }

        assertFalse("hash-rahasia" in insert, insert)
        assertTrue(insert.endsWith("-- params: ${Redactor.MASK}"), insert)
        assertTrue("'a@b.com'" in select, "nilai parameter biasa tetap tercatat: $select")
        assertEquals("req-123", logs.first().mdcPropertyMap["request_id"], "request_id ikut ke log SQL")
    }
}
