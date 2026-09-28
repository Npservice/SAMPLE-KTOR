package com.example.core.security

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hash & verifikasi password dengan bcrypt (cost 10, format `$2b$`).
 * Password lebih dari 72 byte dipotong (batas bawaan bcrypt), bukan ditolak.
 */
object PasswordHasher {
    private const val COST = 10
    private val version = BCrypt.Version.VERSION_2B
    private val longPassword = LongPasswordStrategies.truncate(version)
    private val hasher = BCrypt.with(version, java.security.SecureRandom(), longPassword)
    private val verifier = BCrypt.verifyer(version, longPassword)

    suspend fun hash(password: String): String = withContext(Dispatchers.Default) {
        hasher.hashToString(COST, password.toCharArray())
    }

    suspend fun verify(password: String, hash: String): Boolean = withContext(Dispatchers.Default) {
        verifier.verify(password.toCharArray(), hash).verified
    }
}
