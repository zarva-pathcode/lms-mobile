package com.myschedule.id.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import com.myschedule.id.FirebaseInstance
import com.myschedule.id.data.AbsensiRepository
import com.myschedule.id.data.ScheduleRepository
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAbsensiScreen(navController: NavHostController, uniName: String, className: String) {
    var mySchedules by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var selectedScheduleId by remember { mutableStateOf("") }
    var absensiList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    
    var showCreateScheduleDialog by remember { mutableStateOf(false) }
    var scheduleTitle by remember { mutableStateOf("") }
    var scheduleTime by remember { mutableStateOf("") }
    var selectedDayIndex by remember { mutableIntStateOf(Calendar.MONDAY) }
    
    var message by remember { mutableStateOf("") }
    
    val uid = FirebaseInstance.auth.currentUser?.uid ?: ""
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Tema Biru Konsisten
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

    fun loadData() {
        ScheduleRepository.getSchedulesByTeacher(uid) { all ->
            mySchedules = all.filter { it["universitas"] == uniName && it["kelas"] == className }
        }
    }

    fun loadAbsensi(scheduleId: String) {
        AbsensiRepository.getAbsensiBySchedule(scheduleId) { absensiList = it }
    }

    LaunchedEffect(Unit) { loadData() }
    
    if (message.isNotEmpty()) {
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute -> scheduleTime = "%02d:%02d".format(hour, minute) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(listOf(bluePrimary, blueLight)))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                    Column {
                        Text("Jadwal & Absensi: $className", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(uniName, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(bgLight).padding(paddingValues).padding(16.dp)) {
            
            // BLUEPRINT SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Blueprint Jadwal", fontWeight = FontWeight.Bold, color = bluePrimary)
                        IconButton(onClick = { 
                            message = ""
                            showCreateScheduleDialog = true 
                        }) {
                            Icon(Icons.Default.AddCircle, null, tint = bluePrimary)
                        }
                    }

                    if (mySchedules.isEmpty()) {
                        Text("Belum ada blueprint jadwal.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(mySchedules) { schedule ->
                                val dayIdx = (schedule["dayOfWeek"] as? Long)?.toInt() ?: Calendar.MONDAY
                                val isSelected = selectedScheduleId == schedule["id"]
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) bluePrimary.copy(0.1f) else Color.Transparent)
                                        .clickable { 
                                            selectedScheduleId = schedule["id"] as String
                                            loadAbsensi(selectedScheduleId)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isSelected) bluePrimary else Color.LightGray))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(schedule["title"].toString(), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("Setiap ${daysMap[dayIdx]} jam ${schedule["time"]}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // GENERATED SESSIONS SECTION
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, null, tint = bluePrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sesi Absensi Tergenerate", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedScheduleId.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TouchApp, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("Pilih blueprint di atas untuk melihat sesi", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else if (absensiList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Belum ada sesi yang tergenerate untuk jadwal ini.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(absensiList) { absensi ->
                        @Suppress("UNCHECKED_CAST")
                        val students = absensi["students"] as? List<Map<String, Any>> ?: listOf()
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(1.dp),
                            onClick = {
                                navController.navigate("teacher_absensi_detail/${absensi["id"]}/${absensi["title"]}")
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(absensi["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Tanggal: ${absensi["date"]}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { 
                                        AbsensiRepository.deleteAbsensi(absensi["id"] as String, { loadAbsensi(selectedScheduleId) }, { message = it })
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Color.Red.copy(0.7f), modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(0.5f))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.People, null, modifier = Modifier.size(14.dp), tint = bluePrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Total Input: ${students.size}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    
                                    Surface(
                                        color = bluePrimary.copy(0.1f),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            "Input Absensi", 
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            color = bluePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                if (students.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val summaryText = students.groupBy { it["status"] ?: "Hadir" }
                                        .map { "${it.key}: ${it.value.size}" }
                                        .joinToString(" • ")
                                    Text(summaryText, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        // DIALOG BUAT JADWAL (BLUEPRINT)
        if (showCreateScheduleDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateScheduleDialog = false
                    message = ""
                },
                title = { Text("Tambah Blueprint Jadwal") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (message.isNotEmpty()) {
                            Text(message, color = Color.Red, fontSize = 12.sp)
                        }
                        
                        OutlinedTextField(
                            value = scheduleTitle,
                            onValueChange = { scheduleTitle = it },
                            label = { Text("Nama Mata Kuliah") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Column {
                            Text("Pilih Hari:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(daysMap[selectedDayIndex] ?: "Pilih Hari")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    daysMap.forEach { (idx, name) ->
                                        DropdownMenuItem(text = { Text(name) }, onClick = { 
                                            selectedDayIndex = idx
                                            expanded = false 
                                        })
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth().clickable { timePickerDialog.show() }) {
                            OutlinedTextField(
                                value = scheduleTime,
                                onValueChange = {},
                                label = { Text("Jam") },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                placeholder = { Text("Klik untuk pilih jam") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = bluePrimary),
                        onClick = {
                            if (scheduleTitle.isBlank() || scheduleTime.isBlank()) {
                                message = "Judul dan Jam tidak boleh kosong"
                                return@Button
                            }
                            ScheduleRepository.addScheduleBlueprint(
                                scheduleTitle, selectedDayIndex, scheduleTime, uid, className, uniName,
                                onSuccess = {
                                    showCreateScheduleDialog = false
                                    scheduleTitle = ""; scheduleTime = ""; 
                                    message = "Jadwal berhasil disimpan"
                                    loadData()
                                },
                                onError = { 
                                    message = it 
                                }
                            )
                        }
                    ) { Text("Simpan Blueprint", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateScheduleDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}
