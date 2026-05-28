package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Grading
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()
    val allExamConfigs by viewModel.allExamConfigs.collectAsState()
    val allSectionSubjects by viewModel.allSectionSubjects.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()

    // 1. Selector State Values
    var selectedClassSection by remember { mutableStateOf<String?>(null) }
    var selectedTerm by remember { mutableStateOf<String?>(null) }
    var selectedAssessmentType by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    // Dropdown expanding states
    var classExpanded by remember { mutableStateOf(false) }
    var termExpanded by remember { mutableStateOf(false) }
    var assessmentExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    // Toggle whether sheet/grading list is unlocked
    var isGradingUnlocked by remember { mutableStateOf(false) }

    // Aggregate unique classes
    val classesList = remember(allStudents) {
        allStudents.map { "${it.className} - ${it.sectionName}" }.distinct().sorted()
    }

    // Dynamic terms available
    val termsList = listOf("Term 1", "Term 2")

    // Dynamic assessment modes based on Selected Term
    val assessmentTypeList = remember(selectedTerm) {
        if (selectedTerm == "Term 1") {
            listOf(
                "Term 1 - Periodic Assessment (PA)",
                "Term 1 - Practical",
                "Term 1 - Internal Assessment",
                "Term 1 - Final Term Exam"
            )
        } else if (selectedTerm == "Term 2") {
            listOf(
                "Term 2 - Periodic Assessment (PA)",
                "Term 2 - Practical",
                "Term 2 - Internal Assessment",
                "Term 2 - Final Term Exam"
            )
        } else {
            listOf("Periodic Assessment (PA)", "Term Exam", "Practical", "Internal Assessment")
        }
    }

    // Reset subsequent selections when previous fields change
    LaunchedEffect(selectedClassSection) {
        selectedSubject = null
        isGradingUnlocked = false
    }
    LaunchedEffect(selectedTerm) {
        selectedAssessmentType = null
        selectedSubject = null
        isGradingUnlocked = false
    }
    LaunchedEffect(selectedAssessmentType) {
        selectedSubject = null
        isGradingUnlocked = false
    }

    // Dynamic Subject dropdown based on selected Class Configuration & Section-Specific Subjects
    val subjectsList = remember(selectedClassSection, allSectionSubjects, allExamConfigs) {
        if (selectedClassSection != null) {
            val parts = selectedClassSection!!.split(" - ")
            val className = parts.firstOrNull() ?: ""
            val sectionName = parts.getOrNull(1) ?: ""
            val customSubjects = allSectionSubjects.filter { it.className == className && it.sectionName == sectionName }
            if (customSubjects.isNotEmpty()) {
                customSubjects.map { it.subjectName }
            } else {
                val baseSubjects = viewModel.availableSubjects.toMutableList()
                val config = allExamConfigs.find { it.className == className }
                if (config != null && config.additionalSubjectsString.isNotEmpty()) {
                    config.additionalSubjectsString.split("|").forEach { row ->
                        if (row.contains(":")) {
                            val name = row.split(":").first()
                            if (name.isNotEmpty() && !baseSubjects.contains(name)) {
                                baseSubjects.add(name)
                            }
                        }
                    }
                }
                baseSubjects.toList()
            }
        } else {
            viewModel.availableSubjects
        }
    }

    // Determine configured max marks for current selected mode
    val currentMaxMarks = remember(selectedClassSection, selectedAssessmentType, selectedSubject, allSectionSubjects, allExamConfigs) {
        if (selectedClassSection == null) return@remember 100.0
        val parts = selectedClassSection!!.split(" - ")
        val className = parts.firstOrNull() ?: ""
        val sectionName = parts.getOrNull(1) ?: ""
        val config = allExamConfigs.find { it.className == className }
        
        when {
            selectedAssessmentType != null && selectedAssessmentType!!.contains("Periodic Assessment") -> {
                if (selectedAssessmentType!!.contains("Term 1")) {
                    config?.t1PaMaxMarks1 ?: 20.0
                } else {
                    config?.t2PaMaxMarks1 ?: 20.0
                }
            }
            selectedSubject != null -> {
                val secSub = allSectionSubjects.find { 
                    it.className == className && 
                    it.sectionName == sectionName && 
                    it.subjectName == selectedSubject 
                }
                if (secSub != null) {
                    secSub.maxMarks
                } else {
                    val addSubRow = config?.additionalSubjectsString?.split("|")?.find { it.startsWith("$selectedSubject:") }
                    if (addSubRow != null) {
                        addSubRow.split(":").getOrNull(1)?.toDoubleOrNull() ?: 50.0
                    } else {
                        100.0
                    }
                }
            }
            else -> 100.0
        }
    }

    // Auto-save toggle preference
    var isAutoSaveEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grading & Marks Input", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("marks_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    // Active role badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when(activeRole) {
                            "Admin" -> MaterialTheme.colorScheme.primaryContainer
                            "Teacher" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.tertiaryContainer
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = "", modifier = Modifier.size(12.dp))
                            Text(activeRole, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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
            
            // TRANSITION PANEL: SELECTION WORKFLOW vs ACTIVE GRADING VIEW
            AnimatedContent(
                targetState = isGradingUnlocked,
                label = "FormUnlockTransition"
            ) { unlocked ->
                if (!unlocked) {
                    // WORKFLOW FORM SELECTORS
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Step 1: Term Evaluation Parameters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Selector Class Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Class & Section:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { classExpanded = true },
                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_class_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedClassSection ?: "Select Class", color = MaterialTheme.colorScheme.onSurface)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                                    classesList.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls) },
                                            onClick = {
                                                selectedClassSection = cls
                                                classExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_class_$cls")
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Academic Term Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Academic Term:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { termExpanded = true },
                                    enabled = selectedClassSection != null,
                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_term_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedTerm ?: "Select Term", color = if (selectedClassSection != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = termExpanded, onDismissRequest = { termExpanded = false }) {
                                    termsList.forEach { term ->
                                        DropdownMenuItem(
                                            text = { Text(term) },
                                            onClick = {
                                                selectedTerm = term
                                                termExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_term_$term")
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Assessment Type Selector (Periodic Assessment PA, Term Exam, Practical, Internal Assessment, etc.)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Assessment/Exam Type:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { assessmentExpanded = true },
                                    enabled = selectedTerm != null,
                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_assessment_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedAssessmentType ?: "Select Assessment Mode", color = if (selectedTerm != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = assessmentExpanded, onDismissRequest = { assessmentExpanded = false }) {
                                    assessmentTypeList.forEach { assType ->
                                        DropdownMenuItem(
                                            text = { Text(assType) },
                                            onClick = {
                                                selectedAssessmentType = assType
                                                assessmentExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_assessment_$assType")
                                        )
                                    }
                                }
                            }
                        }

                        // 4. Subject Selector Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Subject:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { subjectExpanded = true },
                                    enabled = selectedAssessmentType != null,
                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_subject_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedSubject ?: "Select Subject", color = if (selectedAssessmentType != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                                    subjectsList.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub) },
                                            onClick = {
                                                selectedSubject = sub
                                                subjectExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_subject_$sub")
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PRIMARY CTA BUTTON: “Marks Entry”
                        val isFormComplete = selectedClassSection != null && selectedTerm != null && selectedAssessmentType != null && selectedSubject != null
                        
                        Button(
                            onClick = {
                                if (isFormComplete) {
                                    isGradingUnlocked = true
                                } else {
                                    Toast.makeText(context, "Please configure all selections before starting.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isFormComplete,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("marks_entry_cta"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Grading, contentDescription = "")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Begin Marks Entry")
                        }
                    }
                } else {
                    // GRADING LIST FOR CHOSEN COMBINATION
                    val selectedParts = selectedClassSection!!.split(" - ")
                    val className = selectedParts[0]
                    val sectionName = selectedParts[1]
                    
                    val filteredStudents = allStudents.filter {
                        it.className == className && it.sectionName == sectionName
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Info Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Active Classroom Grid", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                Text("$selectedClassSection • $selectedTerm", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Exam Mode: $selectedAssessmentType", fontSize = 12.sp)
                                Text("Subject: $selectedSubject (Out of ${currentMaxMarks.toInt()})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Auto-Save / Save preference controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isAutoSaveEnabled,
                                    onCheckedChange = { isAutoSaveEnabled = it },
                                    modifier = Modifier.testTag("auto_save_toggle")
                                )
                                Text("Enable Auto-Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = { isGradingUnlocked = false },
                                modifier = Modifier.testTag("back_to_selectors")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refine Selections")
                                }
                            }
                        }

                        // Warning banner for Coordinator
                        if (activeRole == "Principal/Coordinator") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = "", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Text("Read-Only: Coordinator Role cannot enter/modify marks.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Student score rows list
                        if (filteredStudents.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No students currently enrolled in this classroom.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredStudents, key = { it.id }) { student ->
                                    val currentMark = allMarks.find {
                                        it.studentId == student.id &&
                                        it.subjectName == selectedSubject &&
                                        it.termName == selectedTerm &&
                                        it.examType == selectedAssessmentType
                                    }

                                    StudentMarkInputRow(
                                        student = student,
                                        currentMark = currentMark,
                                        maxMarks = currentMaxMarks,
                                        isAutoSave = isAutoSaveEnabled,
                                        isReadOnly = activeRole == "Principal/Coordinator",
                                        onSaveMark = { markVal ->
                                            viewModel.saveMark(
                                                studentId = student.id,
                                                subject = selectedSubject!!,
                                                termName = selectedTerm!!,
                                                examType = selectedAssessmentType!!,
                                                marks = markVal,
                                                maxMarks = currentMaxMarks
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Manual save button if Auto save is off
                        if (!isAutoSaveEnabled && activeRole != "Principal/Coordinator") {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "All entered marks cached and recorded!", Toast.LENGTH_SHORT).show()
                                    isGradingUnlocked = false
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_all_marks_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = "")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Current Transcript")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentMarkInputRow(
    student: Student,
    currentMark: Mark?,
    maxMarks: Double,
    isAutoSave: Boolean,
    isReadOnly: Boolean,
    onSaveMark: (Double) -> Unit
) {
    var textValue by remember(student.id, currentMark) {
        val score = currentMark?.marksObtained
        mutableStateOf(if (score != null) {
            if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
        } else "")
    }

    var isError by remember { mutableStateOf(false) }
    var isSavedState by remember(currentMark) { mutableStateOf(currentMark != null) }

    val validateAndSave = { inputStr: String ->
        if (inputStr.isEmpty()) {
            isError = false
        } else {
            val potentialDouble = inputStr.toDoubleOrNull()
            if (potentialDouble != null && potentialDouble >= 0.0 && potentialDouble <= maxMarks) {
                isError = false
                if (isAutoSave) {
                    isSavedState = true
                    onSaveMark(potentialDouble)
                }
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
                modifier = Modifier.weight(1.2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Inline Input Textfield
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        if (!isReadOnly && newValue.length <= 5) {
                            textValue = newValue
                            validateAndSave(newValue)
                        }
                    },
                    readOnly = isReadOnly,
                    enabled = !isReadOnly,
                    modifier = Modifier
                        .width(82.dp)
                        .height(52.dp)
                        .testTag("student_mark_input_${student.id}"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0-${maxMarks.toInt()}", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )

                // Save button if not AutoSave
                if (!isAutoSave && !isReadOnly) {
                    IconButton(
                        onClick = {
                            val potentialDouble = textValue.toDoubleOrNull()
                            if (potentialDouble != null && potentialDouble >= 0.0 && potentialDouble <= maxMarks) {
                                isError = false
                                isSavedState = true
                                onSaveMark(potentialDouble)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Status Indicator Icon
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
