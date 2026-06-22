package com.myschedule.id

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeDosenScreen(navController: NavHostController) {

    var searchQuery by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF2F80ED)
    val headerColor = Color.Black
    val placeholderColor = Color.Gray

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(50.dp))

        // ==========================
        // TOP NAV / HEADER
        // ==========================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FOTO PROFIL (KLIK KE PROFIL)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, primaryColor, CircleShape)
                    .clickable {
                        navController.navigate("profiledosen")
                    }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Dashboard Dosen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor,
                modifier = Modifier.weight(1f)
            )

            // MENU TITIK 3 (KANAN)
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile") },
                        onClick = {
                            expanded = false
                            navController.navigate("profiledosen")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            expanded = false
                            UserSession.logout(context)
                            navController.navigate("logout") {
                                popUpTo("homedosenscreen") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // LOGO BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFFBBDEFB), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SEARCH
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari kelas / tugas", color = placeholderColor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            textStyle = LocalTextStyle.current.copy(color = headerColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // MENU DOSEN
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuItem(
                icon = R.drawable.ic_tugas,
                label = "Buat Tugas",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("buat_tugas")
            }
            MenuItem(
                icon = R.drawable.ic_materials,
                label = "Kelola Kelas",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("kelola_kelas")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuItem(
                icon = R.drawable.ic_nilai,
                label = "Input Nilai",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("input_nilai")
            }
            MenuItem(
                icon = R.drawable.ic_material,
                label = "Upload Materi",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("upload_materi")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // EMPTY STATE
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Belum ada aktivitas",
                color = Color.Gray
            )
        }
    }
}
