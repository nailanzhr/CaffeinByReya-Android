package com.example.caffeinbyreya.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("caffein_session", Context.MODE_PRIVATE)

    fun saveSession(nama: String, email: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_nama", nama)
            .putString("user_email", email)
            .apply()
    }

    fun saveProfilePhoto(uri: String) {
        prefs.edit().putString("user_photo", uri).apply()
    }

    fun getProfilePhoto(): String = prefs.getString("user_photo", "") ?: ""
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)
    fun getUserName(): String = prefs.getString("user_nama", "Naila Nafisa Zuhra") ?: "Naila Nafisa Zuhra"
    fun getUserEmail(): String = prefs.getString("user_email", "naila.240170006@mhs.unimal.ac.id") ?: "naila.240170006@mhs.unimal.ac.id"

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .apply()
    }
}