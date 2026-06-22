package com.myschedule.id.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAbsensiDetailScreen(navController: NavHostController, absensiId: String, title: String) {
    var studentsInClass by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var attendanceMap by remember { mutableStateOf(mapOf<String, String>()) } // uid -> status
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadData() {
        FirebaseInstance.db.collection("absensi").document(absensiId).get()
            .addOnSuccessListener { doc ->
                val sessionData = doc.data ?: return@addOnSuccessListener
                val blueprintId = sessionData["blueprintId"] as? String ?: ""
                
                @Suppress("UNCHECKED_CAST")
                val attendedStudents = sessionData["students"] as? List<Map<String, Any>> ?: listOf()
                val currentAttendance = attendedStudents.associate { 
                    (it["uid"] as String) to (it["status"] as? String ?: "Hadir")
                }
                attendanceMap = currentAttendance

                FirebaseInstance.db.collection("schedule_blueprints").document(blueprintId).get()
                    .addOnSuccessListener { bDoc ->
                        val uni = bDoc.getString("universitas") ?: ""
                        val kelas = bDoc.getString("kelas") ?: ""
                        
                        FirebaseInstance.db.collection("users")
                            .whereEqualTo("role", "student")
                            .whereEqualTo("universitas", uni)
                            .get()
                            .addOnSuccessListener { sSnapshot ->
                                studentsInClass = sSnapshot.documents.mapNotNull { d ->
                                    val data = d.data ?: return@mapNotNull null
                                    @Suppress("UNCHECKED_CAST")
                                    val enrolled = data["enrolled_classes"] as? List<String> ?: listOf()
                                    if (enrolled.contains(kelas)) {
                                        val map = data.toMutableMap()
                                        map["uid"] = d.id
                                        map
                                    } else null
                                }
                                isLoading = false
                            }
                    }
            }
    }

    LaunchedEffect(Unit) { loadData() }

    fun updateStatus(studentUid: String, studentName: String, newStatus: String) {
        val attendanceData = hashMapOf(
            "uid" to studentUid,
            "name" to studentName,
            "status" to newStatus,
            "timestamp" to System.currentTimeMillis()
        )

        FirebaseInstance.db.collection("absensi").document(absensiId).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val currentList = (doc.get("students") as? List<Map<String, Any>> ?: listOf()).toMutableList()
                currentList.removeAll { it["uid"] == studentUid }
                currentList.add(attendanceData)
                
                FirebaseInstance.db.collection("absensi").document(absensiId)
                    .update("students", currentList)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Status $studentName: $newStatus", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
            }
    }

    val filteredStudents = studentsInClass.filter {
        it["name"].toString().contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                    .statusBarsPadding()
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Input Absensi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Cari Mahasiswa...", color = Color.White.copy(0.6f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.8f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(0.5f),
                            cursorColor = Color.White
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = bluePrimary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // STATS SUMMARY
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val stats = mapOf(
                        "Hadir" to Color(0xFF10B981),
                        "Sakit" to Color(0xFFF59E0B),
                        "Izin" to Color(0xFF3B82F6),
                        "Alpha" to Color(0xFFEF4444)
                    )
                    
                    stats.forEach { (status, color) ->
                        val count = attendanceMap.values.count { it == status }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = color.copy(0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, color.copy(0.2f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(status, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                                Text(count.toString(), fontSize = 16.sp, color = color, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // MULTI-ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            filteredStudents.forEach { s ->
                                updateStatus(s["uid"] as String, s["name"].toString(), "Hadir")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Hadirkan Semua", fontSize = 11.sp, color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            filteredStudents.forEach { s ->
                                updateStatus(s["uid"] as String, s["name"].toString(), "Alpha")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Alphakan Semua", fontSize = 11.sp, color = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredStudents) { student ->
                        val uid = student["uid"] as String
                        val name = student["name"].toString()
                        val currentStatus = attendanceMap[uid] ?: "Belum Absen"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(bluePrimary.copy(0.1f)), contentAlignment = Alignment.Center) {
                                        Text(name.take(1).uppercase(), color = bluePrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            text = if (currentStatus == "Belum Absen") "Belum ada keterangan" else "Status: $currentStatus",
                                            color = when(currentStatus) {
                                                "Hadir" -> Color(0xFF10B981)
                                                "Sakit" -> Color(0xFFF59E0B)
                                                "Izin" -> Color(0xFF3B82F6)
                                                "Alpha" -> Color(0xFFEF4444)
                                                else -> Color.Gray
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Hadir", "Sakit", "Izin", "Alpha").forEach { status ->
                                        val isSelected = currentStatus == status
                                        val statusColor = when(status) {
                                            "Hadir" -> Color(0xFF10B981)
                                            "Sakit" -> Color(0xFFF59E0B)
                                            "Izin" -> Color(0xFF3B82F6)
                                            "Alpha" -> Color(0xFFEF4444)
                                            else -> Color.Gray
                                        }
                                        
                                        OutlinedButton(
                                            onClick = { updateStatus(uid, name, status) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(0.dp),
                                            border = BorderStroke(1.dp, if (isSelected) statusColor else Color.LightGray),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) statusColor.copy(0.1f) else Color.Transparent,
                                                contentColor = if (isSelected) statusColor else Color.Black
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(status, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
