package com.myschedule.id

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavHostController) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("student") } // 'student' atau 'teacher'
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Field tambahan
    var universitas by remember { mutableStateOf("") }

    // Dropdown States
    var uniExpanded by remember { mutableStateOf(false) }
    
    // State untuk menyimpan daftar universitas dari Firestore
    var universityList by remember { mutableStateOf(listOf<String>()) }

    // Load Data Universities from Firestore
    LaunchedEffect(Unit) {
        FirebaseInstance.db.collection("universities")
            .get()
            .addOnSuccessListener { snapshot ->
                universityList = snapshot.documents.map { it.id }
            }
    }

    val context = LocalContext.current
    val textColor = Color.Black

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE3F2FD))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

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
        Text("Sign Up", fontSize = 26.sp, color = textColor)
        Spacer(modifier = Modifier.height(10.dp))

        // ==========================
        // PILIH ROLE (Radio Button)
        // ==========================
        Text("Pilih Role", color = textColor, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Start)) {
            RadioButton(
                selected = role == "student",
                onClick = { 
                    role = "student"
                    universitas = ""
                }
            )
            Text("Mahasiswa", color = textColor)

            Spacer(modifier = Modifier.width(16.dp))

            RadioButton(
                selected = role == "teacher",
                onClick = { 
                    role = "teacher"
                    universitas = ""
                }
            )
            Text("Dosen", color = textColor)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================
        // FORM INPUT
        // ==========================
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Lengkap", color = textColor) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = textColor) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================
        // UNIVERSITAS (Mahasiswa: Dropdown, Dosen: Input/Dropdown)
        // ==========================
        ExposedDropdownMenuBox(
            expanded = uniExpanded,
            onExpandedChange = { uniExpanded = !uniExpanded }
        ) {
            OutlinedTextField(
                value = universitas,
                onValueChange = { if(role == "teacher") universitas = it },
                readOnly = role == "student",
                label = { Text(if(role == "student") "Pilih Universitas" else "Pilih/Input Universitas") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uniExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = uniExpanded,
                onDismissRequest = { uniExpanded = false }
            ) {
                universityList.forEach { uni ->
                    DropdownMenuItem(
                        text = { Text(uni) },
                        onClick = {
                            universitas = uni
                            uniExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.isBlank() || universitas.isBlank()) {
                        errorMessage = "Semua field wajib diisi"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = ""

                    FirebaseInstance.auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: ""
                            val userRef = FirebaseInstance.db.collection("users").document(uid)
                            val uniRef = FirebaseInstance.db.collection("universities").document(universitas)

                            FirebaseInstance.db.runTransaction { transaction ->
                                // 1. Data User
                                val userData = mutableMapOf(
                                    "uid" to uid,
                                    "name" to name,
                                    "email" to email,
                                    "role" to role,
                                    "universitas" to universitas,
                                    "enrolled_classes" to listOf<String>()
                                )

                                if (role == "teacher") {
                                    // DOSEN: Universitas yang diinput saat signup langsung masuk ke list "managed_campus"
                                    userData["managed_campus"] = listOf(
                                        mapOf("uni" to universitas, "kelas" to "Umum") 
                                    )

                                    // 2. Pastikan Universitas terdaftar di koleksi global 'universities'
                                    val uniSnapshot = transaction.get(uniRef)
                                    if (!uniSnapshot.exists()) {
                                        transaction.set(uniRef, hashMapOf("status" to "active", "classes" to listOf<String>()))
                                    }
                                }

                                transaction.set(userRef, userData)
                                null
                            }.addOnSuccessListener {
                                isLoading = false
                                navController.navigate("login") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }.addOnFailureListener {
                                isLoading = false
                                errorMessage = "Gagal simpan data: ${it.message}"
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            errorMessage = "Gagal daftar: ${it.message}"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Create Account", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = { navController.navigate("login") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Sudah punya akun? Login di sini", color = textColor)
        }
    }
}