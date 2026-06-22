package com.myschedule.id

data class User(
    val username: String,
    val studentId: String,
    val universitas: String,
    val email: String,
    val password: String,
    val role: String // Mahasiswa / Dosen
)