package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.data.AbsensiRepository
import com.myschedule.id.data.ScheduleRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAbsensiScreen(navController: NavHostController, uniName: String, className: String) {
    var enrolledSchedules by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedScheduleId by remember { mutableStateOf("") }
    var absensiSessions by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var message by remember { mutableStateOf("") }
    
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    val studentName = remember { mutableStateOf("") }

    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadSessions(scheduleId: String) {
        AbsensiRepository.getAbsensiBySchedule(scheduleId) {
            absensiSessions = it
        }
    }

    fun loadData() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { studentName.value = it.getString("name") ?: "Mahasiswa" }

        ScheduleRepository.getSchedulesByStudent(uid) {
            enrolledSchedules = it
            if (className.isNotEmpty()) {
                val found = it.find { s -> s["title"].toString().equals(className, ignoreCase = true) }
                if (found != null) {
                    selectedScheduleId = found["id"] as String
                    loadSessions(selectedScheduleId)
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("Absensi Saya", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (className.isNotEmpty()) {
                            Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)) {
            if (enrolledSchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Anda belum memiliki jadwal kuliah.", color = Color.Gray)
                }
            } else {
                Text("Pilih Mata Kuliah", fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                    items(enrolledSchedules) { schedule ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(50.dp),
                            onClick = { 
                                selectedScheduleId = schedule["id"] as String
                                loadSessions(selectedScheduleId)
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedScheduleId == schedule["id"]) bluePrimary.copy(alpha = 0.1f) else Color.White
                            )
                        ) {
                            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                Text(
                                    text = schedule["title"].toString(), 
                                    fontSize = 14.sp,
                                    color = if (selectedScheduleId == schedule["id"]) bluePrimary else Color.Black,
                                    fontWeight = if (selectedScheduleId == schedule["id"]) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedScheduleId.isNotEmpty()) {
                    Text("Sesi Absensi Tersedia", fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (message.isNotEmpty()) {
                        Text(message, color = bluePrimary, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(absensiSessions) { session ->
                            val students = session["students"] as? List<Map<String, Any>> ?: listOf()
                            val alreadyAbsen = students.any { it["uid"] == uid }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session["title"].toString(), fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("Tanggal: ${session["date"]}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    
                                    if (alreadyAbsen) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF10B981))
                                        Text(" Hadir", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    } else {
                                        Button(
                                            onClick = {
                                                AbsensiRepository.submitAbsensi(
                                                    session["id"] as String,
                                                    uid,
                                                    studentName.value,
                                                    onSuccess = {
                                                        message = "Berhasil Absen!"
                                                        loadSessions(selectedScheduleId)
                                                    },
                                                    onError = { message = it }
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                                        ) {
                                            Text("Absen", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Pilih mata kuliah di atas untuk melihat sesi absensi", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
