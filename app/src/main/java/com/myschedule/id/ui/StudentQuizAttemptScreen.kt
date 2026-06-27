package com.myschedule.id.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.data.QuizRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQuizAttemptScreen(navController: NavHostController, quizId: String, quizTitle: String) {
    var quiz by remember { mutableStateOf<Map<String, Any>?>(null) }
    var questions by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var answers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var timeRemaining by remember { mutableStateOf(-1) } // -1 = not started
    var isSubmitting by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var resultScore by remember { mutableStateOf(0) }
    var resultCorrect by remember { mutableStateOf(0) }
    var resultTotal by remember { mutableStateOf(0) }
    var resultPassed by remember { mutableStateOf(false) }
    var resultPassingScore by remember { mutableStateOf(0) }
    var isReviewMode by remember { mutableStateOf(false) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    val studentName = remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadQuiz() {
        QuizRepository.getQuiz(quizId) { data ->
            if (data != null) {
                quiz = data
                @Suppress("UNCHECKED_CAST")
                val qs = (data["questions"] as? List<Map<String, Any>>) ?: listOf()
                questions = qs
                val timeLimit = (data["timeLimit"] as? Long)?.toInt() ?: 30
                timeRemaining = timeLimit * 60

                FirebaseInstance.db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        studentName.value = doc.getString("name") ?: "Mahasiswa"
                    }

                QuizRepository.startAttempt(quizId, uid, studentName.value, onSuccess = {}, onError = {})
            }
        }
    }

    fun submitQuiz() {
        isSubmitting = true
        val answerList = questions.indices.map { answers.getOrElse(it) { -1 } }
        val passingScore = (quiz?.get("passingScore") as? Long)?.toInt() ?: 70

        QuizRepository.submitAttempt(quizId, uid, answerList, questions, passingScore,
            onSuccess = {
                isSubmitting = false
                showResult = true
                resultScore = -1 // will compute from attempt
                // reload attempt for results
                QuizRepository.getAttempt(quizId, uid) { attempt ->
                    if (attempt != null) {
                        resultScore = (attempt["score"] as? Long)?.toInt() ?: 0
                        resultCorrect = (attempt["correctCount"] as? Long)?.toInt() ?: 0
                        resultTotal = (attempt["totalQuestions"] as? Long)?.toInt() ?: 0
                        resultPassed = attempt["passed"] as? Boolean == true
                        resultPassingScore = passingScore
                    }
                }
            },
            onError = {
                isSubmitting = false
                Toast.makeText(context, "Gagal submit: $it", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Timer countdown
    LaunchedEffect(timeRemaining) {
        if (timeRemaining > 0 && !showResult && !isSubmitting) {
            delay(1000)
            timeRemaining--
            if (timeRemaining <= 0) {
                submitQuiz()
            }
        }
    }

    LaunchedEffect(Unit) { loadQuiz() }

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timerColor = when {
        timeRemaining < 60 -> Color.Red
        timeRemaining < 300 -> Color(0xFFF59E0B)
        else -> Color.White
    }

    // Show result dialog
    if (showResult && resultScore >= 0) {
                    val category = when {
                        resultScore < resultPassingScore -> "Belum memenuhi" to Color(0xFFEF4444)
                        resultScore < 75 -> "Cukup" to Color(0xFFF59E0B)
                        resultScore < 90 -> "Baik" to Color(0xFF10B981)
                        else -> "Sangat Baik" to Color(0xFF059669)
                    }

                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(if (resultPassed) "✅ Quiz Selesai!" else "⏱ Waktu Habis!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("Skor: $resultScore / 100", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                                    color = category.second)
                                Text("Benar: $resultCorrect dari $resultTotal soal", fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(category.first,
                                    color = category.second,
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Nilai minimal: $resultPassingScore", fontSize = 12.sp, color = Color.Gray)
                            }
                        },
            confirmButton = {
                Button(onClick = {
                    showResult = false
                    isReviewMode = true
                }) { Text("Lihat Pembahasan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    navController.popBackStack()
                }) { Text("Kembali") }
            }
        )
    }

    // Review mode after submission
    if (isReviewMode && quiz != null) {
        val passingScore = (quiz?.get("passingScore") as? Long)?.toInt() ?: 70

        Scaffold(
            topBar = {
                Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight))).statusBarsPadding()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Text(quizTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)) {
                item {
                    Text("Skor: $resultScore | $resultCorrect/$resultTotal benar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Nilai minimal lulus: $passingScore", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(questions) { i, q ->
                    val studentAns = answers[i] ?: -1
                    val correctAns = (q["correctAnswer"] as? Long)?.toInt() ?: -1
                    val isCorrect = studentAns == correctAns
                    val options = q["options"] as? List<String> ?: listOf()

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFF10B981).copy(0.08f) else Color(0xFFEF4444).copy(0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Soal ${i + 1}: ${q["questionText"]}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            options.forEachIndexed { oi, option ->
                                val isStudentChoice = studentAns == oi
                                val isCorrectAnswer = correctAns == oi
                                val bg = when {
                                    isCorrectAnswer -> Color(0xFF10B981).copy(0.2f)
                                    isStudentChoice && !isCorrect -> Color(0xFFEF4444).copy(0.2f)
                                    else -> Color.Transparent
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    color = bg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "${if (isCorrectAnswer) "✅" else if (isStudentChoice) "❌" else "  "} ${('A' + oi)}. $option",
                                        fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Kembali")
                    }
                }
            }
        }
        return
    }

    // Quiz taking mode
    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight))).statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Soal ${currentQuestionIndex + 1}/${questions.size}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (timeRemaining >= 0) {
                        Icon(Icons.Default.Timer, null, tint = timerColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${String.format("%02d", minutes)}:${String.format("%02d", seconds)}", color = timerColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = bluePrimary)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues)) {
                // Question number indicator
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    questions.indices.forEach { i ->
                        val isAnswered = answers.containsKey(i)
                        val isCurrent = i == currentQuestionIndex
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isCurrent -> bluePrimary
                                isAnswered -> Color(0xFF10B981)
                                else -> Color.LightGray
                            },
                            onClick = { currentQuestionIndex = i }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Current question
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val q = questions.getOrNull(currentQuestionIndex) ?: return@Column
                    val options = q["options"] as? List<String> ?: listOf()
                    val selectedAnswer = answers[currentQuestionIndex]

                    Text("Soal ${currentQuestionIndex + 1}:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                    Text(q["questionText"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { answers = mutableMapOf<Int, Int>().also { it.putAll(answers); it[currentQuestionIndex] = index } },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) bluePrimary.copy(0.1f) else Color.White),
                            border = if (isSelected) BorderStroke(2.dp, bluePrimary) else null
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSelected, onClick = { answers = mutableMapOf<Int, Int>().also { it.putAll(answers); it[currentQuestionIndex] = index } })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${('A' + index)}. $option", fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Navigation buttons
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                        enabled = currentQuestionIndex > 0
                    ) { Text("◀ Sebelum") }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(onClick = { currentQuestionIndex++ }) { Text("Selanjutnya ▶") }
                    } else {
                        Button(
                            onClick = { submitQuiz() },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            else Text("Kumpulkan Quiz")
                        }
                    }
                }
            }
        }
    }
}
