package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Student
import com.example.viewmodel.AppViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndisciplineReportScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val students by viewModel.allStudents.collectAsState()
    
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedGrade by remember { mutableStateOf("A") }
    var incidentDescription by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedTerm by remember { mutableStateOf("Term 1") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

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
                title = { Text("Indiscipline Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Log Behavioral Incident",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Student Selector
            var studentExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = studentExpanded,
                onExpandedChange = { studentExpanded = !studentExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedStudent?.let { "${it.name} (${it.className}-${it.sectionName})" } ?: "Select Student",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Student") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = studentExpanded,
                    onDismissRequest = { studentExpanded = false }
                ) {
                    students.forEach { student ->
                        DropdownMenuItem(
                            text = { Text("${student.name} (${student.className}-${student.sectionName})") },
                            onClick = {
                                selectedStudent = student
                                studentExpanded = false
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        modifier = Modifier.menuAnchor()
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

                // Date Selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f).height(56.dp).padding(top = 8.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.Event, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(selectedDate, fontSize = 14.sp)
                }
            }

            // Grade Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Discipline Grade", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("A", "B", "C").forEach { grade ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = { selectedGrade = grade },
                            label = { Text(grade, modifier = Modifier.padding(horizontal = 8.dp)) },
                            leadingIcon = if (selectedGrade == grade) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Incident Description
            OutlinedTextField(
                value = incidentDescription,
                onValueChange = { incidentDescription = it },
                label = { Text("Incident Case Description") },
                placeholder = { Text("Provide detailed account of the behavior or incident...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                minLines = 5,
                shape = RoundedCornerShape(12.dp)
            )

            // Submit Button
            Button(
                onClick = {
                    if (selectedStudent == null) {
                        Toast.makeText(context, "Please select a student", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (incidentDescription.isBlank()) {
                        Toast.makeText(context, "Please provide an incident description", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.saveDiscipline(
                        studentId = selectedStudent!!.id,
                        date = selectedDate,
                        description = incidentDescription,
                        grade = selectedGrade,
                        termName = selectedTerm
                    )

                    Toast.makeText(context, "Incident report saved successfully", Toast.LENGTH_LONG).show()
                    
                    // Reset fields
                    selectedStudent = null
                    incidentDescription = ""
                    selectedGrade = "A"
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Report, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Submit Formal Report", fontWeight = FontWeight.Bold)
            }
        }
    }
}
