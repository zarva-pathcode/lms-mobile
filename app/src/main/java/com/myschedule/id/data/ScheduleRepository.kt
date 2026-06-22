package com.myschedule.id.data

import com.myschedule.id.FirebaseInstance
import java.util.*

object ScheduleRepository {

    /**
     * Menambahkan Blueprint Jadwal (Aturan Hari & Jam)
     * dayOfWeek: 1=Minggu, 2=Senin, ..., 7=Sabtu
     */
    fun addScheduleBlueprint(
        title: String,
        dayOfWeek: Int, 
        time: String,
        teacherId: String,
        kelas: String,
        universitas: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val data = hashMapOf(
            "title" to title,
            "dayOfWeek" to dayOfWeek,
            "time" to time,
            "teacherId" to teacherId,
            "kelas" to kelas,
            "universitas" to universitas,
            "type" to "blueprint"
        )

        FirebaseInstance.db.collection("schedule_blueprints")
            .add(data)
            .addOnSuccessListener { docRef ->
                // Setelah blueprint dibuat, generate sesi pertama otomatis
                generateInitialSession(docRef.id, dayOfWeek, title, onSuccess, onError)
            }
            .addOnFailureListener { onError(it.message ?: "Gagal tambah blueprint") }
    }

    private fun generateInitialSession(
        blueprintId: String,
        dayOfWeek: Int,
        title: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        
        var daysUntilNext = dayOfWeek - currentDay
        if (daysUntilNext < 0) daysUntilNext += 7
        
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilNext)
        val nextDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        val sessionData = hashMapOf(
            "blueprintId" to blueprintId,
            "title" to title,
            "date" to nextDate,
            "status" to "scheduled",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        FirebaseInstance.db.collection("absensi")
            .add(sessionData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Gagal generate sesi awal") }
    }

    fun getSchedulesByTeacher(teacherId: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("schedule_blueprints")
            .whereEqualTo("teacherId", teacherId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
    }

    fun getSchedulesForStudent(
        kelas: String,
        universitas: String,
        onResult: (List<Map<String, Any>>) -> Unit
    ) {
        FirebaseInstance.db.collection("schedule_blueprints")
            .whereEqualTo("kelas", kelas)
            .whereEqualTo("universitas", universitas)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    val map = doc.data?.toMutableMap() ?: mutableMapOf()
                    map["id"] = doc.id
                    map
                }
                onResult(list)
            }
    }

    fun getSchedulesByStudent(studentUid: String, onResult: (List<Map<String, Any>>) -> Unit) {
        FirebaseInstance.db.collection("users").document(studentUid).get()
            .addOnSuccessListener { doc ->
                val universitas = doc.getString("universitas") ?: ""
                @Suppress("UNCHECKED_CAST")
                val enrolledClasses = doc.get("enrolled_classes") as? List<String> ?: listOf()

                if (universitas.isNotEmpty() && enrolledClasses.isNotEmpty()) {
                    FirebaseInstance.db.collection("schedule_blueprints")
                        .whereEqualTo("universitas", universitas)
                        .whereIn("kelas", enrolledClasses)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val list = snapshot.documents.map { d ->
                                val map = d.data?.toMutableMap() ?: mutableMapOf()
                                map["id"] = d.id
                                map
                            }
                            onResult(list)
                        }
                        .addOnFailureListener { onResult(emptyList()) }
                } else {
                    onResult(emptyList())
                }
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /**
     * Fungsi Cerdas: Cek & Generate Sesi Absensi Otomatis
     */
    fun checkAndGenerateDailySessions(universitas: String, kelasList: List<String>) {
        if (kelasList.isEmpty()) return
        
        val calendar = Calendar.getInstance()
        val todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // 1. Ambil semua blueprint untuk kelas-kelas mahasiswa ini yang jatuh pada hari ini
        FirebaseInstance.db.collection("schedule_blueprints")
            .whereEqualTo("universitas", universitas)
            .whereIn("kelas", kelasList)
            .whereEqualTo("dayOfWeek", todayDayOfWeek)
            .get()
            .addOnSuccessListener { blueprints ->
                for (blueprint in blueprints) {
                    val blueprintId = blueprint.id
                    val title = blueprint.getString("title") ?: ""
                    
                    // 2. Cek apakah sesi untuk hari ini sudah ada
                    FirebaseInstance.db.collection("absensi")
                        .whereEqualTo("blueprintId", blueprintId)
                        .whereEqualTo("date", todayDate)
                        .get()
                        .addOnSuccessListener { sessions ->
                            if (sessions.isEmpty) {
                                // 3. Jika belum ada, buatkan sesi otomatis
                                val newSession = hashMapOf(
                                    "blueprintId" to blueprintId,
                                    "title" to title,
                                    "date" to todayDate,
                                    "status" to "active",
                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                    "students" to listOf<Map<String, Any>>()
                                )
                                FirebaseInstance.db.collection("absensi").add(newSession)
                            }
                        }
                }
            }
    }

    // Fungsi legacy tetap ada agar tidak break UI lama sementara
    fun addSchedule(title: String, date: String, time: String, teacherId: String, kelas: String, universitas: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        // Default ke hari ini jika konversi gagal, idealnya parsing date string
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) 
        addScheduleBlueprint(title, dayOfWeek, time, teacherId, kelas, universitas, onSuccess, onError)
    }
}
