package com.myschedule.id

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

@Composable
fun ProfileDosenScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isEditMode by remember { mutableStateOf(false) }

    var nama by remember { mutableStateOf(UserSession.username) }
    var nidn by remember { mutableStateOf("0123456789") }
    var email by remember { mutableStateOf(UserSession.email.ifEmpty { "dosen@kampus.ac.id" }) }
    var noHp by remember { mutableStateOf("081234567890") }
    var prodi by remember { mutableStateOf("Teknik Informatika") }
    var jabatan by remember { mutableStateOf("Dosen Tetap") }
    var bidangKeahlian by remember { mutableStateOf("Artificial Intelligence") }
    var alamat by remember { mutableStateOf("Jl. Pendidikan No. 1") }
    var bio by remember {
        mutableStateOf(
            "Dosen aktif yang berfokus pada pengajaran, penelitian, dan pengabdian masyarakat."
        )
    }

    var profileUri by remember { mutableStateOf<String?>(UserSession.profileImageUri) }
    var isUploading by remember { mutableStateOf(false) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: "dosen_default"

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isUploading = true
                try {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                    if (bytes != null) {
                        val fileName = "$uid.jpg"
                        val bucket = SupabaseInstance.client.storage.from("avatars")
                        
                        // Upload ke Supabase (Sintaks V3)
                        bucket.upload(fileName, bytes) {
                            upsert = true
                        }
                        
                        // Ambil Public URL dengan cache buster (timestamp)
                        val timestamp = System.currentTimeMillis()
                        val publicUrl = "${bucket.publicUrl(fileName)}?t=$timestamp"
                        
                        // Update Firestore & Session
                        FirebaseInstance.db.collection("users").document(uid)
                            .update("profileImageUri", publicUrl)
                            .addOnSuccessListener {
                                UserSession.profileImageUri = publicUrl
                                profileUri = publicUrl
                                UserSession.updateProfileImage(context, publicUrl)

                                // Paksa Coil untuk hapus cache
                                context.imageLoader.diskCache?.remove(publicUrl)
                                context.imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(publicUrl))

                                Toast.makeText(context, "Foto profil diperbarui", Toast.LENGTH_SHORT).show()
                            }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profil Dosen",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            val painter = if (profileUri.isNullOrEmpty()) {
                painterResource(id = R.drawable.profile)
            } else {
                rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = profileUri)
                        .apply(block = fun ImageRequest.Builder.() {
                            crossfade(true)
                        }).build()
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painter,
                    contentDescription = "Foto Profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .background(Color.LightGray)
                )
                if (isUploading) {
                    CircularProgressIndicator(color = Color(0xFF2F80ED), modifier = Modifier.size(40.dp))
                }
            }

            if (!isUploading) {
                Surface(
                    onClick = { launcher.launch("image/*") },
                    shape = CircleShape,
                    color = Color(0xFF2F80ED),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Foto",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nama,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = jabatan,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (isEditMode) {
                        UserSession.username = nama
                        FirebaseInstance.db.collection("users").document(uid)
                            .update("name", nama)
                        Toast.makeText(context, "Profil berhasil disimpan", Toast.LENGTH_SHORT).show()
                    }
                    isEditMode = !isEditMode
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text(text = if (isEditMode) "Simpan Profil" else "Edit Profil", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ProfileCard(title = "Informasi Utama") {
            ProfileField("Nama Lengkap", nama, isEditMode) { nama = it }
            ProfileField("NIDN", nidn, isEditMode) { nidn = it }
            ProfileField("Email", email, isEditMode) { email = it }
            ProfileField("No. HP", noHp, isEditMode) { noHp = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ProfileCard(title = "Informasi Akademik") {
            ProfileField("Program Studi", prodi, isEditMode) { prodi = it }
            ProfileField("Jabatan", jabatan, isEditMode) { jabatan = it }
            ProfileField("Bidang Keahlian", bidangKeahlian, isEditMode) { bidangKeahlian = it }
        }

        Spacer(modifier = Modifier.height(30.dp))

        val uriHandler = LocalUriHandler.current
        Text(
            text = "Kebijakan Privasi",
            color = Color(0xFF2F80ED),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clickable { uriHandler.openUri("https://www.privacypolicies.com/live/3d189c23-9de0-4ae5-8b34-b0f66c50386a") }
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                UserSession.logout(context)
                navController.navigate("logout") {
                    popUpTo("profiledosen") { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Keluar Sesi", color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun ProfileCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2F80ED))
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, isEditMode: Boolean, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        if (isEditMode) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        } else {
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray, thickness = 0.5.dp)
        }
    }
}