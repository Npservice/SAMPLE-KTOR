package com.example.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Cache berbasis Redis. Koneksi dibuka saat pertama dipakai,
 * jadi aplikasi tetap bisa jalan walau Redis belum nyala selama cache belum dipakai.
 */
class CacheService(private val config: RedisConfig) {
    private val client = lazy {
        val uri = RedisURI.builder().withHost(config.host).withPort(config.port)
        if (config.password.isNotEmpty()) uri.withPassword(config.password.toCharArray())
        RedisClient.create(uri.build())
    }
    private val connection = lazy<StatefulRedisConnection<String, String>> { client.value.connect() }

    fun get(key: String): String? = connection.value.sync().get(key)

    fun set(key: String, value: String, ttlSeconds: Int = config.cacheTtlSeconds) {
        connection.value.sync().set(key, value, SetArgs().ex(ttlSeconds.toLong()))
    }

    fun del(key: String) {
        connection.value.sync().del(key)
    }

    /** Ambil dari cache, kalau tidak ada jalankan [load] lalu simpan. */
    suspend fun remember(key: String, ttlSeconds: Int = config.cacheTtlSeconds, load: suspend () -> String): String =
        get(key) ?: load().also { set(key, it, ttlSeconds) }

    fun close() {
        if (connection.isInitialized()) connection.value.close()
        if (client.isInitialized()) client.value.shutdown()
    }
}
