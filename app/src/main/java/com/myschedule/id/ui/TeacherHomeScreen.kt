package com.myschedule.id.ui

import com.myschedule.id.R

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(navController: NavHostController) {
    var managedCampus by remember { mutableStateOf(listOf<Map<String, String>>()) }
    var dosenName by remember { mutableStateOf("") }
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    // Tema Biru Baru untuk Top Bar dan Komponen
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    LaunchedEffect(Unit) {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val managed = doc.get("managed_campus") as? List<Map<String, String>> ?: listOf()
                managedCampus = managed
                
                UserSession.profileImageUri = doc.getString("profileImageUri")
                dosenName = doc.getString("name") ?: "Dosen"
            }
    }

    val universities = managedCampus.map { it["uni"] }.distinct()

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
                    val painter = if (UserSession.profileImageUri.isNullOrEmpty()) {
                        painterResource(R.drawable.profile)
                    } else {
                        rememberAsyncImagePainter(UserSession.profileImageUri)
                    }

                    Image(
                        painter = painter,
                        contentDescription = "Foto Profil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                navController.navigate("profiledosen")
                            }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dashboard Dosen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(dosenName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

                    IconButton(onClick = { navController.navigate("logout") }) {
                        Icon(Icons.Default.Logout, null, tint = Color.White)
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("teacher_manage_campus") }, 
                containerColor = bluePrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.AddBusiness, null, tint = Color.White)
            }
        }
    ) { paddingValues ->
        if (universities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum ada universitas yang diampu.", color = Color.Gray)
                    Button(
                        onClick = { navController.navigate("teacher_manage_campus") }, 
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                    ) {
                        Text("Kelola Kampus Sekarang", color = Color.White)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(universities) { uni ->
                    UniversityCard(uni ?: "", bluePrimary, onClick = {
                        navController.navigate("teacher_manage_uni/${uni}")
                    })
                }
            }
        }
    }
}

@Composable
fun UniversityCard(name: String, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Business, null, tint = primaryColor, modifier = Modifier.size(30.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
        }
    }
}