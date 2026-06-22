package com.myschedule.id.ui

import com.myschedule.id.data.AuthRepository
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.R

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.ui.autofill.*
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(navController: NavHostController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

    val autofillTree = LocalAutofillTree.current

    val emailNode = AutofillNode(
        autofillTypes = listOf(AutofillType.EmailAddress),
        onFill = { email = it }
    )

    val passwordNode = AutofillNode(
        autofillTypes = listOf(AutofillType.Password),
        onFill = { password = it }
    )

    LaunchedEffect(Unit) {
        autofillTree += emailNode
        autofillTree += passwordNode
    }

    // ==========================
    // AUTO LOGIN (FIX ROLE)
    // ==========================
    LaunchedEffect(Unit) {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val lastEmail = prefs.getString("last_email", null)
        val lastPassword = prefs.getString("last_password", null)

        if (isLoggedIn && lastEmail != null && lastPassword != null) {
            val user = FirebaseInstance.auth.currentUser

            if (user != null) {
                FirebaseInstance.db.collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val role = document.getString("role") ?: "student"

                        if (role == "teacher") {
                            navController.navigate("teacher_home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("student_home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
            }
        }
    }

    val loginTextColor = Color(0xFF2F80ED)
    val textColor = Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFBBDEFB), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login", fontSize = 28.sp, color = textColor)
            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = textColor) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val pos = it.positionInWindow()
                        emailNode.boundingBox = Rect(
                            pos.x,
                            pos.y,
                            pos.x + it.size.width,
                            pos.y + it.size.height
                        )
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                textStyle = LocalTextStyle.current.copy(color = textColor)
            )

            Spacer(modifier = Modifier.height(15.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = textColor) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val pos = it.positionInWindow()
                        passwordNode.boundingBox = Rect(
                            pos.x,
                            pos.y,
                            pos.x + it.size.width,
                            pos.y + it.size.height
                        )
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                textStyle = LocalTextStyle.current.copy(color = textColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Lupa Password?",
                color = loginTextColor,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { navController.navigate("reset") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    AuthRepository.login(
                        email,
                        password,
                        onSuccess = { role: String, kelas: String, universitas: String ->
                            if (role == "teacher") {
                                navController.navigate("teacher_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate("student_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onError = { errorMessage = it }
                    )
                }
            ) {
                Text("Login")
            }


            Spacer(modifier = Modifier.height(20.dp))

            Row {
                Text("Belum punya akun? ", color = textColor)
                Text(
                    text = "Sign Up",
                    color = loginTextColor,
                    modifier = Modifier.clickable { navController.navigate("signup") }
                )
            }
        }
    }
}