package com.myschedule.id.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.google.firebase.firestore.FieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentEnrollClassScreen(navController: NavHostController) {
    var availableClasses by remember { mutableStateOf(listOf<String>()) }
    var enrolledClasses by remember { mutableStateOf(listOf<String>()) }
    var universitas by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadData() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                universitas = doc.getString("universitas") ?: ""
                @Suppress("UNCHECKED_CAST")
                enrolledClasses = doc.get("enrolled_classes") as? List<String> ?: listOf()
                
                if (universitas.isNotEmpty()) {
                    FirebaseInstance.db.collection("universities").document(universitas).get()
                        .addOnSuccessListener { uniDoc ->
                            @Suppress("UNCHECKED_CAST")
                            val classes = uniDoc.get("classes") as? List<String> ?: listOf()
                            availableClasses = classes
                        }
                }
            }
    }

    LaunchedEffect(Unit) { loadData() }

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
                        Text("Ambil Kelas Baru", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(universitas, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (message.isNotEmpty()) {
                Text(message, color = bluePrimary, modifier = Modifier.padding(bottom = 8.dp))
            }

            Text("Daftar Kelas di Kampus Anda", fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableClasses) { className ->
                    val isEnrolled = enrolledClasses.contains(className)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(className, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Color.Black)
                            if (isEnrolled) {
                                Text("Sudah Diambil", color = Color.Gray, fontSize = 12.sp)
                            } else {
                                Button(
                                    onClick = {
                                        FirebaseInstance.db.collection("users").document(uid)
                                            .update("enrolled_classes", FieldValue.arrayUnion(className))
                                            .addOnSuccessListener {
                                                message = "Berhasil mengambil kelas $className"
                                                loadData()
                                            }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                                ) {
                                    Text("Ambil", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
