package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.Mark
import com.example.data.Student
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarksEntryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigateToStudentSearch: () -> Unit
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()
    val allExamConfigs by viewModel.allExamConfigs.collectAsState()
    val allSectionSubjects by viewModel.allSectionSubjects.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    // 1. Selector State Values from ViewModel
    val selectedClassSection by viewModel.selectedClassSection.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val activeExamType by viewModel.activeExamType.collectAsState()

    // Dropdown expanding states
    var classExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }
    var examTypeExpanded by remember { mutableStateOf(false) }

    // Toggle whether sheet/grading list is unlocked
    var isGradingUnlocked by remember { mutableStateOf(false) }

    val examTypes = listOf("PT 1", "PT 2", "FA 1", "Term 1 Internal", "PT 3", "PT 4", "FA 2", "Term 2 Internal")

    // Aggregate unique classes
    val classesList = remember(allStudents) {
        allStudents.map { "${it.className} - ${it.sectionName}" }.distinct().sorted()
    }

    // Reset subsequent selections when previous fields change
    LaunchedEffect(selectedClassSection) {
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
                val config = allExamConfigs.find { it.className == className }
                val baseSubjects = if (config != null && config.mainSubjectsString.isNotEmpty()) {
                    config.mainSubjectsString.split("|").filter { it.isNotEmpty() }.toMutableList()
                } else {
                    viewModel.availableSubjects.toMutableList()
                }
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

    // Helper to get max marks for a specific assessment type
    fun getMaxMarksForAssessment(assessmentType: String): Double {
        if (selectedClassSection == null) return 100.0
        val parts = selectedClassSection!!.split(" - ")
        val className = parts.firstOrNull() ?: ""
        val sectionName = parts.getOrNull(1) ?: ""
        return viewModel.getMaxMarksForAssessment(className, sectionName, selectedSubject, assessmentType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isGradingUnlocked) "Grading: $activeExamType" else stringResource(R.string.marks_entry_title), 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("marks_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
                    }
                },
                actions = {
                    // Search icon button
                    IconButton(onClick = onNavigateToStudentSearch) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Student")
                    }

                    // Active role badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when(currentUserRole) {
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
                            Text(
                                text = when(currentUserRole) {
                                    "Admin" -> stringResource(R.string.role_admin)
                                    "Teacher" -> stringResource(R.string.role_teacher)
                                    else -> currentUserRole ?: ""
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = isGradingUnlocked,
            label = "FormUnlockTransition",
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) { unlocked ->
            if (!unlocked) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // WORKFLOW FORM SELECTORS
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.marks_step_1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Selector Class Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.select_class_section_label), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { classExpanded = true },
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp).testTag("workflow_class_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedClassSection ?: stringResource(R.string.select_class_placeholder), color = MaterialTheme.colorScheme.onSurface)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = classExpanded, onDismissRequest = { classExpanded = false }) {
                                    classesList.forEach { cls ->
                                        DropdownMenuItem(
                                            text = { Text(cls) },
                                            onClick = {
                                                viewModel.updateSelectedClassSection(cls)
                                                classExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_class_$cls")
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Subject Selector Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.select_subject_label), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { subjectExpanded = true },
                                    enabled = selectedClassSection != null,
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp).testTag("workflow_subject_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(selectedSubject ?: stringResource(R.string.select_subject_placeholder), color = if (selectedClassSection != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                                    subjectsList.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub) },
                                            onClick = {
                                                viewModel.updateSelectedSubject(sub)
                                                subjectExpanded = false
                                            },
                                            modifier = Modifier.testTag("menu_subject_$sub")
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Exam Type Selector Dropdown
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Select Exam Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { examTypeExpanded = true },
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(activeExamType, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "")
                                    }
                                }
                                DropdownMenu(expanded = examTypeExpanded, onDismissRequest = { examTypeExpanded = false }) {
                                    examTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                viewModel.updateActiveExamType(type)
                                                examTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PRIMARY CTA BUTTON: “Marks Entry”
                        val isFormComplete = selectedClassSection != null && selectedSubject != null
                        
                        Button(
                            onClick = {
                                if (isFormComplete) {
                                    isGradingUnlocked = true
                                } else {
                                    Toast.makeText(context, context.getString(R.string.error_configure_selections), Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isFormComplete,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 8.dp).testTag("marks_entry_cta"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Grading, contentDescription = "")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.begin_marks_entry))
                        }
                    }
                }
            } else {
                // SINGLE-EXAM MODE GRID VIEW
                val selectedParts = selectedClassSection!!.split(" - ")
                val className = selectedParts[0]
                val sectionName = selectedParts[1]
                
                val filteredStudents = allStudents.filter {
                    it.className == className && it.sectionName == sectionName
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with Switcher
                    Surface(
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("$selectedClassSection", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("$selectedSubject", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Mini switcher for exam type
                            Box {
                                TextButton(onClick = { examTypeExpanded = true }) {
                                    Text(activeExamType, fontWeight = FontWeight.ExtraBold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(expanded = examTypeExpanded, onDismissRequest = { examTypeExpanded = false }) {
                                    examTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                viewModel.updateActiveExamType(type)
                                                examTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            IconButton(onClick = { isGradingUnlocked = false }) {
                                Icon(Icons.Default.Settings, contentDescription = "Change Config", modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Sticky Header for the 2 columns
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Student Name", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("Marks (Max: ${getMaxMarksForAssessment(activeExamType)})", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp), textAlign = TextAlign.Center)
                            }
                        }

                        if (filteredStudents.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(stringResource(R.string.no_students_enrolled))
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
                                    // Student Col
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
                                        Text(student.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    
                                    // Input Col
                                    val max = getMaxMarksForAssessment(activeExamType)
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
                                            isReadOnly = false,
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
                                            isReadOnly = false,
                                            width = 120.dp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Global Save Button
                    if (true) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.marks_saved_toast), Toast.LENGTH_SHORT).show()
                                    isGradingUnlocked = false
                                },
                                modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Complete Grading")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleMarkInputCell(
    initialValue: Double?,
    maxMarks: Double,
    onSave: (String) -> Unit,
    isReadOnly: Boolean,
    width: androidx.compose.ui.unit.Dp
) {
    var textValue by remember {
        val str = if (initialValue != null) {
            if (initialValue % 1.0 == 0.0) initialValue.toInt().toString() else initialValue.toString()
        } else ""
        mutableStateOf(str)
    }

    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(initialValue) {
        if (!isFocused) {
            val currentDouble = textValue.toDoubleOrNull()
            if (initialValue != currentDouble) {
                val str = if (initialValue != null) {
                    if (initialValue % 1.0 == 0.0) initialValue.toInt().toString() else initialValue.toString()
                } else ""
                textValue = str
            }
        }
    }

    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.width(width),
        contentAlignment = Alignment.Center
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (!isReadOnly && newValue.length <= 5) {
                    if (newValue.isEmpty()) {
                        textValue = newValue
                        isError = false
                        onSave("")
                    } else if (newValue == ".") {
                        textValue = newValue
                        isError = false
                    } else if (newValue.endsWith(".") && newValue.dropLast(1).toDoubleOrNull() != null) {
                        val potentialDouble = newValue.dropLast(1).toDoubleOrNull()
                        if (potentialDouble != null && potentialDouble >= 0.0 && potentialDouble <= maxMarks) {
                            textValue = newValue
                            isError = false
                            onSave(newValue)
                        }
                    } else {
                        val potentialDouble = newValue.toDoubleOrNull()
                        if (potentialDouble != null && potentialDouble >= 0.0 && potentialDouble <= maxMarks) {
                            textValue = newValue
                            isError = false
                            onSave(newValue)
                        }
                    }
                }
            },
            readOnly = isReadOnly,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 4.dp)
                .androidx.compose.ui.focus.onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            ),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = textValue,
                    innerTextField = innerTextField,
                    enabled = !isReadOnly,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    trailingIcon = {
                        val maxStr = if (maxMarks % 1.0 == 0.0) maxMarks.toInt().toString() else maxMarks.toString()
                        Text(
                            text = "/ $maxStr",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = !isReadOnly,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    },
                    contentPadding = PaddingValues(0.dp)
                )
            }
        )
    }
}

@Composable
fun GradeSelectionCell(
    initialValue: Double?,
    onSave: (String) -> Unit,
    isReadOnly: Boolean,
    width: androidx.compose.ui.unit.Dp
) {
    val grades = listOf("A", "B", "C")
    val gradeMap = mapOf(3.0 to "A", 2.0 to "B", 1.0 to "C")
    val reverseMap = mapOf("A" to "3", "B" to "2", "C" to "1")
    
    val currentGrade = gradeMap[initialValue] ?: ""
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.width(width), contentAlignment = Alignment.Center) {
        OutlinedButton(
            onClick = { if (!isReadOnly) expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White
            )
        ) {
            Text(
                text = currentGrade.ifEmpty { "Grade" },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = if (currentGrade.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurface
            )
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            grades.forEach { g ->
                DropdownMenuItem(
                    text = { Text(g, fontWeight = FontWeight.Bold) },
                    onClick = {
                        onSave(reverseMap[g]!!)
                        expanded = false
                    }
                )
            }
        }
    }
}
