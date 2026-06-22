package com.myschedule.id

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

@Composable
fun HomeScreen(navController: NavHostController) {

    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    val primaryColor = Color(0xFF2F80ED)
    val headerColor = Color.Black
    val placeholderColor = Color.Gray
    val activityTitleColor = Color.Black
    val activitySubtitleColor = Color.Gray

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(50.dp))

        // ==========================
        // TOP NAV
        // ==========================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, primaryColor, CircleShape)
                    .clickable {
                        navController.navigate("profilescreen")
                    }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "My Schedule",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = headerColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================
        // HEADER
        // ==========================
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

            Image(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = "Notifikasi",
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
                    .clickable {
                        navController.navigate("notifikasi")
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================
        // SEARCH BAR
        // ==========================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Cari tugas atau jadwal",
                    color = placeholderColor
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            textStyle = LocalTextStyle.current.copy(color = headerColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================
        // MENU
        // ==========================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // TUGAS
            MenuItem(
                icon = R.drawable.ic_tugas,
                label = "Tugas",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("pengumpulantugas")
            }

            // NILAI
            MenuItem(
                icon = R.drawable.ic_nilai,
                label = "Nilai",
                modifier = Modifier.weight(1f)
            ) {
                navController.navigate("infonilaiscreen")
            }

            // KOSTSPACEEE
            MenuItem(
                icon = R.drawable.ic_home,
                label = "KostSpaceee",
                modifier = Modifier.weight(1f)
            ) {

                val intent = context.packageManager
                    .getLaunchIntentForPackage("com.company.kostspaceee")

                if (intent != null) {

                    context.startActivity(intent)

                } else {

                    Toast.makeText(
                        context,
                        "APK KostSpaceee belum terinstall",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================
        // TITLE
        // ==========================
        Text(
            text = "Aktivitas Terkini",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = headerColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================
        // LIST AKTIVITAS
        // ==========================
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxHeight()
        ) {

            items(sampleActivities) { item ->

                val isSelected =
                    searchQuery.isNotBlank() &&
                            item.title.contains(
                                searchQuery,
                                ignoreCase = true
                            )

                ActivityItem(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    highlighted = isSelected,
                    titleColor = activityTitleColor,
                    subtitleColor = activitySubtitleColor
                )
            }
        }
    }
}

@Composable
fun MenuItem(
    icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
            .background(
                Color(0xFF2F80ED),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

@Composable
fun ActivityItem(
    icon: Int,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    titleColor: Color = Color.Black,
    subtitleColor: Color = Color.Gray
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(
                if (highlighted)
                    Color(0xFFD0E8FF)
                else
                    Color.White,

                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(id = icon),
            contentDescription = title,
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subtitleColor
            )
        }
    }
}

data class Activity(
    val icon: Int,
    val title: String,
    val subtitle: String
)

val sampleActivities = listOf(

    Activity(
        R.drawable.ic_mtk,
        "Matematika Diskrit",
        "Tugas diunggah - 2 jam lalu"
    ),

    Activity(
        R.drawable.ic_prog,
        "Pemrograman Android",
        "Tugas dikumpulkan - 1 hari lalu"
    ),

    Activity(
        R.drawable.ic_jarkom,
        "Jaringan Komputer",
        "Materi baru tersedia - 3 jam lalu"
    ),

    Activity(
        R.drawable.ic_basisdata,
        "Basis Data",
        "Tugas dikoreksi - 5 jam lalu"
    ),

    Activity(
        R.drawable.ic_so,
        "Sistem Operasi",
        "Materi baru tersedia - 1 hari lalu"
    ),

    Activity(
        R.drawable.ic_imk,
        "Interaksi Manusia Komputer",
        "Tugas dikumpulkan - 2 hari lalu"
    ),

    Activity(
        R.drawable.ic_web,
        "Pengembangan Web",
        "Tugas dikumpulkan - 3 hari lalu"
    ),

    Activity(
        R.drawable.ic_ai,
        "Kecerdasan Buatan",
        "Materi baru tersedia - 2 hari lalu"
    ),

    Activity(
        R.drawable.ic_cloud,
        "Cloud Computing",
        "Tugas dikumpulkan - 3 minggu lalu"
    )
)