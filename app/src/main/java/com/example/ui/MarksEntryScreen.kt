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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import com.example.R
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
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    // Dropdown expanding states
    var classExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    // Toggle whether sheet/grading list is unlocked
    var isGradingUnlocked by remember { mutableStateOf(false) }

    // Aggregate unique classes
    val classesList = remember(allStudents) {
        allStudents.map { "${it.className} - ${it.sectionName}" }.distinct().sorted()
    }

    // Reset subsequent selections when previous fields change
    LaunchedEffect(selectedClassSection) {
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

    // Helper to get max marks for a specific assessment type
    fun getMaxMarksForAssessment(assessmentType: String): Double {
        if (selectedClassSection == null) return 100.0
        val parts = selectedClassSection!!.split(" - ")
        val className = parts.firstOrNull() ?: ""
        val sectionName = parts.getOrNull(1) ?: ""
        val config = allExamConfigs.find { it.className == className }

        return when (assessmentType) {
            "UT 1" -> config?.t1PaMaxMarks1 ?: 20.0
            "UT 2" -> config?.t1PaMaxMarks2 ?: 20.0
            "Term 1" -> {
                val secSub = allSectionSubjects.find { it.className == className && it.sectionName == sectionName && it.subjectName == selectedSubject }
                secSub?.maxMarks ?: 80.0
            }
            "UT 3" -> config?.t2PaMaxMarks1 ?: 20.0
            "UT 4" -> config?.t2PaMaxMarks2 ?: 20.0
            "Term 2" -> {
                val secSub = allSectionSubjects.find { it.className == className && it.sectionName == sectionName && it.subjectName == selectedSubject }
                secSub?.maxMarks ?: 80.0
            }
            else -> 100.0
        }
    }

    // Auto-save toggle preference
    var isAutoSaveEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.marks_entry_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("marks_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.go_back))
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
                            Text(
                                text = if (activeRole == "Principal/Coordinator") stringResource(R.string.coordinator) else {
                                    when(activeRole) {
                                        "Admin" -> stringResource(R.string.role_admin)
                                        "Teacher" -> stringResource(R.string.role_teacher)
                                        else -> activeRole
                                    }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (!unlocked) {
                    item {
                        // WORKFLOW FORM SELECTORS
                        Column(
                            modifier = Modifier.fillMaxWidth(),
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
                                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_class_selector"),
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
                                                    selectedClassSection = cls
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
                                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("workflow_subject_selector"),
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
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("marks_entry_cta"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Grading, contentDescription = "")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.begin_marks_entry))
                            }
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

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Info Header Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(stringResource(R.string.active_classroom_grid), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    Text("$selectedClassSection", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(stringResource(R.string.subjects) + ": $selectedSubject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    Text(stringResource(R.string.enable_auto_save), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }

                                TextButton(
                                    onClick = { isGradingUnlocked = false },
                                    modifier = Modifier.testTag("back_to_selectors")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.refine_selections))
                                    }
                                }
                            }
                        }
                    }

                    // Student score rows list
                    if (filteredStudents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_students_enrolled))
                            }
                        }
                    } else {
                        items(filteredStudents, key = { it.id }) { student ->
                            val studentMarks = allMarks.filter {
                                it.studentId == student.id && it.subjectName == selectedSubject
                            }

                            StudentTermMarksEntry(
                                student = student,
                                studentMarks = studentMarks,
                                isAutoSave = isAutoSaveEnabled,
                                isReadOnly = activeRole == "Principal/Coordinator",
                                getMaxMarks = { type -> getMaxMarksForAssessment(type) },
                                onSaveMark = { type, term, markVal, max ->
                                    viewModel.saveMark(
                                        studentId = student.id,
                                        subject = selectedSubject!!,
                                        termName = term,
                                        examType = type,
                                        marks = markVal,
                                        maxMarks = max
                                    )
                                }
                            )
                        }
                    }

                    // Manual save button if Auto save is off
                    if (!isAutoSaveEnabled && activeRole != "Principal/Coordinator") {
                        item {
                            Button(
                                onClick = {
                                    Toast.makeText(context, context.getString(R.string.marks_saved_toast), Toast.LENGTH_SHORT).show()
                                    isGradingUnlocked = false
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_all_marks_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = "")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.save_current_transcript))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentTermMarksEntry(
    student: Student,
    studentMarks: List<Mark>,
    isAutoSave: Boolean,
    isReadOnly: Boolean,
    getMaxMarks: (String) -> Double,
    onSaveMark: (String, String, Double, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Student Name Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.rollNumber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = student.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Term 1 Card
            TermCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.term_1),
                assessmentTypes = listOf("UT 1", "UT 2", "Term 1"),
                studentMarks = studentMarks,
                isAutoSave = isAutoSave,
                isReadOnly = isReadOnly,
                getMaxMarks = getMaxMarks,
                onSaveMark = { type, mark -> onSaveMark(type, "Term 1", mark, getMaxMarks(type)) }
            )

            // Term 2 Card
            TermCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.term_2),
                assessmentTypes = listOf("UT 3", "UT 4", "Term 2"),
                studentMarks = studentMarks,
                isAutoSave = isAutoSave,
                isReadOnly = isReadOnly,
                getMaxMarks = getMaxMarks,
                onSaveMark = { type, mark -> onSaveMark(type, "Term 2", mark, getMaxMarks(type)) }
            )
        }
    }
}

@Composable
fun TermCard(
    modifier: Modifier = Modifier,
    title: String,
    assessmentTypes: List<String>,
    studentMarks: List<Mark>,
    isAutoSave: Boolean,
    isReadOnly: Boolean,
    getMaxMarks: (String) -> Double,
    onSaveMark: (String, Double) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            assessmentTypes.forEach { type ->
                val currentMark = studentMarks.find { it.examType == type }
                val max = getMaxMarks(type)
                
                AssessmentInputField(
                    label = type,
                    currentMark = currentMark,
                    maxMarks = max,
                    isAutoSave = isAutoSave,
                    isReadOnly = isReadOnly,
                    onSave = { onSaveMark(type, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentInputField(
    label: String,
    currentMark: Mark?,
    maxMarks: Double,
    isAutoSave: Boolean,
    isReadOnly: Boolean,
    onSave: (Double) -> Unit
) {
    // Local state for smooth typing
    var textValue by remember { mutableStateOf("") }
    
    // Initial sync with database value
    LaunchedEffect(currentMark) {
        val score = currentMark?.marksObtained
        val currentStr = if (score != null) {
            if (score % 1.0 == 0.0) score.toInt().toString() else score.toString()
        } else ""
        
        // Only update local state if it's currently empty and external state is not,
        // or if external state changes significantly while not in focus (simplified here)
        if (textValue.isEmpty() && currentStr.isNotEmpty()) {
            textValue = currentStr
        }
    }

    var isError by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = stringResource(R.string.assessment_max_format, label, maxMarks.toInt()), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                if (!isReadOnly && newValue.length <= 5) {
                    textValue = newValue
                    val potentialDouble = newValue.toDoubleOrNull()
                    if (newValue.isEmpty()) {
                        isError = false
                    } else if (potentialDouble != null && potentialDouble >= 0.0 && potentialDouble <= maxMarks) {
                        isError = false
                        if (isAutoSave) onSave(potentialDouble)
                    } else {
                        isError = true
                    }
                }
            },
            readOnly = isReadOnly,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface, 
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = @Composable { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = textValue,
                    innerTextField = innerTextField,
                    enabled = !isReadOnly,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = isError,
                    trailingIcon = {
                        if (currentMark != null && !isError) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        errorContainerColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = !isReadOnly,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                errorContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                )
            }
        )
    }
}
