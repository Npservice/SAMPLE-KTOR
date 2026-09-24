package com.example.modules.users

import com.example.common.validation.Rules
import kotlinx.serialization.Serializable

@Serializable
data class CreateUserDto(
    val email: String,
    val name: String,
    val password: String,
) {
    companion object {
        val rules: Rules = {
            isEmail("email")
            isString("name")
            minLength("password", 6)
            isString("password")
        }
    }
}

/** Semua field opsional, tapi kalau diisi tetap divalidasi. */
@Serializable
data class UpdateUserDto(
    val email: String? = null,
    val name: String? = null,
    val password: String? = null,
) {
    companion object {
        val rules: Rules = {
            optional("email") { isEmail("email") }
            optional("name") { isString("name") }
            optional("password") {
                minLength("password", 6)
                isString("password")
            }
        }
    }
}

data class ListUsersQuery(
    val page: Int = 1,
    val size: Int = 10,
    val search: String? = null,
) {
    companion object {
        val allowed = setOf("page", "size", "search")
        val rules: Rules = {
            optional("page") {
                min("page", 1)
                isInt("page")
            }
            optional("size") {
                min("size", 1)
                isInt("size")
            }
        }

        fun from(query: Map<String, String>) = ListUsersQuery(
            page = query["page"]?.trim()?.toDouble()?.toInt() ?: 1,
            size = query["size"]?.trim()?.toDouble()?.toInt() ?: 10,
            search = query["search"],
        )
    }
}
