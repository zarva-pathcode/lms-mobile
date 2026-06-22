package com.myschedule.id.ui

import android.net.Uri
import android.widget.Toast
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
import com.myschedule.id.data.ClassMaterial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMaterialsScreen(navController: NavHostController, uniName: String, className: String) {
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    var materials by remember { mutableStateOf<List<ClassMaterial>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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
            .addOnFailureListener { e ->
                isLoading = false
                Toast.makeText(context, "Gagal memuat materi", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(uniName, className) { 
        loadMaterials() 
    }

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
                        Text("Materi Kuliah", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(className, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(bgLight)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = bluePrimary)
            } else if (materials.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryBooks, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("Belum ada materi tersedia.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(materials) { material ->
                        MaterialItemCardInternal(material, bluePrimary) {
                            if (material.fileUrl.isNotEmpty()) {
                                // PERBAIKAN: Gunakan Uri.encode untuk keamanan navigasi query param
                                val encodedUrl = Uri.encode(material.fileUrl)
                                val encodedName = Uri.encode(material.fileName)
                                navController.navigate("file_viewer?fileUrl=$encodedUrl&fileName=$encodedName")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialItemCardInternal(material: ClassMaterial, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(primaryColor.copy(0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LibraryBooks, null, tint = primaryColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(material.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(material.fileName, fontSize = 12.sp, color = Color.Gray)
                Text(material.date, fontSize = 11.sp, color = primaryColor)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
