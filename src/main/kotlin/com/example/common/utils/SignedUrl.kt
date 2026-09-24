package com.example.common.utils

import io.ktor.http.URLBuilder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignedUrl {
    private fun sign(baseUrl: String, expires: Long, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal("$baseUrl:$expires".toByteArray()).toHexString()
    }

    /** Normalisasi URL tanpa `expires` & `signature`, dipakai saat sign maupun validate. */
    private fun baseOf(url: String): String {
        val builder = URLBuilder(url)
        builder.parameters.remove("expires")
        builder.parameters.remove("signature")
        return builder.buildString()
    }

    fun generate(baseUrl: String, expiresInMs: Long, secret: String): String {
        val expires = (System.currentTimeMillis() + expiresInMs) / 1000
        val base = baseOf(baseUrl)
        val builder = URLBuilder(base)
        builder.parameters["expires"] = expires.toString()
        builder.parameters["signature"] = sign(base, expires, secret)
        return builder.buildString()
    }

    fun validate(fullUrl: String, secret: String): Boolean {
        val url = runCatching { URLBuilder(fullUrl) }.getOrNull() ?: return false
        val expires = url.parameters["expires"]?.toLongOrNull() ?: return false
        val signature = url.parameters["signature"] ?: return false

        if (System.currentTimeMillis() / 1000 > expires) {
            return false
        }

        val expected = sign(baseOf(fullUrl), expires, secret)
        // Perbandingan constant-time supaya tidak bocor lewat timing attack.
        return MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }
}
