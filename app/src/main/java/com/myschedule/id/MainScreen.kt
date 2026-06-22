package com.myschedule.id

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myschedule.id.ui.StudentHomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {

    val bottomNavController = rememberNavController()

    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bluePrimary = Color(0xFF1565C0)

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("My Schedule")
                },

                navigationIcon = {

                    if (currentRoute != "student_home") {

                        IconButton(
                            onClick = {
                                bottomNavController.popBackStack()
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },

        bottomBar = {

            if (
                currentRoute == "student_home" ||
                currentRoute == "tugas" ||
                currentRoute == "absensi"
            ) {

                BottomBar(bottomNavController)
            }
        }

    ) { paddingValues ->

        NavHost(
            navController = bottomNavController,
            startDestination = "student_home",
            modifier = Modifier.padding(paddingValues)
        ) {

            composable("student_home") {

                StudentHomeScreen(
                    navController = bottomNavController
                )
            }

            composable("tugas") {

                Surface(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Screen Tugas",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            composable("absensi") {

                Surface(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Screen Absensi",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {

    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bluePrimary = Color(0xFF1565C0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),

        horizontalArrangement = Arrangement.SpaceEvenly
    ) {

        // HOME
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.clickable {

                navController.navigate("student_home") {

                    popUpTo("student_home") {
                        saveState = true
                    }

                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "Home",

                tint =
                    if (currentRoute == "student_home")
                        bluePrimary
                    else
                        Color.Gray
            )

            Text(
                text = "Home",

                color =
                    if (currentRoute == "student_home")
                        bluePrimary
                    else
                        Color.Gray
            )
        }

        // TUGAS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.clickable {

                navController.navigate("tugas") {

                    popUpTo("student_home") {
                        saveState = true
                    }

                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_tugas),
                contentDescription = "Tugas",

                tint =
                    if (currentRoute == "tugas")
                        bluePrimary
                    else
                        Color.Gray
            )

            Text(
                text = "Tugas",

                color =
                    if (currentRoute == "tugas")
                        bluePrimary
                    else
                        Color.Gray
            )
        }

        // JADWAL
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.clickable {

                navController.navigate("absensi") {

                    popUpTo("student_home") {
                        saveState = true
                    }

                    launchSingleTop = true
                    restoreState = true
                }
            }
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_absensi),
                contentDescription = "Jadwal",

                tint =
                    if (currentRoute == "absensi")
                        bluePrimary
                    else
                        Color.Gray
            )

            Text(
                text = "Jadwal",

                color =
                    if (currentRoute == "absensi")
                        bluePrimary
                    else
                        Color.Gray
            )
        }

        // KOST
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,

            modifier = Modifier.clickable {

                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(
                        "com.company.kostspaceee"
                    )

                launchIntent?.let {
                    context.startActivity(it)
                }
            }
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "Kost",

                tint = Color.Gray
            )

            Text(
                text = "Kost",
                color = Color.Gray
            )
        }
    }
}