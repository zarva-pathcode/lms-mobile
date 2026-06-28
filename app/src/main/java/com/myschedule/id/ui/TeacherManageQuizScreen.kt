package com.myschedule.id.ui

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.myschedule.id.data.QuizRepository

@Composable
fun TeacherManageQuizScreen(navController: NavHostController, uniName: String, className: String) {
    var quizzes by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var refreshKey by remember { mutableStateOf(0) }
    var deleteTarget by remember { mutableStateOf<Map<String, Any>?>(null) }
    var statusTarget by remember { mutableStateOf<Map<String, Any>?>(null) }

    val context = LocalContext.current
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadQuizzes() {
        QuizRepository.getQuizzesByClass(uniName, className) {
            quizzes = it.sortedByDescending { q -> q["createdAt"] as? Comparable<Any> }
        }
    }

    LaunchedEffect(refreshKey) { loadQuizzes() }

    val entry = navController.currentBackStackEntry
    DisposableEffect(entry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (entry?.savedStateHandle?.get<Boolean>("quizCreated") == true) {
                    refreshKey++
                    entry?.savedStateHandle?.set("quizCreated", false)
                }
            }
        }
        entry?.lifecycle?.addObserver(observer)
        onDispose { entry?.lifecycle?.removeObserver(observer) }
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
                        Text("Kelola Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("teacher_create_quiz/$uniName/$className") }, containerColor = bluePrimary) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quizzes) { quiz ->
                QuizCard(
                    quiz = quiz,
                    primaryColor = bluePrimary,
                    onViewResults = { navController.navigate("teacher_quiz_results/${quiz["id"]}/${quiz["title"]}") },
                    onEdit = { navController.navigate("teacher_create_quiz/$uniName/$className?quizId=${quiz["id"]}") },
                    onDelete = { deleteTarget = quiz },
                    onToggleStatus = { statusTarget = quiz }
                )
            }

            if (quizzes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada quiz", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Konfirmasi hapus
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus Quiz") },
            text = { Text("Yakin ingin menghapus quiz \"${deleteTarget?.get("title")}\"? Semua jawaban siswa akan ikut terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        val qId = deleteTarget?.get("id")?.toString() ?: return@Button
                        deleteTarget = null
                        QuizRepository.deleteQuiz(qId,
                            onSuccess = {
                                Toast.makeText(context, "Quiz berhasil dihapus", Toast.LENGTH_SHORT).show()
                                refreshKey++
                            },
                            onError = { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }

    // Toggle status dialog
    if (statusTarget != null) {
        val quiz = statusTarget!!
        val currentStatus = quiz["status"]?.toString() ?: "active"
        val newStatus = if (currentStatus == "active") "draft" else "active"
        val label = if (newStatus == "active") "Aktifkan" else "Nonaktifkan"
        AlertDialog(
            onDismissRequest = { statusTarget = null },
            title = { Text(label) },
            text = { Text("${label} quiz \"${quiz["title"]}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        val qId = quiz["id"].toString()
                        statusTarget = null
                        QuizRepository.updateQuizStatus(qId, newStatus,
                            onSuccess = {
                                Toast.makeText(context, "Quiz ${if (newStatus == "active") "diaktifkan" else "dinonaktifkan"}", Toast.LENGTH_SHORT).show()
                                refreshKey++
                            },
                            onError = { Toast.makeText(context, "Gagal: $it", Toast.LENGTH_SHORT).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                ) { Text(label) }
            },
            dismissButton = { TextButton(onClick = { statusTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
fun QuizCard(quiz: Map<String, Any>, primaryColor: Color, onViewResults: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToggleStatus: () -> Unit) {
    val totalQuestions = (quiz["totalQuestions"] as? Long)?.toInt() ?: 0
    val timeLimit = (quiz["timeLimit"] as? Long)?.toInt() ?: 0
    val passingScore = (quiz["passingScore"] as? Long)?.toInt() ?: 0
    val status = quiz["status"]?.toString() ?: "active"
    val isActive = status == "active"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(primaryColor.copy(0.1f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Quiz, null, tint = primaryColor)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(quiz["title"].toString(), fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isActive) Color(0xFF10B981).copy(0.15f) else Color(0xFFF59E0B).copy(0.15f)
                        ) {
                            Text(
                                if (isActive) "Active" else "Draft",
                                fontSize = 10.sp,
                                color = if (isActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("$totalQuestions Soal", fontSize = 11.sp, color = Color.Gray)
                        Text("⏱ ${timeLimit}m", fontSize = 11.sp, color = Color.Gray)
                        Text("Lulus ≥ $passingScore", fontSize = 11.sp, color = Color.Gray)
                    }
                    Text("Deadline: ${quiz["deadline"]}", fontSize = 11.sp, color = primaryColor)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onViewResults, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Assessment, null, modifier = Modifier.size(16.dp), tint = primaryColor)
                    Spacer(Modifier.width(4.dp))
                    Text("Hasil", fontSize = 12.sp, color = primaryColor)
                }
                TextButton(onClick = onToggleStatus, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, modifier = Modifier.size(16.dp),
                        tint = if (isActive) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isActive) "Nonaktif" else "Aktifkan", fontSize = 12.sp, color = if (isActive) Color(0xFFF59E0B) else Color(0xFF10B981))
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus", fontSize = 12.sp, color = Color.Red)
                }
            }
        }
    }
}
