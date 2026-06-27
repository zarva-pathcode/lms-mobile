package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassDashboardScreen(navController: NavHostController, uniName: String, className: String) {
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    val menuItems = listOf(
        DashboardMenu("Jadwal & Absensi", Icons.Default.CalendarMonth, "teacher_manage_absensi/$uniName/$className"),
        DashboardMenu("Buat Quiz", Icons.Default.Quiz, "teacher_manage_quiz/$uniName/$className"),
        DashboardMenu("Kelola Tugas", Icons.Default.Assignment, "teacher_manage_tasks/$uniName/$className"),
        DashboardMenu("Upload Materi", Icons.Default.LibraryBooks, "teacher_manage_materials/$uniName/$className"),
        DashboardMenu("Data Mahasiswa", Icons.Default.People, "teacher_class_students/$uniName/$className"),
        DashboardMenu("Informasi Nilai", Icons.Default.Assessment, "teacher_class_grades/$uniName/$className")
    )

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
                        Text(className, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(uniName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)
        ) {
            Text("Menu Pengelolaan Kelas", fontWeight = FontWeight.Bold, color = Color(0xFF1E1B4B))
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(menuItems) { item ->
                    MenuCardInternal(item, bluePrimary) {
                        navController.navigate(item.route)
                    }
                }
            }
        }
    }
}

@Composable
fun MenuCardInternal(menu: DashboardMenu, primaryColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(menu.icon, null, tint = primaryColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(menu.title, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

data class DashboardMenu(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)
