package com.myschedule.id

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengumpulanTugasScreen(navController: NavHostController) {

    val semesters = (1..7).map { "Semester $it" }

    val semuaTugasPerSemester = mapOf(
        "Semester 1" to listOf("Tugas Matematika", "Tugas Algoritma", "Tugas Logika"),
        "Semester 2" to listOf("Tugas Struktur Data", "Tugas Basis Data", "Tugas Sistem Operasi"),
        "Semester 3" to listOf("Tugas RPL", "Tugas Jaringan", "Tugas Statistik"),
        "Semester 4" to listOf("Tugas Analisis Sistem", "Tugas Pemrograman Mobile", "Tugas Keamanan"),
        "Semester 5" to listOf("Tugas Cloud", "Tugas Manajemen Proyek TI", "Tugas Sistem Informasi"),
        "Semester 6" to listOf("Tugas Big Data", "Tugas Interaksi Manusia", "Tugas Grafika Komputer"),
        "Semester 7" to listOf("Tugas Magang", "Tugas Skripsi")
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedSemester by remember { mutableStateOf(semesters[0]) }
    var selectedTab by remember { mutableStateOf(0) }

    var tugasBelumSelesai by remember {
        mutableStateOf(generateTugas(semuaTugasPerSemester[selectedSemester] ?: emptyList()))
    }

    var tugasSelesai by remember {
        mutableStateOf(generateTugas(semuaTugasPerSemester[selectedSemester] ?: emptyList()))
    }

    // WARNA TEMA
    val backgroundColor = Color(0xFFE3F2FD) // biru muda latar
    val headerColor = Color(0xFFBBDEFB) // header biru lembut
    val primaryText = Color.Black
    val dropdownTextColor = Color(0xFF2F80ED) // teks dropdown biru
    val judulTextColor = Color(0xFF2F80ED) // biru soft selaras InfoNilaiScreen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(60.dp)) // turunkan header agar sejajar

        // HEADER DENGAN ICON BACK
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // ICON BACK
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = judulTextColor,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            navController.navigateUp() // kembali ke riwayat sebelumnya
                        }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // JUDUL
                Text(
                    text = "Pengumpulan Tugas",
                    fontSize = 22.sp,
                    color = judulTextColor // biru soft
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DROPDOWN SEMESTER
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedSemester,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih Semester") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f),
                        tint = dropdownTextColor
                    )
                },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                semesters.forEach { semester ->
                    DropdownMenuItem(
                        text = { Text(semester, color = dropdownTextColor) },
                        onClick = {
                            selectedSemester = semester
                            tugasBelumSelesai = generateTugas(
                                semuaTugasPerSemester[selectedSemester] ?: emptyList()
                            )
                            tugasSelesai = generateTugas(
                                semuaTugasPerSemester[selectedSemester] ?: emptyList()
                            )
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TAB
        val tabTitles = listOf("Belum Selesai", "Selesai")

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = headerColor,
            contentColor = primaryText
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, color = primaryText) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LIST TUGAS
        val tugasToShow = if (selectedTab == 0) tugasBelumSelesai else tugasSelesai

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tugasToShow) { tugas ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = tugas,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(14.dp),
                        color = primaryText
                    )
                }
            }
        }
    }
}

fun generateTugas(listTugas: List<String>): List<String> {
    val status = listOf("Belum Selesai", "Selesai")
    return listTugas.map { "$it - ${status.random()}" }
}