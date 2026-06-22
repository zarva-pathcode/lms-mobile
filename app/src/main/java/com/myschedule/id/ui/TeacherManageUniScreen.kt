package com.myschedule.id.ui

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
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManageUniScreen(navController: NavHostController, uniName: String) {
    var managedClasses by remember { mutableStateOf(listOf<String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    // Fungsi refresh data
    fun loadData() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val managed = doc.get("managed_campus") as? List<Map<String, String>> ?: listOf()
                managedClasses = managed.filter { it["uni"] == uniName }.mapNotNull { it["kelas"] }
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
                        Text(uniName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Kelola kelas Anda", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = bluePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
            }
        }
    ) { paddingValues ->
        // Dialog Tambah Kelas
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { if (!isLoading) showDialog = false },
                title = { Text("Tambah Kelas Baru") },
                text = {
                    Column {
                        Text("Masukkan nama kelas untuk $uniName", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newClassName,
                            onValueChange = { newClassName = it },
                            label = { Text("Nama Kelas") },
                            placeholder = { Text("Contoh: TI-22-A") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newClassName.isBlank()) return@Button
                            isLoading = true

                            val uniRef = FirebaseInstance.db.collection("universities").document(uniName)
                            val userRef = FirebaseInstance.db.collection("users").document(uid)

                            uniRef.set(
                                mapOf("classes" to FieldValue.arrayUnion(newClassName)),
                                SetOptions.merge()
                            ).addOnSuccessListener {
                                userRef.get().addOnSuccessListener { userSnap ->
                                    @Suppress("UNCHECKED_CAST")
                                    val managed = userSnap.get("managed_campus") as? List<Map<String, String>> ?: listOf()
                                    if (!managed.any { it["uni"] == uniName && it["kelas"] == newClassName }) {
                                        val newEntry = mapOf("uni" to uniName, "kelas" to newClassName)
                                        userRef.update("managed_campus", FieldValue.arrayUnion(newEntry))
                                            .addOnSuccessListener {
                                                isLoading = false
                                                showDialog = false
                                                newClassName = ""
                                                loadData()
                                            }
                                            .addOnFailureListener { isLoading = false }
                                    } else {
                                        isLoading = false
                                        showDialog = false
                                        newClassName = ""
                                    }
                                }.addOnFailureListener { isLoading = false }
                            }.addOnFailureListener { isLoading = false }
                        },
                        enabled = !isLoading && newClassName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Simpan", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }, enabled = !isLoading) {
                        Text("Batal")
                    }
                }
            )
        }

        // List Kelas
        if (managedClasses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Belum ada kelas. Klik + untuk menambah.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(managedClasses) { className ->
                    ClassCardInternal2(className, bluePrimary) {
                        navController.navigate("teacher_class_dashboard/${uniName}/${className}")
                    }
                }
            }
        }
    }
}

@Composable
fun ClassCardInternal2(className: String, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Class, null, tint = primaryColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(className, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
