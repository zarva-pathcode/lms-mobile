package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTasksOverviewScreen(navController: NavHostController) {
    var allTasks by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var enrolledClasses by remember { mutableStateOf(listOf<String>()) }
    var universitas by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadAllTasks() {
        isLoading = true
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                universitas = doc.getString("universitas") ?: ""
                @Suppress("UNCHECKED_CAST")
                enrolledClasses = doc.get("enrolled_classes") as? List<String> ?: listOf()

                if (enrolledClasses.isNotEmpty()) {
                    FirebaseInstance.db.collection("tasks")
                        .whereEqualTo("uniName", universitas)
                        .whereIn("className", enrolledClasses)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            allTasks = snapshot.documents.map { d ->
                                val m = d.data?.toMutableMap() ?: mutableMapOf()
                                m["id"] = d.id
                                m
                            }.sortedByDescending { it["createdAt"] as? com.google.firebase.Timestamp }
                            isLoading = false
                        }
                        .addOnFailureListener { isLoading = false }
                } else {
                    isLoading = false
                }
            }
    }

    LaunchedEffect(Unit) { loadAllTasks() }

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
                    Text("Semua Tugas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
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
            } else if (allTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada tugas dari kelas yang Anda ambil.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(allTasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                navController.navigate("student_tasks/${task["uniName"]}/${task["className"]}")
                            },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                        Text("Kelas: ${task["className"]}", fontSize = 12.sp, color = bluePrimary)
                                    }
                                    Text(task["deadline"].toString(), fontSize = 11.sp, color = Color.Red)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(task["description"].toString(), fontSize = 13.sp, maxLines = 2, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
