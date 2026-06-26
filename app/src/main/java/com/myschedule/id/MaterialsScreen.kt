package com.myschedule.id

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.net.URLEncoder

// DATA CLASS
data class MaterialItem(
    val id: String = "",
    val title: String,
    val course: String,
    val date: String,
    val fileUrl: String,
    val fileName: String,
    val linkUrl: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialsScreen(navController: NavHostController) {
    var materials by remember { mutableStateOf<List<MaterialItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRole = UserSession.role

    fun loadMaterials() {
        isLoading = true
        FirebaseInstance.db.collection("materials")
            .get()
            .addOnSuccessListener { snapshot ->
                materials = snapshot.documents.map { doc ->
                    MaterialItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        course = doc.getString("course") ?: "",
                        date = doc.getString("date") ?: "",
                        fileUrl = doc.getString("fileUrl") ?: "",
                        fileName = doc.getString("fileName") ?: "File Materi",
                        linkUrl = doc.getString("linkUrl") ?: ""
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    LaunchedEffect(Unit) { loadMaterials() }

    Scaffold(
        topBar = {
            HeaderSection()
        },
        floatingActionButton = {
            if (userRole == "dosen") {
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF1565C0)) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color(0xFFF8F7FF))) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF1565C0))
            } else if (materials.isEmpty()) {
                Text("Belum ada materi.", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    items(materials) { material ->
                        MaterialCard(material, navController)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMaterialDialog(
                onDismiss = { showAddDialog = false },
                onSuccess = {
                    showAddDialog = false
                    loadMaterials()
                }
            )
        }
    }
}

@Composable
fun AddMaterialDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedFileUri = it
            // PERBAIKAN: Ambil nama asli beserta ekstensi
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) selectedFileName = c.getString(nameIndex)
            }
            if (selectedFileName.isEmpty()) selectedFileName = "materi.pdf"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Tambah Materi Baru", color = Color(0xFF1565C0)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Materi") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Mata Kuliah") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.UploadFile, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFileName.isEmpty()) "Pilih File" else selectedFileName, color = Color.Black, maxLines = 1)
                }

                OutlinedTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    label = { Text("Link URL (opsional)") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (isUploading) {
                    Text("Sedang mengupload...", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1565C0))
                }
            }
        },
        confirmButton = {
            val isLinkOnly = selectedFileUri == null && linkUrl.isNotBlank()
            Button(
                enabled = !isUploading && title.isNotBlank() && (selectedFileUri != null || linkUrl.isNotBlank()),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                onClick = {
                    val data = hashMapOf(
                        "title" to title,
                        "course" to course,
                        "fileUrl" to "",
                        "fileName" to if (selectedFileUri != null) selectedFileName else "",
                        "linkUrl" to linkUrl,
                        "date" to java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    )

                    if (isLinkOnly) {
                        FirebaseInstance.db.collection("materials").add(data)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener {
                                Toast.makeText(context, "Gagal simpan", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                    } else {
                        scope.launch {
                            isUploading = true
                            try {
                                val bytes = context.contentResolver.openInputStream(selectedFileUri!!)?.readBytes()
                                if (bytes != null) {
                                    val ext = selectedFileName.substringAfterLast('.', "pdf")
                                    val storageName = "mat_${System.currentTimeMillis()}.$ext"
                                    val bucket = SupabaseInstance.client.storage.from("materials")
                                    bucket.upload(storageName, bytes) { upsert = true }
                                    data["fileUrl"] = bucket.publicUrl(storageName)
                                }

                                FirebaseInstance.db.collection("materials").add(data)
                                    .addOnSuccessListener {
                                        isUploading = false
                                        onSuccess()
                                    }
                                    .addOnFailureListener {
                                        isUploading = false
                                        Toast.makeText(context, "Gagal simpan", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                            } catch (e: Exception) {
                                isUploading = false
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        }
                    }
                }
            ) { Text("Simpan", color = Color.White) }
        },
        dismissButton = {
            TextButton(enabled = !isUploading, onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun HeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFF1565C0), Color(0xFF64B5F6))))
            .statusBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Materi Kuliah",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun MaterialCard(material: MaterialItem, navController: NavHostController) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFF1565C0).copy(0.1f), shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, tint = Color(0xFF1565C0))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = material.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Text(text = material.course, fontSize = 13.sp, color = Color.Gray)
                    Text(text = material.date, fontSize = 12.sp, color = Color(0xFF1565C0))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (material.fileUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val encodedUrl = URLEncoder.encode(material.fileUrl, "UTF-8")
                            val encodedName = URLEncoder.encode(material.fileName, "UTF-8")
                            navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("File: ${material.fileName}", fontSize = 13.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium)
                }
            }

            if (material.linkUrl.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(material.linkUrl))
                            context.startActivity(intent)
                        }
                        .padding(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Link, null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Link", fontSize = 13.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
