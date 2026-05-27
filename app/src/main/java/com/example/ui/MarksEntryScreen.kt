package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Mark
import com.example.data.Student
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksEntryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()

    // Aggregate unique classes and sections
    val availableClassSections = remember(allStudents) {
        allStudents.map { Pair(it.className, it.sectionName) }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
    }

    // Selected states
    var selectedClassSection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedSubject by remember { mutableStateOf("Mathematics") }

    // Dropdown expanded states
    var classMenuExpanded by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    // Init first selection if available
    LaunchedEffect(availableClassSections) {
        if (selectedClassSection == null && availableClassSections.isNotEmpty()) {
            selectedClassSection = availableClassSections.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic Marks Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("marks_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SELECTORS LAYOUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Class & Section Selector Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { classMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("class_selector_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = selectedClassSection?.let { "${it.first} - ${it.second}" } ?: "Select Class",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand")
                        }
                    }

                    DropdownMenu(
                        expanded = classMenuExpanded,
                        onDismissRequest = { classMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
                    ) {
                        availableClassSections.forEach { pair ->
                            DropdownMenuItem(
                                text = { Text("${pair.first} - Sect. ${pair.second}") },
                                onClick = {
                                    selectedClassSection = pair
                                    classMenuExpanded = false
                                },
                                modifier = Modifier.testTag("class_menu_item_${pair.first}_${pair.second}")
                            )
                        }
                    }
                }

                // Subject Selector Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { subjectMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("subject_selector_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = selectedSubject,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand")
                        }
                    }

                    DropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
                    ) {
                        viewModel.availableSubjects.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    selectedSubject = subject
                                    subjectMenuExpanded = false
                                },
                                modifier = Modifier.testTag("subject_menu_item_$subject")
                            )
                        }
                    }
                }
            }

            // STUDENTS MARKS GRID
            if (selectedClassSection != null) {
                val currentPair = selectedClassSection!!
                val classStudents = allStudents.filter {
                    it.className == currentPair.first && it.sectionName == currentPair.second
                }

                Text(
                    text = "Entering Marks for $selectedSubject (Out of 100)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (classStudents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No students enrolled in this class section yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(classStudents, key = { it.id }) { student ->
                            // Find existing mark for this student and subject
                            val studentMark = allMarks.find {
                                it.studentId == student.id && it.subjectName == selectedSubject
                            }

                            StudentMarkInputRow(
                                student = student,
                                currentMark = studentMark,
                                onSaveMark = { marksValue ->
                                    viewModel.saveMark(student.id, selectedSubject, marksValue)
                                }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a Class and Section above", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StudentMarkInputRow(
    student: Student,
    currentMark: Mark?,
    onSaveMark: (Double) -> Unit
) {
    var textValue by remember(student.id, currentMark) {
        mutableStateOf(currentMark?.marksObtained?.toInt()?.toString() ?: "")
    }

    var isError by remember { mutableStateOf(false) }
    var isSavedState by remember(currentMark) { mutableStateOf(currentMark != null) }

    // Run verification on input change
    val validateAndSave = { inputStr: String ->
        if (inputStr.isEmpty()) {
            isError = false
        } else {
            val potentialDouble = inputStr.toDoubleOrNull()
            if (potentialDouble != null && potentialDouble in 0.0..100.0) {
                isError = false
                isSavedState = true
                onSaveMark(potentialDouble)
            } else {
                isError = true
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mark_row_${student.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else if (isSavedState) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Roll ID: " + student.rollNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Inline Input Textfield
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (newValue.length <= 3) {
                            textValue = newValue
                            validateAndSave(newValue)
                        }
                    },
                    modifier = Modifier
                        .width(76.dp)
                        .height(52.dp)
                        .testTag("student_mark_input_${student.id}"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )

                // Responsive status icon
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (isError) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isSavedState) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Saved",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
