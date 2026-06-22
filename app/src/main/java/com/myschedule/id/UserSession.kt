package com.myschedule.id

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object UserSession {

    // ==========================
    // DATA USER AKTIF (RUNTIME)
    // Menggunakan mutableStateOf agar perubahan mentrigger recomposition di Compose
    // ==========================
    var username by mutableStateOf("")
    var studentId by mutableStateOf("")
    var universitas by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var profileImageUri by mutableStateOf<String?>(null)
    var role by mutableStateOf("") // "mahasiswa" atau "dosen"
    var isDarkMode by mutableStateOf(false)
    var isNotifOn by mutableStateOf(false)

    // ==========================
    // UPDATE FOTO PROFIL
    // ==========================
    fun updateProfileImage(context: Context, uri: String) {
        profileImageUri = uri
    }

    // ==========================
    // SIMPAN KE PREFERENCES
    // ==========================
    fun saveToPrefs(context: Context, isLoggedIn: Boolean, rememberPassword: Boolean = true) {
        val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_logged_in", isLoggedIn)
            if (rememberPassword) {
                if (email.isNotEmpty()) putString("last_email", email)
                if (password.isNotEmpty()) putString("last_password", password)
            } else {
                putString("last_email", null)
                putString("last_password", null)
            }
            apply()
        }
    }

    // ==========================
    // LOGOUT (TIDAK HAPUS AKUN)
    // ==========================
    fun logout(context: Context, rememberPassword: Boolean = false) {
        saveToPrefs(context, false, rememberPassword)

        // Reset data runtime
        username = ""
        studentId = ""
        universitas = ""
        email = ""
        password = ""
        profileImageUri = null
        role = ""
    }

    // ==========================
    // CLEAR SESSION
    // ==========================
    fun clearSession(context: Context, rememberPassword: Boolean = false) {
        logout(context, rememberPassword)
        isDarkMode = false
        isNotifOn = false
    }
}