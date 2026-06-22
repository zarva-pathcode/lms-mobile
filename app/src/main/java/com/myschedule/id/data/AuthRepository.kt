package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import com.myschedule.id.UserSession

object AuthRepository {

    fun login(
        email: String,
        password: String,
        onSuccess: (role: String, kelas: String, universitas: String) -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseInstance.auth
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user?.uid
                if (uid == null) {
                    onError("UID tidak ditemukan")
                    return@addOnSuccessListener
                }

                FirebaseInstance.db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        // SINKRONISASI KE USERSESSION
                        UserSession.username = doc.getString("name") ?: ""
                        UserSession.email = doc.getString("email") ?: email
                        UserSession.role = doc.getString("role") ?: "student"
                        UserSession.universitas = doc.getString("universitas") ?: ""
                        UserSession.profileImageUri = doc.getString("profileImageUri")
                        
                        val role = UserSession.role
                        val kelas = doc.getString("kelas") ?: ""
                        val universitas = UserSession.universitas
                        
                        onSuccess(role, kelas, universitas)
                    }
                    .addOnFailureListener {
                        onError("Gagal ambil data user")
                    }
            }
            .addOnFailureListener {
                onError("Login gagal")
            }
    }

    fun logout() {
        FirebaseInstance.auth.signOut()
        UserSession.profileImageUri = null
    }
}