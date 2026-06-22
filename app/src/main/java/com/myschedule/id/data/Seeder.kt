package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import com.google.firebase.auth.FirebaseAuthUserCollisionException

object Seeder {

    private val usersToSeed = listOf(
        Triple("teacher@gmail.com", "teacher", "Dosen 1"),
        Triple("student@gmail.com", "student", "Mahasiswa 1")
    )

    fun seedUsers(onDone: () -> Unit, onError: (String) -> Unit) {
        seedNext(0, onDone, onError)
    }

    private fun seedNext(index: Int, onDone: () -> Unit, onError: (String) -> Unit) {
        // Semua user selesai diproses
        if (index >= usersToSeed.size) {
            FirebaseInstance.auth.signOut()
            onDone()
            return
        }

        val (email, role, name) = usersToSeed[index]
        val auth = FirebaseInstance.auth
        val db = FirebaseInstance.db

        auth.createUserWithEmailAndPassword(email, "123456")
            .addOnCompleteListener { task ->
                val shouldContinue = task.isSuccessful ||
                        task.exception is FirebaseAuthUserCollisionException

                if (!shouldContinue) {
                    onError("Auth Gagal ($email): ${task.exception?.message}")
                    return@addOnCompleteListener
                }

                // Sign in untuk dapat UID
                auth.signInWithEmailAndPassword(email, "123456")
                    .addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: run {
                            onError("UID null untuk $email")
                            return@addOnSuccessListener
                        }

                        val data = hashMapOf(
                            "email" to email,
                            "role" to role,
                            "name" to name
                        )

                        db.collection("users").document(uid).set(data)
                            .addOnSuccessListener {
                                // ✅ Proses user berikutnya SETELAH yang ini selesai
                                seedNext(index + 1, onDone, onError)
                            }
                            .addOnFailureListener {
                                onError("Firestore Gagal ($email): ${it.message}")
                            }
                    }
                    .addOnFailureListener {
                        onError("SignIn Gagal ($email): ${it.message}")
                    }
            }
    }
}