package com.myschedule.id

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

// DATA CLASS
data class Notifikasi(
    val title: String,
    val subtitle: String
)

// SAMPLE DATA
val sampleNotifikasi = listOf(
    Notifikasi("Tugas Matematika Diskrit", "Tugas dikumpulkan 2 jam lalu"),
    Notifikasi("Pengumuman Nilai", "Nilai Pemrograman Android telah tersedia"),
    Notifikasi("Materi Baru", "Materi Jaringan Komputer telah diunggah"),
    Notifikasi("Tugas Basis Data", "Tugas dikoreksi oleh dosen"),
    Notifikasi("Pengingat", "Pengumpulan tugas Sistem Operasi besok")
)

@SuppressLint("ComposableNaming")
@Composable
fun notifikasi(navController: NavHostController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)) // background biru muda
            .padding(16.dp)
    ) {

        Spacer(modifier = Modifier.height(20.dp)) // sedikit lebih turun

        // HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back",
                modifier = Modifier
                    .size(26.dp)
                    .clickable {
                        // Navigasi kembali ke screen sebelumnya
                        navController.popBackStack()
                    }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Notifikasi",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333) // abu gelap selaras tema
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIST NOTIFIKASI
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            items(sampleNotifikasi) { notif ->

                NotifikasiItem(
                    title = notif.title,
                    subtitle = notif.subtitle
                )

            }
        }
    }
}

@Composable
fun NotifikasiItem(
    title: String,
    subtitle: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp)) // card putih kontras
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            painter = painterResource(id = R.drawable.ic_notification),
            contentDescription = "Icon Notifikasi",
            tint = Color(0xFF2F80ED), // biru utama selaras tema
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333) // abu gelap
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF777777) // abu muda
            )
        }
    }
}