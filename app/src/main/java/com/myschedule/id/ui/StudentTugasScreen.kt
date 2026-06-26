package com.myschedule.id.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.SupabaseInstance
import com.myschedule.id.data.TaskRepository
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentTugasScreen(navController: NavHostController, uniName: String, className: String) {
    var studentName by remember { mutableStateOf("") }
    var classTasks by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var refreshKey by remember { mutableStateOf(0) }
    
    var selectedTaskForSubmission by remember { mutableStateOf<Map<String, Any>?>(null) }
    var currentStudentSubmission by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var isLinkMode by remember { mutableStateOf(false) }
    var submissionLinkUrl by remember { mutableStateOf("") }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    fun loadData() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("name") ?: "Mahasiswa"
                TaskRepository.getTasksByClass(uniName, className) { tasks ->
                    classTasks = tasks.sortedByDescending { it["createdAt"] as? com.google.firebase.Timestamp }
                    refreshKey++
                }
            }
    }

    LaunchedEffect(Unit) { loadData() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            // Ambil nama asli file beserta ekstensinya dari ContentResolver
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) {
                    selectedFileName = c.getString(nameIndex)
                }
            }
            if (selectedFileName.isEmpty()) selectedFileName = "file_tugas"
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight))).statusBarsPadding()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("Tugas: $className", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(uniName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)) {
            if (classTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada tugas untuk kelas ini.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(classTasks) { task ->
                        var studentSubmission by remember { mutableStateOf<Map<String, Any>?>(null) }
                        
                        LaunchedEffect(task["id"], refreshKey) {
                            TaskRepository.getStudentSubmission(task["id"] as String, uid) {
                                studentSubmission = it
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(task["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f), color = Color.Black)
                                    val deadline = task["deadline"].toString()
                                    Text(deadline, fontSize = 11.sp, color = Color.Red)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(task["description"].toString(), fontSize = 13.sp, color = Color.DarkGray)
                                
                                if (task["fileUrl"] != null && task["fileUrl"].toString().isNotEmpty()) {
                                    TextButton(onClick = {
                                        val url = task["fileUrl"].toString()
                                        val name = task["title"].toString()
                                        val encodedUrl = Uri.encode(url)
                                        val encodedName = Uri.encode(name)
                                        navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                    }) {
                                        Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(16.dp), tint = bluePrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lihat Lampiran Tugas", fontSize = 12.sp, color = bluePrimary)
                                    }
                                }
                                
                                val taskLinkUrl = task["linkUrl"]?.toString() ?: ""
                                if (taskLinkUrl.isNotEmpty() && taskLinkUrl != "null") {
                                    TextButton(onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(taskLinkUrl))
                                        context.startActivity(intent)
                                    }) {
                                        Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp), tint = bluePrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lihat Link Tugas", fontSize = 12.sp, color = bluePrimary)
                                    }
                                }

                                if (studentSubmission != null) {
                                    val grade = studentSubmission!!["grade"]?.toString() ?: ""
                                    val subFileUrl = studentSubmission!!["fileUrl"]?.toString() ?: ""
                                    val subFileName = studentSubmission!!["fileName"]?.toString() ?: ""
                                    val subLink = studentSubmission!!["submissionLink"]?.toString() ?: ""
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = if (grade.isNotEmpty()) Color(0xFF10B981).copy(0.1f) else Color.Gray.copy(0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = if (grade.isNotEmpty()) "Nilai: $grade" else "Status: Sudah Dikumpulkan",
                                                fontSize = 12.sp,
                                                color = if (grade.isNotEmpty()) Color(0xFF059669) else Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (subFileUrl.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val encodedUrl = Uri.encode(subFileUrl)
                                                        val encodedName = Uri.encode(subFileName)
                                                        navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                                    }.padding(vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("File: $subFileName", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            if (subLink.isNotEmpty()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(subLink))
                                                        context.startActivity(intent)
                                                    }.padding(vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Lihat Link", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                            Text("(Klik untuk lihat)", fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = { 
                                        val sub = studentSubmission
                                        selectedTaskForSubmission = task
                                        currentStudentSubmission = sub
                                        selectedFileName = sub?.get("fileName")?.toString() ?: ""
                                        submissionLinkUrl = sub?.get("submissionLink")?.toString() ?: ""
                                        isLinkMode = sub?.get("submissionLink")?.toString()?.isNotEmpty() == true
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                                ) {
                                    val hasSubmissionLink = studentSubmission?.get("submissionLink")?.toString()?.isNotEmpty() ?: false
                                    Text(
                                        if (studentSubmission != null) {
                                            if (hasSubmissionLink) "Ganti Link" else "Ganti File"
                                        } else "Kumpulkan Tugas",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTaskForSubmission != null) {
            AlertDialog(
                onDismissRequest = { if (!isUploading) selectedTaskForSubmission = null },
                title = { Text("Kumpulkan Tugas") },
                text = {
                    Column {
                        Text(selectedTaskForSubmission!!["title"].toString(), fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { isLinkMode = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isLinkMode) bluePrimary else Color.LightGray
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload File", fontSize = 12.sp, color = if (!isLinkMode) Color.White else Color.Black)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { isLinkMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLinkMode) bluePrimary else Color.LightGray
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Link URL", fontSize = 12.sp, color = if (isLinkMode) Color.White else Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isLinkMode) {
                            OutlinedTextField(
                                value = submissionLinkUrl,
                                onValueChange = { submissionLinkUrl = it },
                                label = { Text("Link URL") },
                                placeholder = { Text("https://...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                enabled = !isUploading
                            ) {
                                Icon(Icons.Default.UploadFile, null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedFileName.isEmpty()) "Pilih File" else selectedFileName, color = Color.Black, maxLines = 1)
                            }
                        }

                        if (isUploading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = bluePrimary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isUploading && (if (isLinkMode) submissionLinkUrl.isNotBlank() else selectedFileUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                        onClick = {
                            scope.launch {
                                isUploading = true
                                try {
                                    if (isLinkMode) {
                                        TaskRepository.submitTaskLink(
                                            selectedTaskForSubmission!!["id"] as String,
                                            uid, studentName, submissionLinkUrl,
                                            onSuccess = {
                                                isUploading = false
                                                selectedTaskForSubmission = null
                                                selectedFileUri = null
                                                selectedFileName = ""
                                                submissionLinkUrl = ""
                                                isLinkMode = false
                                                loadData()
                                                Toast.makeText(context, "Tugas berhasil dikumpulkan", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = {
                                                isUploading = false
                                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                Log.e("StudentTugasScreen", it)
                                            }
                                        )
                                    } else {
                                        val bytes = context.contentResolver.openInputStream(selectedFileUri!!)?.readBytes()
                                        if (bytes != null) {
                                            val fileName = "${uid}_${System.currentTimeMillis()}_${selectedFileName.replace(" ", "_")}"
                                            val bucket = SupabaseInstance.client.storage.from("submissions")
                                            bucket.upload(fileName, bytes) { upsert = true }
                                            val publicUrl = bucket.publicUrl(fileName)
                                            
                                            TaskRepository.submitTaskFile(
                                                selectedTaskForSubmission!!["id"] as String,
                                                uid, studentName, publicUrl, selectedFileName,
                                                onSuccess = {
                                                    isUploading = false
                                                    selectedTaskForSubmission = null
                                                    selectedFileUri = null
                                                    selectedFileName = ""
                                                    submissionLinkUrl = ""
                                                    isLinkMode = false
                                                    loadData()
                                                    Toast.makeText(context, "Tugas berhasil dikumpulkan", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { 
                                                    isUploading = false
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                    Log.e("StudentTugasScreen", it)
                                                }
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    isUploading = false
                                    Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) { Text("Kirim", color = Color.White) }
                },
                dismissButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val taskId = selectedTaskForSubmission!!["id"] as String
                        if (currentStudentSubmission != null) {
                            TextButton(
                                enabled = !isUploading,
                                onClick = {
                                    TaskRepository.deleteSubmission(taskId, uid, {
                                        currentStudentSubmission = null
                                        selectedTaskForSubmission = null
                                        loadData()
                                        Toast.makeText(context, "Submission dihapus", Toast.LENGTH_SHORT).show()
                                    }, {
                                        Toast.makeText(context, "Gagal hapus: $it", Toast.LENGTH_SHORT).show()
                                    })
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hapus")
                            }
                        }
                        TextButton(enabled = !isUploading, onClick = { selectedTaskForSubmission = null }) { Text("Batal") }
                    }
                }
            )
        }
    }
}