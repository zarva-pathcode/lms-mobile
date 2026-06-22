package com.myschedule.id.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.UserSession
import com.myschedule.id.data.ScheduleRepository
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(navController: NavHostController) {

    var availableClassesAtUni by remember { mutableStateOf(listOf<String>()) }
    var enrolledClasses by remember { mutableStateOf(listOf<String>()) }
    var universitas by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }

    var selectedClassForDetail by remember { mutableStateOf<String?>(null) }
    var classBlueprints by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""

    fun refreshData() {
        FirebaseInstance.db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->

                universitas = doc.getString("universitas") ?: ""

                @Suppress("UNCHECKED_CAST")
                val classes =
                    doc.get("enrolled_classes") as? List<String> ?: listOf()

                enrolledClasses = classes

                UserSession.username = doc.getString("name") ?: ""
                UserSession.profileImageUri =
                    doc.getString("profileImageUri")
                UserSession.universitas = universitas
                UserSession.studentId =
                    doc.getString("studentId") ?: ""

                if (universitas.isNotEmpty() &&
                    enrolledClasses.isNotEmpty()
                ) {

                    ScheduleRepository.checkAndGenerateDailySessions(
                        universitas,
                        enrolledClasses
                    )
                }

                if (universitas.isNotEmpty()) {

                    FirebaseInstance.db.collection("universities")
                        .document(universitas)
                        .get()
                        .addOnSuccessListener { uniDoc ->

                            if (uniDoc.exists()) {

                                @Suppress("UNCHECKED_CAST")
                                val allClassNames =
                                    uniDoc.get("classes")
                                            as? List<String>
                                        ?: listOf()

                                availableClassesAtUni =
                                    allClassNames.filter {
                                        !enrolledClasses.contains(it)
                                    }
                            }
                        }
                }
            }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)
    val bgLight = Color(0xFFF8F7FF)

    val daysMap = mapOf(
        Calendar.MONDAY to "Senin",
        Calendar.TUESDAY to "Selasa",
        Calendar.WEDNESDAY to "Rabu",
        Calendar.THURSDAY to "Kamis",
        Calendar.FRIDAY to "Jumat",
        Calendar.SATURDAY to "Sabtu",
        Calendar.SUNDAY to "Minggu"
    )

    Scaffold(
        topBar = {
            MainTopBar(navController, bluePrimary, blueLight)
        },

        // =========================
        // BOTTOM NAVBAR
        // =========================
        bottomBar = {
            MainBottomBar(navController)
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgLight)
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            val isShowingAvailable =
                enrolledClasses.isEmpty()

            TabRow(
                selectedTabIndex = 0,
                containerColor = Color.White,
                contentColor = bluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(
                            tabPositions[0]
                        ),
                        color = bluePrimary
                    )
                }
            ) {

                Tab(
                    selected = true,
                    onClick = { },
                    text = {

                        Text(
                            if (isShowingAvailable)
                                "Kelas Tersedia"
                            else
                                "Kelas Saya",

                            fontWeight = FontWeight.Bold,
                            color = bluePrimary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (message.isNotEmpty()) {

                Text(
                    message,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    message = ""
                }
            }

            val currentList =
                if (isShowingAvailable)
                    availableClassesAtUni
                else
                    enrolledClasses

            if (currentList.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        if (isShowingAvailable)
                            "Tidak ada kelas baru di $universitas."
                        else
                            "Anda belum mengambil kelas apapun.",

                        color = Color.Gray
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(currentList) { className ->

                        ClassItemCard(
                            className = className,
                            isEnrolled = !isShowingAvailable,
                            primaryColor = bluePrimary,

                            onAction = {

                                if (isShowingAvailable) {

                                    selectedClassForDetail =
                                        className

                                    ScheduleRepository
                                        .getSchedulesForStudent(
                                            className,
                                            universitas
                                        ) {
                                            classBlueprints = it
                                        }

                                } else {

                                    navController.navigate(
                                        "student_class_dashboard/$universitas/$className"
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (selectedClassForDetail != null) {

            AlertDialog(

                onDismissRequest = {
                    selectedClassForDetail = null
                },

                title = {

                    Text(
                        "Detail Kelas: $selectedClassForDetail",
                        fontWeight = FontWeight.Bold,
                        color = bluePrimary
                    )
                },

                text = {

                    Column(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {

                        Text(
                            "Aturan Jadwal (Blueprint):",
                            fontWeight = FontWeight.SemiBold,
                            color = bluePrimary
                        )

                        if (classBlueprints.isEmpty()) {

                            Text(
                                "Belum ada aturan jadwal.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                        } else {

                            classBlueprints.forEach { bp ->

                                val dayIdx =
                                    (bp["dayOfWeek"] as? Long)
                                        ?.toInt() ?: 1

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),

                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(
                                            0xFFF3F4F6
                                        )
                                    )
                                ) {

                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {

                                        Text(
                                            bp["title"].toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Text(
                                            "Setiap ${daysMap[dayIdx]} jam ${bp["time"]}",
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },

                confirmButton = {

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bluePrimary
                        ),

                        onClick = {

                            val className =
                                selectedClassForDetail ?: return@Button

                            FirebaseInstance.db.collection("users")
                                .document(uid)
                                .update(
                                    "enrolled_classes",
                                    com.google.firebase.firestore.FieldValue
                                        .arrayUnion(className)
                                )
                                .addOnSuccessListener {

                                    message =
                                        "Berhasil mengambil kelas $className"

                                    enrolledClasses =
                                        listOf(className)

                                    refreshData()

                                    selectedClassForDetail = null
                                }
                        }
                    ) {

                        Text(
                            "Ambil Kelas",
                            color = Color.White
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {
                            selectedClassForDetail = null
                        }
                    ) {

                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun ClassItemCard(
    className: String,
    isEnrolled: Boolean,
    primaryColor: Color,
    onAction: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction() },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(0.1f)),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Default.Class,
                    null,
                    tint = primaryColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                className,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            if (!isEnrolled) {

                Icon(
                    Icons.Default.AddCircleOutline,
                    null,
                    tint = primaryColor
                )

            } else {

                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun MainTopBar(
    navController: NavHostController,
    bluePrimary: Color,
    blueLight: Color
) {

    val profileImage = UserSession.profileImageUri

    Column {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(bluePrimary, blueLight)
                    )
                )
                .statusBarsPadding()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        navController.navigate("student_profile")
                    },

                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f))
                ) {

                    if (profileImage != null) {

                        AsyncImage(
                            model = profileImage,
                            contentDescription = "Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                    } else {

                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(
                        "Dashboard Mahasiswa",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        UserSession.username.ifEmpty {
                            "Selamat Datang"
                        },

                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {
                        navController.navigate("logout")
                    }
                ) {

                    Icon(
                        Icons.Default.Logout,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainBottomBar(navController: NavHostController) {

    val context = LocalContext.current

    val items = listOf(

        BottomItem(
            "student_home",
            Icons.Default.Home,
            "Home"
        ),

        BottomItem(
            "student_tasks_overview",
            Icons.Default.Assignment,
            "Tugas"
        ),

        BottomItem(
            "student_schedule_overview",
            Icons.Default.CalendarToday,
            "Jadwal"
        ),

        // =====================
        // MENU KOSTSPACEE
        // =====================
        BottomItem(
            "kostspacee",
            Icons.Default.HomeWork,
            "Kost"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry?.destination?.route

    val bluePrimary = Color(0xFF1565C0)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {

        items.forEach { item ->

            NavigationBarItem(

                icon = {
                    Icon(item.icon, null)
                },

                label = {
                    Text(
                        item.title,
                        fontSize = 10.sp
                    )
                },

                selected = currentRoute == item.route,

                onClick = {

                    // =========================
                    // KHUSUS MENU KOST
                    // =========================
                    if (item.route == "kostspacee") {

                        val launchIntent =
                            context.packageManager
                                .getLaunchIntentForPackage(
                                    "com.company.kostspaceee"
                                )

                        if (launchIntent != null) {

                            context.startActivity(
                                launchIntent
                            )

                        } else {

                            android.widget.Toast
                                .makeText(
                                    context,
                                    "APK KostSpacee belum terinstall",
                                    android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                        }

                    } else {

                        if (currentRoute != item.route) {

                            navController.navigate(item.route) {

                                popUpTo("student_home") {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = bluePrimary,
                    selectedTextColor = bluePrimary,
                    indicatorColor =
                        bluePrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

data class BottomItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)