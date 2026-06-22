package com.myschedule.id.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavHostController
import com.myschedule.id.SupabaseInstance
import com.myschedule.id.data.TaskRepository
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManageTasksScreen(navController: NavHostController, uniName: String, className: String) {
    var tasks by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var taskDeadline by remember { mutableStateOf("") }
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val calendar = Calendar.getInstance()
    
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadTasks() {
        TaskRepository.getTasksByClass(uniName, className) { 
            tasks = it.sortedByDescending { task -> 
                task["createdAt"] as? com.google.firebase.Timestamp 
            }
        }
    }

    LaunchedEffect(Unit) { loadTasks() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
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

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            taskDeadline = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("Kelola Tugas: $className", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(uniName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }, 
                containerColor = bluePrimary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada tugas yang dibuat.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tasks) { task ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(task["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                Text(task["description"].toString(), fontSize = 14.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Event, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Deadline: ${task["deadline"]}", fontSize = 12.sp, color = Color.Red)
                                }
                                
                                val fileUrl = task["fileUrl"]?.toString() ?: ""
                                val fileName = task["fileName"]?.toString() ?: task["title"].toString()
                                
                                if (fileUrl.isNotEmpty() && fileUrl != "null") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            // PERBAIKAN: Gunakan Uri.encode untuk keamanan navigasi
                                            val encodedUrl = Uri.encode(fileUrl)
                                            val encodedName = Uri.encode(fileName)
                                            navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                        }
                                    ) {
                                        Icon(Icons.Default.AttachFile, null, tint = bluePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lihat Lampiran Tugas", fontSize = 12.sp, color = bluePrimary, fontWeight = FontWeight.Medium)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        navController.navigate("teacher_task_submissions/${task["id"]}/${task["title"]}")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                                ) {
                                    Text("Lihat Pengumpulan", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { if (!isUploading) showCreateDialog = false },
                title = { Text("Buat Tugas Baru", color = bluePrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Judul Tugas") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = taskDesc,
                            onValueChange = { taskDesc = it },
                            label = { Text("Deskripsi") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                            OutlinedTextField(
                                value = taskDeadline,
                                onValueChange = {},
                                label = { Text("Deadline") },
                                placeholder = { Text("Pilih Tanggal") },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Lampiran File:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                        
                        if (isUploading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = bluePrimary)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !isUploading && taskTitle.isNotBlank() && taskDeadline.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                        onClick = {
                            scope.launch {
                                isUploading = true
                                var finalFileUrl: String? = null
                                var finalFileName: String? = null
                                
                                try {
                                    if (selectedFileUri != null) {
                                        val bytes = withContext(Dispatchers.IO) {
                                            context.contentResolver.openInputStream(selectedFileUri!!)?.use { it.readBytes() }
                                        }
                                        if (bytes != null) {
                                            val ext = selectedFileName.substringAfterLast('.', "pdf")
                                            val storageName = "task_${System.currentTimeMillis()}.${ext}"
                                            val bucket = SupabaseInstance.client.storage.from("assignments")
                                            
                                            withContext(Dispatchers.IO) {
                                                bucket.upload(storageName, bytes) { upsert = true }
                                            }
                                            finalFileUrl = bucket.publicUrl(storageName)
                                            finalFileName = selectedFileName
                                        }
                                    }
                                    
                                    TaskRepository.createTask(uniName, className, taskTitle, taskDesc, taskDeadline, finalFileUrl, finalFileName, {
                                        Toast.makeText(context, "Tugas berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                        showCreateDialog = false
                                        taskTitle = ""; taskDesc = ""; taskDeadline = ""; selectedFileUri = null; selectedFileName = ""
                                        isUploading = false
                                        loadTasks()
                                    }, {
                                        isUploading = false
                                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    })
                                    
                                } catch (e: Exception) {
                                    isUploading = false
                                    Toast.makeText(context, "Gagal upload: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) { Text("Simpan", color = Color.White) }
                },
                dismissButton = {
                    TextButton(enabled = !isUploading, onClick = { showCreateDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
