package com.myschedule.id.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance

@Composable
fun ResetPasswordScreen(navController: NavHostController, oobCode: String? = null) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Tema Biru Konsisten
    val bluePrimary = Color(0xFF1565C0)
    val blueLight = Color(0xFF64B5F6)

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
                .background(brush = Brush.verticalGradient(listOf(bluePrimary, blueLight))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (oobCode == null) "Lupa Kata Sandi" else "Atur Sandi Baru",
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            if (oobCode == null) {
                Text("Masukkan email terdaftar untuk menerima link reset kata sandi.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (email.isBlank()) return@Button
                        isLoading = true
                        FirebaseInstance.auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Link reset telah dikirim ke email!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Gagal: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Kirim Link Reset", color = Color.White)
                }
            } else {
                Text("Silakan masukkan kata sandi baru Anda.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Kata Sandi Baru") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            Toast.makeText(context, "Sandi minimal 6 karakter", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        FirebaseInstance.auth.confirmPasswordReset(oobCode, newPassword)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Sandi berhasil diubah! Silakan login.", Toast.LENGTH_LONG).show()
                                navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Gagal/Link Kadaluarsa: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = bluePrimary)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Simpan Sandi Baru", color = Color.White)
                }
            }
            
            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Kembali ke Login", color = bluePrimary)
            }
        }
    }
}
