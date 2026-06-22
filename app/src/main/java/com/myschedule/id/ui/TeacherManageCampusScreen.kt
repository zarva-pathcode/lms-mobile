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
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherManageCampusScreen(navController: NavHostController) {
    var universitas by remember { mutableStateOf("") }
    var kelas by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // List universitas yang sudah pernah diinput oleh dosen ini
    var myCampusList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    fun loadMyCampus() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val managed = doc.get("managed_campus") as? List<Map<String, Any>> ?: listOf()
                myCampusList = managed
            }
    }

    LaunchedEffect(Unit) { loadMyCampus() }

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
                    Text("Kelola Kampus & Kelas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tambah Universitas/Kelas Baru", fontWeight = FontWeight.Bold, color = bluePrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = universitas,
                        onValueChange = { universitas = it },
                        label = { Text("Nama Universitas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = kelas,
                        onValueChange = { kelas = it },
                        label = { Text("Nama Kelas") },
                        placeholder = { Text("Contoh: TI-22-A") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = bluePrimary)
                    } else {
                        Button(
                            onClick = {
                                if (universitas.isBlank() || kelas.isBlank()) return@Button
                                isLoading = true

                                val uniRef = FirebaseInstance.db.collection("universities").document(universitas)
                                val teacherRef = FirebaseInstance.db.collection("users").document(uid)

                                FirebaseInstance.db.runTransaction { transaction ->
                                    val uniSnapshot = transaction.get(uniRef)
                                    val teacherDoc = transaction.get(teacherRef)

                                    @Suppress("UNCHECKED_CAST")
                                    val currentClasses = uniSnapshot.get("classes") as? List<String> ?: listOf()
                                    val isNewClassInUni = !currentClasses.contains(kelas)

                                    @Suppress("UNCHECKED_CAST")
                                    val managed = teacherDoc.get("managed_campus") as? List<Map<String, Any>> ?: listOf()
                                    val entryExists = managed.any { it["uni"] == universitas && it["kelas"] == kelas }

                                    if (isNewClassInUni) {
                                        val newClasses = currentClasses.toMutableList()
                                        newClasses.add(kelas)
                                        transaction.set(uniRef, mapOf("classes" to newClasses), SetOptions.merge())
                                    } else if (!uniSnapshot.exists()) {
                                        transaction.set(uniRef, mapOf("classes" to listOf(kelas)))
                                    }

                                    if (!entryExists) {
                                        val newManaged = managed.toMutableList()
                                        newManaged.add(mapOf("uni" to universitas, "kelas" to kelas))
                                        transaction.update(teacherRef, "managed_campus", newManaged)
                                    }

                                    null
                                }.addOnSuccessListener {
                                    isLoading = false
                                    message = "Berhasil ditambahkan"
                                    universitas = ""; kelas = ""
                                    loadMyCampus()
                                }.addOnFailureListener {
                                    isLoading = false
                                    message = "Gagal: ${it.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                        ) {
                            Text("Simpan", color = Color.White)
                        }
                    }
                    if (message.isNotEmpty()) {
                        Text(message, color = bluePrimary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Daftar Kampus & Kelas Saya", fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(myCampusList) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item["uni"].toString(), fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("Kelas: ${item["kelas"]}", color = Color.Gray, fontSize = 14.sp)
                            }
                            IconButton(onClick = {
                                val newList = myCampusList.filter { it != item }
                                FirebaseInstance.db.collection("users").document(uid)
                                    .update("managed_campus", newList)
                                    .addOnSuccessListener { loadMyCampus() }
                            }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}