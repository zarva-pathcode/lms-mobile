package com.myschedule.id.ui

import android.net.Uri
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherTaskSubmissionsScreen(navController: NavHostController, taskId: String, taskTitle: String) {
    var submissions by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadSubmissions() {
        TaskRepository.getSubmissions(taskId) { 
            submissions = it 
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadSubmissions() }

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
                        Text("Pengumpulan Tugas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(taskTitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = bluePrimary)
            }
        } else if (submissions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada mahasiswa yang mengumpulkan.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(submissions) { sub ->
                    SubmissionCardInternal(taskId, sub, navController, bluePrimary) { loadSubmissions() }
                }
            }
        }
    }
}

@Composable
fun SubmissionCardInternal(taskId: String, sub: Map<String, Any>, navController: NavHostController, primaryColor: Color, onUpdate: () -> Unit) {
    var gradeValue by remember { mutableStateOf(sub["grade"]?.toString() ?: "") }
    var isEditingGrade by remember { mutableStateOf(false) }
    val studentUid = sub["studentUid"] as? String ?: ""
    val studentName = sub["studentName"]?.toString() ?: "Mahasiswa"
    
    val timestamp = sub["submittedAt"] as? com.google.firebase.Timestamp
    val dateStr = timestamp?.let {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(it.toDate())
    } ?: "-"

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(studentName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Surface(
                    color = if (gradeValue.isNotEmpty()) Color(0xFF10B981).copy(0.1f) else Color.Gray.copy(0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (gradeValue.isNotEmpty()) "Nilai: $gradeValue" else "Belum Dinilai",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        color = if (gradeValue.isNotEmpty()) Color(0xFF059669) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("File Tugas:", fontSize = 12.sp, color = Color.Gray)
            
            val fileUrl = sub["fileUrl"]?.toString() ?: ""
            val fileName = sub["fileName"]?.toString() ?: "File Tugas"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (fileUrl.isNotEmpty()) {
                            // PERBAIKAN: Menggunakan Uri.encode untuk keamanan navigasi query param
                            val encodedUrl = Uri.encode(fileUrl)
                            val encodedName = Uri.encode("$studentName - $fileName")
                            navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                        }
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FileOpen, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    fileName, 
                    color = primaryColor, 
                    fontSize = 14.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text("Dikumpulkan: $dateStr", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            
            if (isEditingGrade) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gradeValue,
                        onValueChange = { if (it.length <= 3) gradeValue = it },
                        label = { Text("Nilai (0-100)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Button(onClick = {
                        if (gradeValue.isNotEmpty()) {
                            TaskRepository.updateGrade(taskId, studentUid, gradeValue, {
                                isEditingGrade = false
                                onUpdate()
                            }, {
                                Log.e("TeacherTaskSubmissions", it)
                            })
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) {
                        Text("Simpan", color = Color.White)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { isEditingGrade = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                ) {
                    Text(if (gradeValue.isEmpty()) "Input Nilai" else "Ubah Nilai")
                }
            }
        }
    }
}
