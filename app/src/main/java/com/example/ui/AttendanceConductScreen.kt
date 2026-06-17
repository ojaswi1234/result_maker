package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.Student
import com.example.data.ExcelBackupHelper
import com.example.viewmodel.AppViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceConductScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToIndiscipline: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.allStudents.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    
    var selectedClass by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedTerm by remember { mutableStateOf("Term 1") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    // State maps for attendance
    val attendanceStates = remember { mutableStateMapOf<Int, String>() }

    val filteredStudents = remember(students, selectedClass, selectedSection) {
        students.filter { it.className == selectedClass && it.sectionName == selectedSection }
    }

    // Initialize defaults when filtered students change
    LaunchedEffect(filteredStudents) {
        filteredStudents.forEach { student ->
            if (!attendanceStates.containsKey(student.id)) attendanceStates[student.id] = "Present"
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        selectedDate = date.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (selectedClass.isEmpty() || selectedSection.isEmpty()) {
                            Toast.makeText(context, "Select Class & Section to export", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val attendanceForSection = allAttendance.filter { record ->
                            filteredStudents.any { it.id == record.studentId }
                        }
                        val file = ExcelBackupHelper.exportAttendanceToExcel(
                            context,
                            selectedClass,
                            selectedSection,
                            attendanceForSection,
                            filteredStudents
                        )
                        if (file != null) {
                            ExcelBackupHelper.shareFile(context, file)
                        } else {
                            Toast.makeText(context, "Failed to export attendance", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Upload, contentDescription = "Export Attendance")
                    }
                    TextButton(onClick = onNavigateToIndiscipline) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Indiscipline Log", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (filteredStudents.isEmpty()) {
                        Toast.makeText(context, "No students to save", Toast.LENGTH_SHORT).show()
                        return@ExtendedFloatingActionButton
                    }
                    
                    filteredStudents.forEach { student ->
                        val status = attendanceStates[student.id] ?: "Present"
                        viewModel.saveAttendance(student.id, selectedDate, status, selectedTerm)
                    }
                    
                    Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_LONG).show()
                    // Optional: Clear attendance state or keep for visual confirmation
                },
                icon = { Icon(Icons.Default.Check, contentDescription = null) },
                text = { Text("Save Attendance") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Filters
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Class/Section Selector
                        val classes = students.map { "${it.className} - ${it.sectionName}" }.distinct()
                        var expanded by remember { mutableStateOf(false) }
                        
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = if (selectedClass.isNotEmpty()) "$selectedClass - $selectedSection" else "Select Class",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class & Section") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                classes.forEach { classSection ->
                                    DropdownMenuItem(
                                        text = { Text(classSection) },
                                        onClick = {
                                            val parts = classSection.split(" - ")
                                            selectedClass = parts[0]
                                            selectedSection = parts[1]
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Term Selector
                        var termExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = termExpanded,
                            onExpandedChange = { termExpanded = !termExpanded },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            OutlinedTextField(
                                value = selectedTerm,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Term") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termExpanded) },
                                modifier = Modifier.menuAnchor(),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            ExposedDropdownMenu(
                                expanded = termExpanded,
                                onDismissRequest = { termExpanded = false }
                            ) {
                                listOf("Term 1", "Term 2").forEach { term ->
                                    DropdownMenuItem(
                                        text = { Text(term) },
                                        onClick = {
                                            selectedTerm = term
                                            termExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Date Picker
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { },
                        label = { Text("Attendance Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
                        readOnly = true,
                        enabled = false, // Disable typing, force click
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (selectedClass.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a Class to start marking attendance", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredStudents) { student ->
                        StudentAttendanceRow(
                            student = student,
                            status = attendanceStates[student.id] ?: "Present",
                            onStatusChange = { attendanceStates[student.id] = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    student: Student,
    status: String,
    onStatusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Roll: ${student.rollNumber}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            // Attendance Selector
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("Present", "Absent", "Leave").forEach { option ->
                    val isSelected = status == option
                    Box(
                        modifier = Modifier
                            .clickable { onStatusChange(option) }
                            .background(
                                if (isSelected) {
                                    when (option) {
                                        "Present" -> Color(0xFF4CAF50)
                                        "Absent" -> Color(0xFFF44336)
                                        else -> Color(0xFFFFC107)
                                    }
                                } else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = option.take(1),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
