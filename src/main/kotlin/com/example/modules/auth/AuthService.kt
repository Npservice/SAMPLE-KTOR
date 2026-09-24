package com.example.modules.auth

import com.example.common.exceptions.ConflictException
import com.example.common.exceptions.UnauthorizedException
import com.example.common.security.PasswordHasher
import com.example.modules.users.User
import com.example.modules.users.UsersRepository

class AuthService(
    private val usersRepository: UsersRepository,
    private val jwtService: JwtService,
) {

    suspend fun register(dto: RegisterDto): TokenPair {
        if (usersRepository.findByEmail(dto.email) != null) {
            throw ConflictException("Email already registered")
        }

        val password = PasswordHasher.hash(dto.password)
        val user = usersRepository.create(dto.email, dto.name, password)

        return buildTokenPair(user, emptyList(), emptyList())
    }

    suspend fun login(dto: LoginDto): TokenPair {
        val user = usersRepository.findByEmailWithPassword(dto.email)

        val passwordMatches = user?.password?.let { PasswordHasher.verify(dto.password, it) } ?: false

        if (user == null || !passwordMatches) {
            throw UnauthorizedException("Invalid credentials")
        }

        val (roles, permissions) = flattenRolePermissions(user)
        return buildTokenPair(user, roles, permissions)
    }

    suspend fun refreshToken(token: String): TokenPair {
        val userId = jwtService.verify(token)?.subject
            ?: throw UnauthorizedException("Refresh token not valid")

        val user = usersRepository.findOneWithRelations(userId)
            ?: throw UnauthorizedException("Refresh token not valid")

        val (roles, permissions) = flattenRolePermissions(user)
        return buildTokenPair(user, roles, permissions)
    }

    private fun flattenRolePermissions(user: User): Pair<List<String>, List<String>> {
        val roles = user.roles.orEmpty()
        return roles.map { it.name } to roles.flatMap { role -> role.permissions.orEmpty().map { it.name } }.distinct()
    }

    private fun buildTokenPair(user: User, roles: List<String>, permissions: List<String>) = TokenPair(
        accessToken = jwtService.signAccessToken(user.id, user.email, roles, permissions),
        refreshToken = jwtService.signRefreshToken(user.id),
        user = AuthUser(
            id = user.id,
            email = user.email,
            name = user.name,
            roles = roles,
            permissions = permissions,
        ),
    )
}
