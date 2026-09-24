package com.example.common.logging

import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.slf4j.LoggerFactory

/**
 * Catat setiap query SQL ke logger `sql` (file `logs/sql.log`), lengkap dengan nilai parameternya.
 *
 * Untuk INSERT/UPDATE yang menulis kolom sensitif (mis. `password`), nilai parameternya tidak
 * ditampilkan: yang dicatat hanya SQL dengan placeholder `?`.
 */
object SqlFileLogger : SqlLogger {
    private val log = LoggerFactory.getLogger("sql")

    /** Kolom yang nilainya tidak boleh muncul di log saat ditulis. */
    private val SENSITIVE_COLUMNS = listOf("password")

    private val WRITE_TYPES = setOf(StatementType.INSERT, StatementType.UPDATE)

    override fun log(context: StatementContext, transaction: Transaction) {
        if (!log.isInfoEnabled) return

        val sql = context.sql(transaction)
        val writesSensitive = context.statement.type in WRITE_TYPES &&
            SENSITIVE_COLUMNS.any { sql.contains(it, ignoreCase = true) }

        log.info(if (writesSensitive) "$sql -- params: ${Redactor.MASK}" else context.expandArgs(transaction))
    }
}
