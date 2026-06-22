package com.myschedule.id.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.data.TaskRepository
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentTasksGradesScreen(
    navController: NavHostController,
    studentUid: String,
    studentName: String,
    uniName: String,
    className: String
) {
    var tasksWithGrades by remember { mutableStateOf(listOf<Pair<Map<String, Any>, Map<String, Any>?>>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadData() {
        TaskRepository.getTasksByClass(uniName, className) { allTasks ->
            val tasksList = mutableListOf<Pair<Map<String, Any>, Map<String, Any>?>>()
            var loadedCount = 0
            
            if (allTasks.isEmpty()) {
                isLoading = false
                return@getTasksByClass
            }

            allTasks.forEach { task ->
                val taskId = task["id"] as String
                TaskRepository.getStudentSubmission(taskId, studentUid) { submission ->
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

    LaunchedEffect(Unit) { loadData() }

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
                        Text("Nilai Mahasiswa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(studentName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = bluePrimary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasksWithGrades) { (task, submission) ->
                    GradeTaskItemInternal(task, submission, studentUid, bluePrimary, navController) { loadData() }
                }
            }
        }
    }
}

@Composable
fun GradeTaskItemInternal(
    task: Map<String, Any>, 
    submission: Map<String, Any>?, 
    studentUid: String, 
    primaryColor: Color, 
    navController: NavHostController,
    onUpdate: () -> Unit
) {
    var gradeValue by remember { mutableStateOf(submission?.get("grade")?.toString() ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    val taskId = task["id"] as String
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(task["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), color = Color.Black)
                Surface(
                    color = if (gradeValue.isNotEmpty()) Color(0xFF10B981).copy(0.1f) else Color.Gray.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (gradeValue.isNotEmpty()) "Nilai: $gradeValue" else "Belum Dinilai",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = if (gradeValue.isNotEmpty()) Color(0xFF059669) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (submission == null) {
                Text("Status: Belum mengumpulkan", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text("Status: Sudah mengumpulkan", color = Color(0xFF059669), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                
                // PERBAIKAN: Menggunakan fileUrl dan navigasi query param
                val fileUrl = submission["fileUrl"]?.toString() ?: ""
                val fileName = submission["fileName"]?.toString() ?: "File Tugas"
                
                if (fileUrl.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                                val encodedUrl = Uri.encode(fileUrl)
                                val encodedName = Uri.encode(fileName)
                                navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FileOpen, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(fileName, color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gradeValue,
                        onValueChange = { if (it.length <= 3) gradeValue = it },
                        label = { Text("Skor") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        onClick = {
                            TaskRepository.updateGrade(taskId, studentUid, gradeValue, {
                                isEditing = false
                                onUpdate()
                            }, {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            })
                        }
                    ) {
                        Text("Simpan", color = Color.White)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { isEditing = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Text(if (gradeValue.isEmpty()) "Input Nilai" else "Ubah Nilai")
                }
            }
        }
    }
}