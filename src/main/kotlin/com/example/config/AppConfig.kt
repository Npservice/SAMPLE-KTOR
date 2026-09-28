package com.example.config

import io.github.cdimascio.dotenv.dotenv

/**
 * Baca `.env` (kalau ada), fallback ke environment variable sistem,
 * lalu ke nilai default.
 */
class Env(private val values: Map<String, String>) {
    fun get(key: String, default: String): String = values[key]?.takeIf { it.isNotEmpty() } ?: default
    fun int(key: String, default: Int): Int = values[key]?.toIntOrNull() ?: default

    companion object {
        fun load(): Env {
            val dotenv = dotenv { ignoreIfMissing = true }
            // Environment variable sistem menang atas isi .env.
            return Env(dotenv.entries().associate { it.key to it.value } + System.getenv())
        }
    }
}

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val database: String,
) {
    val jdbcUrl get() = "jdbc:mysql://$host:$port/$database?useUnicode=true&characterEncoding=utf8"
}

data class JwtConfig(
    val secret: String,
    val accessExpiresIn: String,
    val refreshExpiresIn: String,
)

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String,
    val cacheTtlSeconds: Int,
)

data class ThrottleConfig(
    val ttlSeconds: Int,
    val limit: Int,
)

data class FilesConfig(
    val appUrl: String,
    val tempDir: String,
    val storageDir: String,
    val signedUrlSecret: String,
)


data class AppConfig(
    val appEnv: String,
    val port: Int,
    val database: DatabaseConfig,
    val jwt: JwtConfig,
    val redis: RedisConfig,
    val throttle: ThrottleConfig,
    val files: FilesConfig,
) {
    val isProduction get() = appEnv == "production"

    companion object {
        fun from(env: Env) = AppConfig(
            appEnv = env.get("APP_ENV", "development"),
            port = env.int("PORT", 3000),
            database = DatabaseConfig(
                host = env.get("DB_HOST", "localhost"),
                port = env.int("DB_PORT", 3306),
                username = env.get("DB_USERNAME", "root"),
                password = env.get("DB_PASSWORD", ""),
                database = env.get("DB_DATABASE", "example"),
            ),
            jwt = JwtConfig(
                secret = env.get("JWT_SECRET", "change-me"),
                accessExpiresIn = env.get("JWT_ACCESS_EXPIRES_IN", "15m"),
                refreshExpiresIn = env.get("JWT_REFRESH_EXPIRES_IN", "7d"),
            ),
            redis = RedisConfig(
                host = env.get("REDIS_HOST", "localhost"),
                port = env.int("REDIS_PORT", 6379),
                password = env.get("REDIS_PASSWORD", ""),
                cacheTtlSeconds = env.int("CACHE_TTL", 60),
            ),
            throttle = ThrottleConfig(
                ttlSeconds = env.int("THROTTLE_TTL", 60),
                limit = env.int("THROTTLE_LIMIT", 100),
            ),
            files = FilesConfig(
                appUrl = env.get("APP_URL", "http://localhost:3000"),
                tempDir = env.get("FILE_TEMP_DIR", "./storage/temp"),
                storageDir = env.get("FILE_STORAGE_DIR", "./storage/files"),
                signedUrlSecret = env.get("SIGNED_URL_SECRET", ""),
            ),
        )
    }
}
