package com.myschedule.id.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Quiz
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
import com.myschedule.id.data.QuizRepository
import com.myschedule.id.data.TaskRepository
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesScreen(navController: NavHostController, uniName: String, className: String) {
    var tasksWithGrades by remember { mutableStateOf(listOf<Pair<Map<String, Any>, Map<String, Any>?>>()) }
    var quizWithAttempts by remember { mutableStateOf(listOf<Pair<Map<String, Any>, Map<String, Any>?>>()) }
    var isLoading by remember { mutableStateOf(true) }
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    val context = LocalContext.current

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

    fun loadQuizGrades() {
        QuizRepository.getQuizzesByClass(uniName, className) { quizzes ->
            val active = quizzes.filter { (it["status"]?.toString() ?: "active") == "active" }
            if (active.isEmpty()) { quizWithAttempts = emptyList(); return@getQuizzesByClass }
            val list = mutableListOf<Pair<Map<String, Any>, Map<String, Any>?>>()
            var loaded = 0
            active.forEach { quiz ->
                QuizRepository.getAttempt(quiz["id"].toString(), uid) { attempt ->
                    list.add(quiz to attempt)
                    loaded++
                    if (loaded == active.size) {
                        quizWithAttempts = list.sortedByDescending { (it.first["deadline"] as? String) ?: "" }
                    }
                }
            }
        }
    }

    LaunchedEffect(uniName, className) { loadGrades() }
    LaunchedEffect(uniName, className) { loadQuizGrades() }

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
            } else if (tasksWithGrades.isEmpty() && quizWithAttempts.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("Belum ada tugas atau quiz.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (tasksWithGrades.isNotEmpty()) {
                        item {
                            Text("Nilai Tugas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = bluePrimary)
                        }
                        items(tasksWithGrades) { (task, submission) ->
                            GradeItemCardInternal(task, submission, bluePrimary) {
                                if (submission != null) {
                                    val fileUrl = submission["fileUrl"]?.toString() ?: ""
                                    val fileName = submission["fileName"]?.toString() ?: "Tugas Saya"
                                    val submissionLink = submission["submissionLink"]?.toString() ?: ""
                                    when {
                                        fileUrl.isNotEmpty() && fileUrl != "null" -> {
                                            val encodedUrl = URLEncoder.encode(fileUrl, "UTF-8")
                                            val encodedName = URLEncoder.encode(fileName, "UTF-8")
                                            navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                        }
                                        submissionLink.isNotEmpty() -> {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(submissionLink))
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (quizWithAttempts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Nilai Quiz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = bluePrimary)
                        }
                        items(quizWithAttempts) { (quiz, attempt) ->
                            QuizGradeCard(quiz, attempt, bluePrimary)
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
                    val hasFile = submission["fileUrl"]?.toString()?.isNotEmpty() == true
                    val hasLink = submission["submissionLink"]?.toString()?.isNotEmpty() == true
                    val label = when {
                        hasFile && hasLink -> "File + Link"
                        hasFile -> "File"
                        hasLink -> "Link"
                        else -> "Sudah dikumpulkan"
                    }
                    Text("(Klik untuk lihat $label)", fontSize = 10.sp, color = Color.Gray)
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

@Composable
fun QuizGradeCard(quiz: Map<String, Any>, attempt: Map<String, Any>?, primaryColor: Color) {
    val score = (attempt?.get("score") as? Long)?.toInt() ?: -1
    val submitted = attempt != null && attempt["submittedAt"] != null
    val passingScore = (quiz["passingScore"] as? Long)?.toInt() ?: 70
    val totalQuestions = (quiz["totalQuestions"] as? Long)?.toInt() ?: 0

    val (category, catColor) = when {
        !submitted -> "Belum dikerjakan" to Color.Gray
        score < passingScore -> "Belum memenuhi" to Color(0xFFEF4444)
        score < 75 -> "Cukup" to Color(0xFFF59E0B)
        score < 90 -> "Baik" to Color(0xFF10B981)
        else -> "Sangat Baik" to Color(0xFF059669)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(primaryColor.copy(0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Quiz, null, tint = primaryColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text("$totalQuestions Soal", fontSize = 11.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (submitted) "$score" else "-",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = catColor
                )
                Text(category, fontSize = 10.sp, color = catColor)
            }
        }
    }
}
