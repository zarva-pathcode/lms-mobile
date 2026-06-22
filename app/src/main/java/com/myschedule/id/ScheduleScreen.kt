package com.myschedule.id

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

data class ScheduleItem(
    val title: String,
    val color: Color
)

data class DaySchedule(
    val day: String,
    val date: String,
    val classes: List<ScheduleItem>
)

val scheduleData = listOf(
    DaySchedule("Min", "23", listOf()),
    DaySchedule("Sen", "24", listOf(
        ScheduleItem("Matematika Diskrit", Color(0xFF2F80ED)),
        ScheduleItem("Struktur Data", Color.Gray),
        ScheduleItem("Bahasa Inggris II", Color(0xFF27AE60))
    )),
    DaySchedule("Sel", "25", listOf(
        ScheduleItem("Algoritma & Pemrograman", Color(0xFF2F80ED)),
        ScheduleItem("Basis Data I", Color.Gray)
    )),
    DaySchedule("Rab", "26", listOf(
        ScheduleItem("Sistem Operasi", Color(0xFF2F80ED)),
        ScheduleItem("Jaringan Komputer", Color(0xFF27AE60))
    )),
    DaySchedule("Kam", "27", listOf())
)

@Composable
fun ScheduleScreen(navController: NavHostController) {

    var selectedTab by remember { mutableStateOf(0) }
    var selectedDateIndex by remember { mutableStateOf(1) }

    val selectedSchedule = scheduleData[selectedDateIndex]
    val titleColor = Color(0xFF2F80ED) // judul biru selaras

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)) // biru muda selaras
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        // ==========================
        // TOP NAV (Profil & Title)
        // ==========================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PROFIL POJOK KIRI
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
                    .border(2.dp, titleColor, CircleShape)
                    .clickable {
                        navController.navigate("profilescreen")
                    }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Jadwal Saya",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TAB
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFBBDEFB), // selaras header
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF2F80ED)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Hari Ini", color = titleColor) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Mingguan", color = titleColor) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        DateSelector(selectedIndex = selectedDateIndex) { selectedDateIndex = it }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "RINGKASAN MINGGUAN",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            item { ScheduleCard(selectedSchedule) }
        }
    }
}

@Composable
fun DateSelector(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

        itemsIndexed(scheduleData) { index, item ->

            val selected = index == selectedIndex

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Color(0xFF2F80ED) else Color(0xFFF3F4F6))
                    .clickable { onSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                val contentColor = if (selected) Color.White else Color.Gray

                Text(
                    text = item.day,
                    fontSize = 12.sp,
                    color = contentColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (selected) Color.White else Color.Black
                )
            }
        }
    }
}

@Composable
fun ScheduleCard(daySchedule: DaySchedule) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "${daySchedule.day}, ${daySchedule.date} Jun",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                if (daySchedule.classes.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE9EEF6))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${daySchedule.classes.size} Kelas",
                            fontSize = 12.sp,
                            color = Color(0xFF2F80ED)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            daySchedule.classes.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(item.color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (daySchedule.classes.isEmpty()) {
                Text(
                    text = "Tidak ada kelas",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}
