package com.example.core.security

import com.example.core.exception.ForbiddenException
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

class AuthorizationConfig {
    var roles: List<String> = emptyList()
    var permissions: List<String> = emptyList()
}

/**
 * Cek role & permission user. Jalan setelah autentikasi JWT (`AuthenticationChecked`),
 * jadi urutannya: JWT -> roles -> permissions.
 */
val AuthorizationPlugin = createRouteScopedPlugin("Authorization", ::AuthorizationConfig) {
    val requiredRoles = pluginConfig.roles
    val requiredPermissions = pluginConfig.permissions

    on(AuthenticationChecked) { call ->
        val user = call.principal<AuthenticatedUser>() ?: return@on

        // Role: cukup punya salah satu.
        if (requiredRoles.isNotEmpty() && requiredRoles.none { it in user.roles }) {
            throw ForbiddenException("Insufficient role permissions")
        }

        // Permission: wajib punya semuanya.
        if (requiredPermissions.isNotEmpty() && !requiredPermissions.all { it in user.permissions }) {
            throw ForbiddenException("Insufficient permissions")
        }
    }
}

private class AuthorizationRouteSelector(private val description: String) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Transparent

    override fun toString() = "(authorize $description)"
}

/**
 * Batasi route di dalamnya berdasarkan role dan/atau permission.
 * Harus dipakai di dalam `authenticate("jwt") { }`.
 *
 * ```
 * authorize(roles = listOf("admin"), permissions = listOf("users:delete")) {
 *     delete("{id}") { ... }
 * }
 * ```
 */
fun Route.authorize(
    roles: List<String> = emptyList(),
    permissions: List<String> = emptyList(),
    build: Route.() -> Unit,
): Route {
    val route = createChild(AuthorizationRouteSelector("roles=$roles permissions=$permissions"))
    route.install(AuthorizationPlugin) {
        this.roles = roles
        this.permissions = permissions
    }
    route.build()
    return route
}
