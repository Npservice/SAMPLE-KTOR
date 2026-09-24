package com.example.modules.todos

import com.example.common.validation.Rules
import kotlinx.serialization.Serializable

@Serializable
data class CreateTodoDto(
    val title: String,
    val description: String? = null,
) {
    companion object {
        val rules: Rules = {
            isString("title")
            minLength("title", 1)
            maxLength("title", 255)
            optional("description") { isString("description") }
        }
    }
}

/** Semua field opsional, tapi kalau diisi tetap divalidasi. */
@Serializable
data class UpdateTodoDto(
    val title: String? = null,
    val description: String? = null,
    val isCompleted: Boolean? = null,
) {
    companion object {
        val rules: Rules = {
            optional("title") {
                isString("title")
                minLength("title", 1)
                maxLength("title", 255)
            }
            optional("description") { isString("description") }
            optional("is_completed") { isBoolean("is_completed") }
        }
    }
}

data class ListTodosQuery(
    val page: Int = 1,
    val size: Int = 10,
    val search: String? = null,
    /** Filter status: null = semua, true = sudah selesai, false = belum selesai. */
    val isCompleted: Boolean? = null,
) {
    companion object {
        val allowed = setOf("page", "size", "search", "is_completed")
        val rules: Rules = {
            optional("page") {
                min("page", 1)
                isInt("page")
            }
            optional("size") {
                min("size", 1)
                isInt("size")
            }
            optional("is_completed") { isBoolean("is_completed") }
        }

        fun from(query: Map<String, String>) = ListTodosQuery(
            page = query["page"]?.trim()?.toDouble()?.toInt() ?: 1,
            size = query["size"]?.trim()?.toDouble()?.toInt() ?: 10,
            search = query["search"],
            isCompleted = query["is_completed"]?.toBooleanStrict(),
        )
    }
}
