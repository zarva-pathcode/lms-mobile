package com.myschedule.id

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Tema Biru Baru
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    
    var name by remember { mutableStateOf(UserSession.username) }
    var isEditingName by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(UserSession.isDarkMode) }
    var isNotifOn by remember { mutableStateOf(UserSession.isNotifOn) }
    var profileUri by remember { mutableStateOf<String?>(UserSession.profileImageUri) }
    var isUploading by remember { mutableStateOf(false) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: "default_user"

    LaunchedEffect(Unit) {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                name = doc.getString("name") ?: ""
                UserSession.studentId = doc.getString("nim") ?: ""
            }
    }

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
                        
                        Log.d("SupabaseUpload", "Memulai upload: $fileName")
                        
                        bucket.upload(fileName, bytes) {
                            upsert = true
                        }
                        
                        Log.d("SupabaseUpload", "Upload berhasil ke Storage")
                        
                        val timestamp = System.currentTimeMillis()
                        val publicUrl = "${bucket.publicUrl(fileName)}?t=$timestamp"
                        
                        Log.d("SupabaseUpload", "URL Baru: $publicUrl")
                        
                        FirebaseInstance.db.collection("users").document(uid)
                            .update("profileImageUri", publicUrl)
                            .addOnSuccessListener {
                                UserSession.profileImageUri = publicUrl
                                profileUri = publicUrl
                                UserSession.updateProfileImage(context, publicUrl)
                                
                                context.imageLoader.diskCache?.remove(publicUrl)
                                context.imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(publicUrl))

                                Toast.makeText(context, "Foto profil diperbarui", Toast.LENGTH_SHORT).show()
                                Log.d("SupabaseUpload", "Firestore berhasil diperbarui")
                            }
                            .addOnFailureListener { e ->
                                Log.e("SupabaseUpload", "Gagal update Firestore: ${e.message}")
                                Toast.makeText(context, "Gagal sinkron data", Toast.LENGTH_SHORT).show()
                            }
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseUpload", "Error Total: ${e.message}", e)
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
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
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Text(
                        text = "Profil Saya",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F7FF))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- AVATAR SECTION ---
            Box(contentAlignment = Alignment.BottomEnd) {
                val painter = if (profileUri.isNullOrEmpty()) {
                    painterResource(R.drawable.profile)
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
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                            .background(Color.LightGray)
                    )
                    if (isUploading) {
                        CircularProgressIndicator(color = bluePrimary, modifier = Modifier.size(40.dp))
                    }
                }

                if (!isUploading) {
                    Surface(
                        onClick = { launcher.launch("image/*") },
                        shape = CircleShape,
                        color = bluePrimary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Ganti Foto",
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // --- NAMA EDIT SECTION ---
            if (isEditingName) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Nama Lengkap") }
                    )
                    IconButton(onClick = {
                        if (name.isNotBlank()) {
                            UserSession.username = name
                            FirebaseInstance.db.collection("users").document(uid)
                                .update("name", name)
                                .addOnSuccessListener {
                                    isEditingName = false
                                    Toast.makeText(context, "Nama berhasil disimpan", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }) {
                        Icon(Icons.Default.Check, null, tint = bluePrimary)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { isEditingName = true }) {
                        Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Text("Mahasiswa / Student", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // --- INFO & SETTINGS ---
            ProfileSectionTitleInternal(title = "Informasi Akademik", primaryColor = bluePrimary)
            ProfileInfoCardInternal(Icons.Default.School, "Universitas", UserSession.universitas)
            ProfileInfoCardInternal(Icons.Default.Badge, "Student ID / NIM", UserSession.studentId.ifEmpty { "-" })
            ProfileInfoCardInternal(Icons.Default.Email, "Email", UserSession.email)

            Spacer(modifier = Modifier.height(16.dp))

//            ProfileSectionTitleInternal(title = "Pengaturan Aplikasi", primaryColor = bluePrimary)
//            SettingsToggleCardInternal(Icons.Default.Notifications, "Notifikasi", isNotifOn, bluePrimary) {
//                isNotifOn = it
//                UserSession.isNotifOn = it
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))

            val uriHandler = LocalUriHandler.current
            Text(
                text = "Kebijakan Privasi",
                color = bluePrimary,
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
                        popUpTo("splash") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar Sesi", fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileSectionTitleInternal(title: String, primaryColor: Color) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = primaryColor
    )
}

@Composable
fun ProfileInfoCardInternal(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Color.Gray)
                Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SettingsToggleCardInternal(icon: ImageVector, label: String, state: Boolean, primaryColor: Color, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Switch(checked = state, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedThumbColor = primaryColor))
        }
    }
}
