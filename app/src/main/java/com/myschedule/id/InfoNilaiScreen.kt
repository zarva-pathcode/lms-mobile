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
fun InfoNilaiScreen(navController: NavHostController) {

    val semesters = (1..7).map { "Semester $it" }

    val mataKuliahPerSemester = mapOf(
        "Semester 1" to listOf("Matematika Dasar", "Algoritma & Pemrograman", "Dasar Jaringan", "Logika Informatika"),
        "Semester 2" to listOf("Struktur Data", "Basis Data", "Sistem Operasi", "Pemrograman Lanjut"),
        "Semester 3" to listOf("Rekayasa Perangkat Lunak", "Jaringan Komputer", "Statistik", "Pemrograman Web"),
        "Semester 4" to listOf("Analisis Sistem", "Pemrograman Mobile", "Keamanan Komputer", "AI"),
        "Semester 5" to listOf("Cloud Computing", "Manajemen Proyek TI", "Sistem Informasi", "IoT"),
        "Semester 6" to listOf("Big Data", "IMK", "Grafika Komputer", "Game Programming"),
        "Semester 7" to listOf("Magang", "Skripsi")
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedSemester by remember { mutableStateOf(semesters[0]) }
    var nilaiList by remember { mutableStateOf(generateNilai(mataKuliahPerSemester[selectedSemester] ?: emptyList())) }

    val backgroundColor = Color(0xFFE3F2FD) // biru muda
    val headerColor = Color(0xFFBBDEFB) // biru header
    val dropdownTextColor = Color(0xFF2F80ED) // teks dropdown biru
    val listTextColor = Color.Black
    val judulTextColor = Color(0xFF2F80ED) // biru soft

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(60.dp)) // header diturunkan agar sejajar MaterialsScreen

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
                    text = "Info Nilai Tugas",
                    fontSize = 22.sp,
                    color = judulTextColor
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
                            nilaiList = generateNilai(mataKuliahPerSemester[selectedSemester] ?: emptyList())
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Nilai Tugas $selectedSemester",
            fontSize = 20.sp,
            color = listTextColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(nilaiList) { nilai ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = nilai,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(14.dp),
                        color = listTextColor
                    )
                }
            }
        }
    }
}

fun generateNilai(matakuliah: List<String>): List<String> {
    val grades = listOf("A", "A-", "B+", "B", "B-", "C+", "C")
    return matakuliah.map { "$it : ${grades.random()}" }
}