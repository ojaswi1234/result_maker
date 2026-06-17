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
import com.example.viewmodel.AppViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceConductScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.allStudents.collectAsState()
    
    var selectedClass by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedTerm by remember { mutableStateOf("Term 1") }

    // State maps for attendance and discipline
    val attendanceStates = remember { mutableStateMapOf<Int, String>() }
    val disciplineGrades = remember { mutableStateMapOf<Int, String>() }
    val remarksStates = remember { mutableStateMapOf<Int, String>() }

    val filteredStudents = remember(students, selectedClass, selectedSection) {
        students.filter { it.className == selectedClass && it.sectionName == selectedSection }
    }

    // Initialize defaults when filtered students change
    LaunchedEffect(filteredStudents) {
        filteredStudents.forEach { student ->
            if (!attendanceStates.containsKey(student.id)) attendanceStates[student.id] = "Present"
            if (!disciplineGrades.containsKey(student.id)) disciplineGrades[student.id] = "A"
            if (!remarksStates.containsKey(student.id)) remarksStates[student.id] = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance & Conduct", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        val grade = disciplineGrades[student.id] ?: "A"
                        val remarks = remarksStates[student.id] ?: ""
                        
                        viewModel.saveAttendance(student.id, selectedDate, status, selectedTerm)
                        viewModel.saveDiscipline(student.id, selectedDate, remarks, grade, selectedTerm)
                    }
                    
                    Toast.makeText(context, "All records saved successfully!", Toast.LENGTH_LONG).show()
                    // Clear UI state
                    attendanceStates.clear()
                    disciplineGrades.clear()
                    remarksStates.clear()
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null) },
                text = { Text("Save All Records") },
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
            // Header Filters (Sticky-like at top of Column)
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
                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.weight(1f)
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

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        readOnly = true
                    )
                }
            }

            if (selectedClass.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a Class & Section to begin", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            onStatusChange = { attendanceStates[student.id] = it },
                            grade = disciplineGrades[student.id] ?: "A",
                            onGradeChange = { disciplineGrades[student.id] = it },
                            remarks = remarksStates[student.id] ?: "",
                            onRemarksChange = { remarksStates[student.id] = it }
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
    onStatusChange: (String) -> Unit,
    grade: String,
    onGradeChange: (String) -> Unit,
    remarks: String,
    onRemarksChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(text = "Roll No: ${student.rollNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Attendance Selector (Row of buttons)
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = option.take(1),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Discipline Grade Selector
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Grade:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    listOf("A", "B", "C").forEach { g ->
                        val isSelected = grade == g
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onGradeChange(g) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Remarks Field
                OutlinedTextField(
                    value = remarks,
                    onValueChange = onRemarksChange,
                    placeholder = { Text("Remarks (Optional)", fontSize = 12.sp) },
                    modifier = Modifier.weight(1.5f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
