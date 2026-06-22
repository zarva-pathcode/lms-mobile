package com.myschedule.id.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.SupabaseInstance
import com.myschedule.id.data.ClassMaterial
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManageMaterialsScreen(navController: NavHostController, uniName: String, className: String) {
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    var materials by remember { mutableStateOf<List<ClassMaterial>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun loadMaterials() {
        isLoading = true
        FirebaseInstance.db.collection("materials")
            .whereEqualTo("uniName", uniName)
            .whereEqualTo("className", className)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    ClassMaterial(
                        id = doc.id,
                        title = doc.getString("title") ?: "Materi Kuliah",
                        fileName = doc.getString("fileName") ?: "file",
                        fileUrl = doc.getString("fileUrl") ?: "",
                        date = doc.getString("date") ?: "-"
                    )
                }
                materials = list
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Gagal memuat: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(Unit) { loadMaterials() }

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
                        Text("Materi Kuliah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }, 
                containerColor = bluePrimary
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(bgLight)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = bluePrimary)
            } else if (materials.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("Belum ada materi diunggah.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(materials) { material ->
                        MaterialItemCardInternal2(material, bluePrimary) {
                            if (material.fileUrl.isNotEmpty()) {
                                try {
                                    val encodedUrl = Uri.encode(material.fileUrl)
                                    val encodedName = Uri.encode(material.fileName)
                                    navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Navigasi Error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddClassMaterialDialogInternal(
                uniName = uniName,
                className = className,
                bluePrimary = bluePrimary,
                onDismiss = { showAddDialog = false },
                onSuccess = {
                    showAddDialog = false
                    loadMaterials()
                    Toast.makeText(context, "Materi berhasil diunggah!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun AddClassMaterialDialogInternal(
    uniName: String, 
    className: String, 
    bluePrimary: Color,
    onDismiss: () -> Unit, 
    onSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseInstance.auth.currentUser?.uid ?: ""

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedFileUri = it
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst()) selectedFileName = c.getString(nameIndex)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Upload Materi Baru", color = bluePrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Materi") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                ) {
                    Icon(Icons.Default.AttachFile, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedFileName.isEmpty()) "Pilih File" else selectedFileName, color = Color.Black, maxLines = 1)
                }
                if (isUploading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = bluePrimary)
            }
        },
        confirmButton = {
            Button(
                enabled = !isUploading && title.isNotBlank() && selectedFileUri != null,
                colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                onClick = {
                    scope.launch {
                        isUploading = true
                        try {
                            val bytes = withContext(Dispatchers.IO) {
                                context.contentResolver.openInputStream(selectedFileUri!!)?.use { it.readBytes() }
                            }
                            if (bytes != null) {
                                // PERBAIKAN: Nama file unik dengan timestamp
                                val ext = selectedFileName.substringAfterLast('.', "")
                                val fileName = "mat_${System.currentTimeMillis()}${if(ext.isNotEmpty()) ".$ext" else ""}"
                                
                                val bucket = SupabaseInstance.client.storage.from("materials")
                                withContext(Dispatchers.IO) { bucket.upload(fileName, bytes) { upsert = true } }
                                val fileUrl = bucket.publicUrl(fileName)

                                val data = hashMapOf(
                                    "title" to title,
                                    "fileName" to selectedFileName,
                                    "fileUrl" to fileUrl,
                                    "uniName" to uniName,
                                    "className" to className,
                                    "teacherId" to currentUserId,
                                    "date" to SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                                )

                                FirebaseInstance.db.collection("materials").add(data)
                                    .addOnSuccessListener { onSuccess() }
                                    .addOnFailureListener { e -> 
                                        isUploading = false
                                        Toast.makeText(context, "Firestore Error", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } catch (e: Exception) {
                            isUploading = false
                            Toast.makeText(context, "Upload Gagal", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            ) { Text("Upload", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
fun MaterialItemCardInternal2(material: ClassMaterial, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(primaryColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LibraryBooks, null, tint = primaryColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(material.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(material.date, fontSize = 11.sp, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}