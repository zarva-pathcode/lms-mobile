package com.myschedule.id

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {

    var progress by remember { mutableStateOf(0f) }
    var percent by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {

        val steps = 100
        val delayTime = 3000L / steps

        for (i in 1..steps) {
            delay(delayTime)
            progress = i / steps.toFloat()
            percent = i
        }

        // 🔥 CEK FIREBASE LOGIN
        val user = FirebaseInstance.auth.currentUser

        if (user != null) {

            // ambil role dari Firestore
            FirebaseInstance.db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { doc ->

                    val role = doc.getString("role") ?: "student"

                    if (role == "teacher") {
                        navController.navigate("teacher_home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("student_home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }

        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    val animatedProgress by animateFloatAsState(targetValue = progress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "organize your academic life",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            LinearProgressIndicator(
                progress = animatedProgress,
                color = Color.Black,
                trackColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Processing $percent%",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}