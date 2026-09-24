package com.example.modules.users

import com.example.common.exceptions.NotFoundException
import com.example.common.security.PasswordHasher
import com.example.common.utils.Paging
import com.example.common.utils.toPaging

open class UsersService(private val usersRepository: UsersRepository) {

    open suspend fun create(dto: CreateUserDto): User {
        val password = PasswordHasher.hash(dto.password)
        return usersRepository.create(dto.email, dto.name, password)
    }

    open suspend fun findAll(query: ListUsersQuery): Paging<User> {
        val (items, total) = usersRepository.findAllPaginated(query.page, query.size, query.search)
        return toPaging(items, query.size, query.page, total)
    }

    open suspend fun findOne(id: String): User =
        usersRepository.findOneWithRelations(id) ?: throw NotFoundException("User #$id not found")

    open suspend fun update(id: String, dto: UpdateUserDto): User {
        findOne(id)
        val password = dto.password?.let { PasswordHasher.hash(it) }
        usersRepository.update(id, dto.email, dto.name, password)
        return findOne(id)
    }

    open suspend fun remove(id: String) {
        findOne(id)
        usersRepository.softDelete(id)
    }
}
