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
import com.myschedule.id.data.QuizRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherQuizResultsScreen(navController: NavHostController, quizId: String, quizTitle: String) {
    var quiz by remember { mutableStateOf<Map<String, Any>?>(null) }
    var attempts by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var refreshKey by remember { mutableStateOf(0) }
    var selectedAttempt by remember { mutableStateOf<Map<String, Any>?>(null) }
    var overrideScore by remember { mutableStateOf("") }

    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadData() {
        QuizRepository.getQuiz(quizId) { quiz = it }
        QuizRepository.getAttempts(quizId) { attempts = it }
    }

    LaunchedEffect(refreshKey) { loadData() }

    val totalStudents = attempts.size
    val avgScore = if (totalStudents > 0) attempts.mapNotNull { it["score"] as? Long } .let { scores -> scores.sum() / scores.size } else 0
    val maxScore = attempts.maxOfOrNull { (it["score"] as? Long)?.toInt() ?: 0 } ?: 0
    val minScore = attempts.minOfOrNull { (it["score"] as? Long)?.toInt() ?: 0 } ?: 0
    val passedCount = attempts.count { it["passed"] as? Boolean == true }
    val totalQuestions = (quiz?.get("totalQuestions") as? Long)?.toInt() ?: 0

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hasil Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(quizTitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Rata-rata", "$avgScore", Color(0xFF1565C0), modifier = Modifier.weight(1f))
                StatCard("Tertinggi", "$maxScore", Color(0xFF10B981), modifier = Modifier.weight(1f))
                StatCard("Terendah", "$minScore", Color(0xFFEF4444), modifier = Modifier.weight(1f))
                StatCard("Lulus", "$passedCount/$totalStudents", Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(attempts) { attempt ->
                    val name = attempt["studentName"]?.toString() ?: "-"
                    val score = (attempt["score"] as? Long)?.toInt() ?: 0
                    val passed = attempt["passed"] as? Boolean == false
                    val correct = (attempt["correctCount"] as? Long)?.toInt() ?: 0

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedAttempt = attempt },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold)
                                Text("Benar: $correct/$totalQuestions", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                                color = if (attempt["passed"] as? Boolean == true) Color(0xFF10B981) else Color(0xFFEF4444))
                        }
                    }
                }

                if (attempts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text("Belum ada mahasiswa yang mengerjakan", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    if (selectedAttempt != null) {
        val attempt = selectedAttempt!!
        val studentName = attempt["studentName"]?.toString() ?: "-"
        val score = (attempt["score"] as? Long)?.toInt() ?: 0
        val answers = (attempt["answers"] as? List<*>)?.map { (it as? Long)?.toInt() ?: -1 } ?: listOf()
        val questions = (quiz?.get("questions") as? List<Map<String, Any>>) ?: listOf()

        AlertDialog(
            onDismissRequest = { selectedAttempt = null },
            title = { Text("Jawaban $studentName", color = bluePrimary) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Skor: $score", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    questions.forEachIndexed { i, q ->
                        val studentAns = answers.getOrElse(i) { -1 }
                        val correctAns = (q["correctAnswer"] as? Long)?.toInt() ?: -1
                        val isCorrect = studentAns == correctAns
                        val options = q["options"] as? List<String> ?: listOf()

                        Card(colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFF10B981).copy(0.1f) else Color(0xFFEF4444).copy(0.1f)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Soal ${i + 1}: ${q["questionText"]}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text("Jawaban: ${if (studentAns in options.indices) "${('A' + studentAns)}. ${options[studentAns]}" else "Tidak dijawab"}", fontSize = 12.sp, color = Color.Gray)
                                Text("Benar: ${('A' + correctAns)}. ${options[correctAns]}", fontSize = 12.sp, color = Color(0xFF10B981))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Override Nilai", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = overrideScore, onValueChange = { it.filter { c -> c.isDigit() }.let { v -> if (v.isEmpty() || v.toInt() <= 100) overrideScore = v } }, label = { Text("Nilai 0-100") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val newScore = overrideScore.toIntOrNull() ?: return@Button
                                val passed = newScore >= ((quiz?.get("passingScore") as? Long)?.toInt() ?: 70)
                                QuizRepository.updateScore(quizId, attempt["studentUid"].toString(), newScore, passed,
                                    onSuccess = { selectedAttempt = null; refreshKey++ },
                                    onError = { }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                        ) { Text("Simpan") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAttempt = null }) { Text("Tutup") }
            }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = color.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 9.sp, color = color)
        }
    }
}
