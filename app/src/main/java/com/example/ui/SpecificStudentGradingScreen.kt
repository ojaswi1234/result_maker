package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecificStudentGradingScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()
    val selectedClassSection by viewModel.selectedClassSection.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val activeExamType by viewModel.activeExamType.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = remember(allStudents, searchQuery, selectedClassSection) {
        if (selectedClassSection == null) {
            allStudents.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.rollNumber.contains(searchQuery, ignoreCase = true)
            }
        } else {
            val parts = selectedClassSection!!.split(" - ")
            val className = parts[0]
            val sectionName = parts[1]
            allStudents.filter {
                it.className == className && it.sectionName == sectionName &&
                (searchQuery.isEmpty() ||
                 it.name.contains(searchQuery, ignoreCase = true) ||
                 it.rollNumber.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    fun getMaxMarks(): Double {
        if (selectedClassSection == null) return 100.0
        val parts = selectedClassSection!!.split(" - ")
        val className = parts[0]
        val sectionName = parts[1]
        return viewModel.getMaxMarksForAssessment(className, sectionName, selectedSubject, activeExamType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search & Edit Marks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Filter by Name or Roll No") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            if (selectedSubject == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please select a subject in Marks Entry first.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        // Context Info
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Editing for: ${selectedSubject} | ${activeExamType}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (selectedClassSection != null) {
                                    Text(
                                        text = "Class: $selectedClassSection",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Student Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Marks (Max: ${getMaxMarks()})", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp), textAlign = TextAlign.Center)
                        }
                    }

                    if (filteredStudents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No students found matching your search.")
                            }
                        }
                    } else {
                        items(filteredStudents, key = { it.id }) { student ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Student Info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(student.rollNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Text("${student.className} - ${student.sectionName}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                // Input Col
                                val max = getMaxMarks()
                                val score = viewModel.getMarksForExam(student.id, selectedSubject!!, activeExamType)
                                val artEducation = stringResource(R.string.subject_art_education)
                                val gamesHealth = stringResource(R.string.subject_games_health)
                                val isCoScholastic = selectedSubject == artEducation || selectedSubject == gamesHealth

                                if (isCoScholastic) {
                                    GradeSelectionCell(
                                        initialValue = score,
                                        onSave = { gradeValue ->
                                            viewModel.updateMarksForExam(
                                                studentId = student.id,
                                                subjectName = selectedSubject!!,
                                                examType = activeExamType,
                                                newValue = gradeValue,
                                                maxMarks = 3.0
                                            )
                                        },
                                        isReadOnly = activeRole == "Principal/Coordinator",
                                        width = 120.dp
                                    )
                                } else {
                                    SingleMarkInputCell(
                                        initialValue = score,
                                        maxMarks = max,
                                        onSave = { valVal ->
                                            viewModel.updateMarksForExam(
                                                studentId = student.id,
                                                subjectName = selectedSubject!!,
                                                examType = activeExamType,
                                                newValue = valVal,
                                                maxMarks = max
                                            )
                                        },
                                        isReadOnly = activeRole == "Principal/Coordinator",
                                        width = 120.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
