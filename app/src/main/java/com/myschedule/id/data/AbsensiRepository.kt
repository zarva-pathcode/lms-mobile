package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import com.google.firebase.firestore.FieldValue

object AbsensiRepository {

    // Teacher: Create Absensi Session
    fun createAbsensi(
        blueprintId: String,
        title: String,
        date: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "blueprintId" to blueprintId,
            "title" to title,
            "date" to date,
            "createdAt" to FieldValue.serverTimestamp(),
            "students" to listOf<Map<String, Any>>() // List of {uid, name, status, timestamp}
        )

        FirebaseInstance.db.collection("absensi")
            .add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal membuat absensi") }
    }

    // Get Absensi by Blueprint (Schedule)
    fun getAbsensiBySchedule(blueprintId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("absensi")
            .whereEqualTo("blueprintId", blueprintId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // Student: Submit Absensi (Self-Absen)
    fun submitAbsensi(
        absensiId: String,
        studentUid: String,
        studentName: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val attendanceData = hashMapOf(
            "uid" to studentUid,
            "name" to studentName,
            "status" to status,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseInstance.db.collection("absensi").document(absensiId)
            .update("students", FieldValue.arrayUnion(attendanceData))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal absen") }
    }

    // Delete Absensi Session
    fun deleteAbsensi(absensiId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseInstance.db.collection("absensi").document(absensiId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal hapus absensi") }
    }
}