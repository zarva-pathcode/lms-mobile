package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.data.TaskRepository
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesScreen(navController: NavHostController, uniName: String, className: String) {
    var tasksWithGrades by remember { mutableStateOf(listOf<Pair<Map<String, Any>, Map<String, Any>?>>()) }
    var isLoading by remember { mutableStateOf(true) }
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadGrades() {
        isLoading = true
        TaskRepository.getTasksByClass(uniName, className) { allTasks ->
            val tasksList = mutableListOf<Pair<Map<String, Any>, Map<String, Any>?>>()
            var loadedCount = 0
            
            if (allTasks.isEmpty()) {
                isLoading = false
                return@getTasksByClass
            }

            allTasks.forEach { task ->
                val taskId = task["id"] as String
                TaskRepository.getStudentSubmission(taskId, uid) { submission ->
                    tasksList.add(task to submission)
                    loadedCount++
                    if (loadedCount == allTasks.size) {
                        tasksWithGrades = tasksList.sortedByDescending { (it.first["createdAt"] as? com.google.firebase.Timestamp) }
                        isLoading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(uniName, className) { loadGrades() }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                    .statusBarsPadding()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("Daftar Nilai", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = bluePrimary)
            } else if (tasksWithGrades.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("Belum ada tugas atau nilai.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasksWithGrades) { (task, submission) ->
                        GradeItemCardInternal(task, submission, bluePrimary) {
                            if (submission != null && submission["fileUrl"] != null) {
                                val url = submission["fileUrl"].toString()
                                val name = submission["fileName"]?.toString() ?: "Tugas Saya"
                                if (url.isNotEmpty() && url != "null") {
                                    val encodedUrl = URLEncoder.encode(url, "UTF-8")
                                    val encodedName = URLEncoder.encode(name, "UTF-8")
                                    // PERBAIKAN: Navigasi menggunakan Query Params agar URL tidak pecah
                                    navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeItemCardInternal(task: Map<String, Any>, submission: Map<String, Any>?, primaryColor: Color, onClick: () -> Unit) {
    val grade = submission?.get("grade")?.toString() ?: ""
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(if (grade.isNotEmpty()) Color(0xFF10B981).copy(0.1f) else Color.Gray.copy(0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Assessment, 
                    null, 
                    tint = if (grade.isNotEmpty()) Color(0xFF059669) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(
                    if (submission == null) "Belum mengumpulkan" else "Sudah mengumpulkan",
                    fontSize = 11.sp,
                    color = if (submission == null) Color.Red else Color(0xFF059669)
                )
                if (submission != null) {
                    Text("(Klik untuk melihat file)", fontSize = 10.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (grade.isNotEmpty()) grade else "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (grade.isNotEmpty()) Color(0xFF059669) else Color.Gray
                )
                Text("Nilai", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
