package com.myschedule.id.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.data.QuizRepository
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class QuestionState(
    val questionText: String = "",
    val options: List<String> = List(4) { "" },
    val correctAnswer: Int = -1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherCreateQuizScreen(navController: NavHostController, uniName: String, className: String, quizId: String = "") {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadlineDate by remember { mutableStateOf("") }
    var deadlineTime by remember { mutableStateOf("23:59") }
    var timeLimit by remember { mutableStateOf("30") }
    var passingScore by remember { mutableStateOf("70") }
    var questions by remember { mutableStateOf(listOf<QuestionState>()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    val datePickerState = rememberDatePickerState()
    val draftKey = if (quizId.isNotEmpty()) "draft_edit_$quizId" else "draft"

    fun loadDraft() {
        val prefs = context.getSharedPreferences("quiz_draft", Context.MODE_PRIVATE)
        val draftJson = prefs.getString(draftKey, null) ?: return
        try {
            val json = JSONObject(draftJson)
            title = json.optString("title", "")
            description = json.optString("description", "")
            deadlineDate = json.optString("deadlineDate", "")
            deadlineTime = json.optString("deadlineTime", "23:59")
            timeLimit = json.optString("timeLimit", "30")
            passingScore = json.optString("passingScore", "70")
            val questionsArray = json.optJSONArray("questions")
            if (questionsArray != null) {
                questions = (0 until questionsArray.length()).map { i ->
                    val q = questionsArray.getJSONObject(i)
                    val optsArray = q.optJSONArray("options")
                    val opts = if (optsArray != null) {
                        (0 until optsArray.length()).map { optsArray.getString(it) }
                    } else List(4) { "" }
                    QuestionState(
                        questionText = q.optString("questionText", ""),
                        options = opts,
                        correctAnswer = q.optInt("correctAnswer", -1)
                    )
                }
            }
        } catch (_: Exception) {}
    }

    fun saveDraft() {
        val prefs = context.getSharedPreferences("quiz_draft", Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("deadlineDate", deadlineDate)
            put("deadlineTime", deadlineTime)
            put("timeLimit", timeLimit)
            put("passingScore", passingScore)
            put("questions", JSONArray(questions.map { q ->
                JSONObject().apply {
                    put("questionText", q.questionText)
                    put("options", JSONArray(q.options))
                    put("correctAnswer", q.correctAnswer)
                }
            }))
        }
        prefs.edit().putString(draftKey, json.toString()).apply()
    }

    fun clearDraft() {
        context.getSharedPreferences("quiz_draft", Context.MODE_PRIVATE).edit().remove(draftKey).apply()
    }

    LaunchedEffect(Unit) { loadDraft() }

    // Load quiz data for edit mode
    LaunchedEffect(quizId) {
        if (quizId.isNotEmpty()) {
            isEditMode = true
            QuizRepository.getQuiz(quizId) { quiz ->
                if (quiz != null) {
                    title = quiz["title"]?.toString() ?: ""
                    description = quiz["description"]?.toString() ?: ""
                    val deadline = quiz["deadline"]?.toString() ?: ""
                    if (deadline.contains(" ")) {
                        deadlineDate = deadline.substringBefore(" ")
                        deadlineTime = deadline.substringAfter(" ")
                    }
                    timeLimit = ((quiz["timeLimit"] as? Long)?.toInt() ?: 30).toString()
                    passingScore = ((quiz["passingScore"] as? Long)?.toInt() ?: 70).toString()
                    val qList = quiz["questions"] as? List<Map<String, Any>> ?: listOf()
                    questions = qList.map { q ->
                        QuestionState(
                            questionText = q["questionText"]?.toString() ?: "",
                            options = (q["options"] as? List<String>) ?: List(4) { "" },
                            correctAnswer = ((q["correctAnswer"] as? Long)?.toInt() ?: -1)
                        )
                    }
                }
            }
        }
    }

    fun addQuestion() {
        questions = questions + QuestionState()
        saveDraft()
    }

    fun removeQuestion(index: Int) {
        questions = questions.toMutableList().apply { removeAt(index) }
        saveDraft()
    }

    fun updateQuestion(index: Int, update: (QuestionState) -> QuestionState) {
        questions = questions.toMutableList().apply { this[index] = update(this[index]) }
        saveDraft()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isEditMode) "Edit Quiz" else "Buat Quiz Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; saveDraft() },
                    label = { Text("Judul Quiz") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; saveDraft() },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = deadlineDate,
                        onValueChange = {},
                        label = { Text("Tanggal Deadline") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = deadlineTime,
                            onValueChange = {},
                            label = { Text("Jam (HH:mm)") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.Schedule, null) }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTimePicker = true }
                        )
                    }
                    OutlinedTextField(
                        value = timeLimit,
                        onValueChange = {
                        it.filter { c -> c.isDigit() }.let { v ->
                            if (v.isEmpty() || (v.length <= 3 && v.toInt() >= 1)) { timeLimit = v; saveDraft() }
                        }
                        },
                        label = { Text("Timer (menit)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = passingScore,
                    onValueChange = {
                        it.filter { c -> c.isDigit() }.let { v ->
                            if (v.isEmpty() || (v.toInt() in 1..100)) { passingScore = v; saveDraft() }
                        }
                    },
                    label = { Text("Nilai Lulus (1-100)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                HorizontalDivider()
                Text("Soal", fontWeight = FontWeight.Bold, color = bluePrimary)
            }

            itemsIndexed(questions) { index, q ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Soal ${index + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { removeQuestion(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                        OutlinedTextField(
                            value = q.questionText,
                            onValueChange = { newText -> updateQuestion(index) { it.copy(questionText = newText) } },
                            label = { Text("Pertanyaan") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        q.options.forEachIndexed { optIndex, opt ->
                            OutlinedTextField(
                                value = opt,
                                onValueChange = { newVal ->
                                    val newOptions = q.options.toMutableList().also { it[optIndex] = newVal }
                                    updateQuestion(index) { it.copy(options = newOptions) }
                                },
                                label = { Text("Opsi ${('A' + optIndex)}") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Jawaban Benar:", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            q.options.indices.forEach { optIndex ->
                                FilterChip(
                                    selected = q.correctAnswer == optIndex,
                                    onClick = { updateQuestion(index) { it.copy(correctAnswer = optIndex) } },
                                    label = { Text("${'A' + optIndex}", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF10B981).copy(0.2f))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = { addQuestion() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Soal")
                }
            }

            if (errorMessage.isNotEmpty()) {
                item { Text(errorMessage, color = Color.Red, fontSize = 13.sp) }
            }

            if (isSubmitting) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.isBlank() || deadlineDate.isBlank() || timeLimit.isBlank() || passingScore.isBlank()) {
                            errorMessage = "Isi semua field wajib"; return@Button
                        }
                        if (timeLimit.toInt() < 1) { errorMessage = "Timer minimal 1 menit"; return@Button }
                        if (passingScore.toInt() < 1) { errorMessage = "Nilai lulus minimal 1"; return@Button }
                        if (questions.isEmpty()) {
                            errorMessage = "Tambah minimal 1 soal"; return@Button
                        }
                        if (questions.any { it.questionText.isBlank() || it.options.any { o -> o.isBlank() } || it.correctAnswer < 0 }) {
                            errorMessage = "Lengkapi semua soal"; return@Button
                        }

                        isSubmitting = true
                        errorMessage = ""
                        val questionMaps = questions.map { q -> mapOf("questionText" to q.questionText, "options" to q.options, "correctAnswer" to q.correctAnswer) }

                        if (isEditMode) {
                            QuizRepository.updateQuiz(
                                quizId, title, description, "$deadlineDate $deadlineTime",
                                timeLimit.toInt(), passingScore.toInt(), questionMaps,
                                onSuccess = {
                                    Toast.makeText(context, "Quiz berhasil diupdate", Toast.LENGTH_SHORT).show()
                                    navController.previousBackStackEntry?.savedStateHandle?.set("quizCreated", true)
                                    navController.popBackStack()
                                },
                                onError = { errorMessage = it; isSubmitting = false }
                            )
                        } else {
                            QuizRepository.createQuiz(
                                uniName, className, title, description, "$deadlineDate $deadlineTime",
                                timeLimit.toInt(), passingScore.toInt(), questionMaps,
                                onSuccess = {
                                    clearDraft()
                                    Toast.makeText(context, "Quiz berhasil dibuat", Toast.LENGTH_SHORT).show()
                                    navController.previousBackStackEntry?.savedStateHandle?.set("quizCreated", true)
                                    navController.popBackStack()
                                },
                                onError = { errorMessage = it; isSubmitting = false }
                            )
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                ) {
                    Text(if (isEditMode) "Simpan" else "Buat Quiz")
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        deadlineDate = sdf.format(Date(millis))
                        saveDraft()
                    }
                    showDatePicker = false
                }) { Text("OK", color = bluePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val parts = deadlineTime.split(":").map { it.toIntOrNull() ?: 0 }
        val timeState = rememberTimePickerState(
            initialHour = parts.getOrElse(0) { 23 }.coerceIn(0, 23),
            initialMinute = parts.getOrElse(1) { 59 }.coerceIn(0, 59),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    deadlineTime = "${timeState.hour.toString().padStart(2, '0')}:${timeState.minute.toString().padStart(2, '0')}"
                    saveDraft()
                    showTimePicker = false
                }) { Text("OK", color = bluePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal", color = Color.Gray) }
            }
        )
    }
}
