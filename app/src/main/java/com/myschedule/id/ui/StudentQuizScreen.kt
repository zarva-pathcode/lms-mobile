package com.myschedule.id.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.data.QuizRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizScreen(navController: NavHostController, uniName: String, className: String) {
    var quizzes by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var attempts by remember { mutableStateOf(mapOf<String, Map<String, Any>>()) }
    var refreshKey by remember { mutableStateOf(0) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadData() {
        QuizRepository.getQuizzesByClass(uniName, className) { list ->
            quizzes = list.filter { (it["status"]?.toString() ?: "active") == "active" }
                .sortedByDescending { it["deadline"] as? String }
            list.forEach { quiz ->
                val qId = quiz["id"].toString()
                QuizRepository.getAttempt(qId, uid) { attempt ->
                    if (attempt != null) {
                        attempts = attempts + (qId to attempt)
                    }
                }
            }
        }
    }

    LaunchedEffect(refreshKey) { loadData() }

    fun isExpired(deadline: String): Boolean {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val deadlineDate = fmt.parse(deadline)
            deadlineDate != null && deadlineDate.before(Date())
        } catch (_: Exception) { false }
    }

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
                    Text("Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quizzes) { quiz ->
                val qId = quiz["id"].toString()
                val attempt = attempts[qId]
                val expired = isExpired(quiz["deadline"]?.toString() ?: "")
                val submitted = attempt != null && (attempt["submittedAt"] != null)
                val score = (attempt?.get("score") as? Long)?.toInt() ?: -1
                val passingScore = (quiz["passingScore"] as? Long)?.toInt() ?: 70
                val totalQuestions = (quiz["totalQuestions"] as? Long)?.toInt() ?: 0
                val timeLimit = (quiz["timeLimit"] as? Long)?.toInt() ?: 0

                val category = when {
                    score < passingScore -> "Belum memenuhi" to Color(0xFFEF4444)
                    score < 75 -> "Cukup" to Color(0xFFF59E0B)
                    score < 90 -> "Baik" to Color(0xFF10B981)
                    else -> "Sangat Baik" to Color(0xFF059669)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).background(bluePrimary.copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Quiz, null, tint = bluePrimary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(quiz["title"].toString(), fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("$totalQuestions Soal | ⏱ ${timeLimit}m", fontSize = 12.sp, color = Color.Gray)
                            Text("Deadline: ${quiz["deadline"]}", fontSize = 11.sp, color = bluePrimary)

                            if (submitted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (score < passingScore) Icons.Default.Cancel else Icons.Default.CheckCircle, null,
                                        tint = category.second, modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skor: $score - ${category.first}", fontSize = 12.sp, color = category.second)
                                }
                            }
                        }

                        if (submitted) {
                            Icon(Icons.Default.CheckCircle, null, tint = category.second)
                        } else if (expired) {
                            Text("Terlambat", fontSize = 12.sp, color = Color.Red)
                        } else {
                            Button(
                                onClick = { navController.navigate("student_quiz_attempt/$qId/${quiz["title"]}") },
                                colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Kerjakan", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (quizzes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("Tidak ada quiz tersedia", color = Color.Gray)
                    }
                }
            }
        }
    }
}
