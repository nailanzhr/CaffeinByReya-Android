package com.example.caffeinbyreya.data

import java.security.MessageDigest

object SecurityUtils {
    private const val APP_SALT = "CaffeinByReya_Unimal2026_Salt"

    fun hashPasswordWithSalt(password: String, email: String): String {
        val combined = "$APP_SALT:${email.lowercase().trim()}:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}