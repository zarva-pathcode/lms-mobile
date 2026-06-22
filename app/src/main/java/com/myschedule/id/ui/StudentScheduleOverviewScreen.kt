package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.UserSession
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScheduleOverviewScreen(navController: NavHostController) {
    var allBlueprints by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(true) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    val daysMap = mapOf(
        Calendar.MONDAY to "Senin",
        Calendar.TUESDAY to "Selasa",
        Calendar.WEDNESDAY to "Rabu",
        Calendar.THURSDAY to "Kamis",
        Calendar.FRIDAY to "Jumat",
        Calendar.SATURDAY to "Sabtu",
        Calendar.SUNDAY to "Minggu"
    )

    fun loadAllSchedules() {
        isLoading = true
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val universitas = doc.getString("universitas") ?: ""
                @Suppress("UNCHECKED_CAST")
                val enrolledClasses = doc.get("enrolled_classes") as? List<String> ?: listOf()

                if (enrolledClasses.isNotEmpty()) {
                    FirebaseInstance.db.collection("schedule_blueprints")
                        .whereEqualTo("universitas", universitas)
                        .whereIn("kelas", enrolledClasses)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            allBlueprints = snapshot.documents.map { d ->
                                val m = d.data?.toMutableMap() ?: mutableMapOf()
                                m["id"] = d.id
                                m
                            }.sortedBy { it["dayOfWeek"] as? Long }
                            isLoading = false
                        }
                        .addOnFailureListener { isLoading = false }
                } else {
                    isLoading = false
                }
            }
    }

    LaunchedEffect(Unit) { loadAllSchedules() }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight))).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { navController.navigate("student_profile") },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(0.2f))
                    ) {
                        if (UserSession.profileImageUri != null) {
                            AsyncImage(
                                model = UserSession.profileImageUri,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Jadwal Kuliah Saya", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                }
            }
        },
        bottomBar = { MainBottomBar(navController) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = bluePrimary)
                }
            } else if (allBlueprints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada jadwal kuliah.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val grouped = allBlueprints.groupBy { it["dayOfWeek"] as? Long }
                    
                    grouped.keys.sortedBy { it }.forEach { dayIdx ->
                        item {
                            Text(
                                text = daysMap[dayIdx?.toInt()] ?: "Lainnya",
                                fontWeight = FontWeight.ExtraBold,
                                color = bluePrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        items(grouped[dayIdx] ?: listOf()) { bp ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bp["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                        Text("Kelas: ${bp["kelas"]}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text(bp["time"].toString(), fontWeight = FontWeight.Medium, color = bluePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
