package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassStudentsScreen(navController: NavHostController, uniName: String, className: String) {
    var students by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    LaunchedEffect(Unit) {
        isLoading = true
        FirebaseInstance.db.collection("users")
            .whereEqualTo("role", "student")
            .whereEqualTo("universitas", uniName)
            .get()
            .addOnSuccessListener { snapshot ->
                // Filter mahasiswa yang mengambil kelas ini berdasarkan field 'enrolled_classes' di Firestore
                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    @Suppress("UNCHECKED_CAST")
                    val enrolled = data?.get("enrolled_classes") as? List<String> ?: listOf()
                    if (enrolled.contains(className)) data else null
                }
                students = list
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
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
                        Text("Daftar Mahasiswa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$className | $uniName", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = bluePrimary)
            } else if (students.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.PeopleOutline, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Belum ada mahasiswa di kelas ini.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Total Mahasiswa: ${students.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(students) { student ->
                        StudentCardInternal(student, bluePrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCardInternal(student: Map<String, Any>, primaryColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Inisial atau Foto
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student["name"].toString().take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student["name"].toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "NIM: ${student["studentId"] ?: "-"}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Text(
                    text = student["email"].toString(),
                    fontSize = 12.sp,
                    color = primaryColor.copy(alpha = 0.8f)
                )
            }
            
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
