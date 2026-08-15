package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.Student
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSectionScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var showEditStudentDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    
    // Active detail selection
    var selectedClassSection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showingSubjectsPage by remember { mutableStateOf(false) }

    // Aggregate unique Class + Section lists from students database
    val classSectionsMap = remember(allStudents) {
        val map = mutableMapOf<Pair<String, String>, Int>()
        // Ensure some initial preset groupings are visible even if student list goes empty, but primarily rely on student distribution
        allStudents.forEach { student ->
            val key = Pair(student.className, student.sectionName)
            map[key] = (map[key] ?: 0) + 1
        }
        // If map is completely empty, we can show a nice placeholder state or guide
        map.toList().sortedWith(compareBy({ it.first.first }, { it.first.second }))
    }

    val csvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && selectedClassSection != null) {
            viewModel.importStudentsFromCSV(context, uri, selectedClassSection!!.first, selectedClassSection!!.second)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (showingSubjectsPage && selectedClassSection != null) {
                            stringResource(R.string.subjects_title_format, selectedClassSection!!.first, selectedClassSection!!.second)
                        } else if (selectedClassSection != null) {
                            stringResource(R.string.class_section_title_format, selectedClassSection!!.first, selectedClassSection!!.second)
                        } else {
                            stringResource(R.string.classes_sections_title)
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showingSubjectsPage) {
                                showingSubjectsPage = false
                            } else if (selectedClassSection != null) {
                                selectedClassSection = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("class_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.go_back)
                        )
                    }
                },
                actions = {
                    if (selectedClassSection != null && !showingSubjectsPage) {
                        IconButton(
                            onClick = { csvLauncher.launch("*/*") },
                            modifier = Modifier.testTag("import_csv_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = "Import CSV")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showingSubjectsPage) {
                FloatingActionButton(
                    onClick = { showAddStudentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_student))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            if (selectedClassSection != null) {
                // Roster list of selected class
                val currentClass = selectedClassSection!!.first
                val currentSection = selectedClassSection!!.second

                if (showingSubjectsPage) {
                    SectionSubjectsView(
                        className = currentClass,
                        sectionName = currentSection,
                        viewModel = viewModel,
                        onBack = { showingSubjectsPage = false }
                    )
                } else {
                    val classStudents = allStudents.filter {
                        it.className == currentClass && it.sectionName == currentSection
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.class_roster_portfolio),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showingSubjectsPage = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("view_modify_subjects_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = stringResource(R.string.subjects),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.view_modify_subjects), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val file = com.example.data.ExcelBackupHelper.generateClassRosterExcel(
                                                context,
                                                currentClass,
                                                currentSection,
                                                classStudents
                                            )
                                            if (file != null) {
                                                com.example.data.ExcelBackupHelper.downloadFile(context, file)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("download_excel_roster_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.download_excel_roster), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                                    }
                                }
                            }
                        }

                        if (classStudents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(stringResource(R.string.no_students_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(onClick = { showAddStudentDialog = true }) {
                                            Text(stringResource(R.string.add_first_student))
                                        }
                                    }
                                }
                            }
                        } else {
                            items(classStudents, key = { it.id }) { student ->
                                StudentRowCard(
                                    student = student,
                                    onEdit = {
                                        studentToEdit = student
                                        showEditStudentDialog = true
                                    },
                                    onDelete = { viewModel.deleteStudent(student) }
                                )
                            }
                        }
                    }
                }
            } else {
                // OVERVIEW LIST OF CLASS SECTIONS
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.active_grade_registers),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (classSectionsMap.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = "Empty",
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.no_classes_available),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(classSectionsMap) { (pair, count) ->
                                val className = pair.first
                                val sectionName = pair.second

                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = "$className - Section $sectionName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = stringResource(R.string.students_registered_count, count),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 14.sp
                                        )
                                    },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Class,
                                                contentDescription = "Class Icon",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = stringResource(R.string.open_details)
                                        )
                                    },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedClassSection = Pair(className, sectionName)
                                            viewModel.initializeDefaultSubjectsForSectionIfNeeded(className, sectionName)
                                        }
                                        .testTag("class_tile_$className" + "_" + sectionName)
                                )
                            }
                    }
                }
            }

            // ADD STUDENT DIALOG
            if (showAddStudentDialog) {
                AddStudentDialog(
                    selectedClassSection = selectedClassSection,
                    onDismiss = { showAddStudentDialog = false },
                    onConfirm = { name, roll, clsName, secName, father, mother, admission, mobile ->
                        viewModel.addStudent(name, roll, clsName, secName, father, mother, admission, mobile)
                        viewModel.initializeDefaultSubjectsForSectionIfNeeded(clsName, secName)
                        showAddStudentDialog = false
                    }
                )
            }

            // EDIT STUDENT DIALOG
            if (showEditStudentDialog && studentToEdit != null) {
                EditStudentDialog(
                    student = studentToEdit!!,
                    onDismiss = { showEditStudentDialog = false },
                    onConfirm = { updatedStudent ->
                        viewModel.updateStudent(updatedStudent)
                        showEditStudentDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun StudentRowCard(
    student: Student,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_row_${student.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Student Icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Column {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Roll No: " + student.rollNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_student_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_student_${student.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddStudentDialog(
    selectedClassSection: Pair<String, String>?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, roll: String, className: String, sectionName: String, fatherName: String, motherName: String, admissionNumber: String, mobileNumber: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var fatherName by remember { mutableStateOf("") }
    var motherName by remember { mutableStateOf("") }
    var admissionNumber by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var className by remember { mutableStateOf(selectedClassSection?.first ?: "Grade 10") }
    var sectionName by remember { mutableStateOf(selectedClassSection?.second ?: "A") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.register_single_student), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_name_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = roll,
                    onValueChange = { roll = it },
                    label = { Text(stringResource(R.string.roll_number_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_roll_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admissionNumber,
                    onValueChange = { admissionNumber = it },
                    label = { Text("Admission Number") },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_admission_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text(stringResource(R.string.father_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_father_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text(stringResource(R.string.mother_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_mother_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_mobile_field"),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )

                // Only edit class if no class context exists
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text(stringResource(R.string.class_name_label)) },
                    placeholder = { Text(stringResource(R.string.class_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_class_field"),
                    singleLine = true,
                    enabled = (selectedClassSection == null)
                )

                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text(stringResource(R.string.class_section_label)) },
                    placeholder = { Text(stringResource(R.string.section_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("add_student_section_field"),
                    singleLine = true,
                    enabled = (selectedClassSection == null)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && roll.isNotEmpty() && className.isNotEmpty() && sectionName.isNotEmpty()) {
                        onConfirm(name, roll, className, sectionName, fatherName, motherName, admissionNumber, mobileNumber)
                    }
                },
                modifier = Modifier.testTag("confirm_add_student_button")
            ) {
                Text(stringResource(R.string.enroll_student))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditStudentDialog(
    student: Student,
    onDismiss: () -> Unit,
    onConfirm: (Student) -> Unit
) {
    var name by remember { mutableStateOf(student.name) }
    var roll by remember { mutableStateOf(student.rollNumber) }
    var fatherName by remember { mutableStateOf(student.fatherName) }
    var motherName by remember { mutableStateOf(student.motherName) }
    var admissionNumber by remember { mutableStateOf(student.admissionNumber ?: "") }
    var mobileNumber by remember { mutableStateOf(student.mobileNumber ?: "") }
    var className by remember { mutableStateOf(student.className) }
    var sectionName by remember { mutableStateOf(student.sectionName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_student_details), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_name_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = roll,
                    onValueChange = { roll = it },
                    label = { Text(stringResource(R.string.roll_number_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_roll_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = admissionNumber,
                    onValueChange = { admissionNumber = it },
                    label = { Text("Admission Number") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_admission_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text(stringResource(R.string.father_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_father_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text(stringResource(R.string.mother_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_mother_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_mobile_field"),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text(stringResource(R.string.class_name_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_class_field"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text(stringResource(R.string.class_section_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_student_section_field"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && roll.isNotEmpty() && className.isNotEmpty() && sectionName.isNotEmpty()) {
                        onConfirm(student.copy(
                            name = name.trim(),
                            rollNumber = roll.trim(),
                            fatherName = fatherName.trim(),
                            motherName = motherName.trim(),
                            admissionNumber = admissionNumber.trim(),
                            mobileNumber = mobileNumber.trim(),
                            className = className.trim(),
                            sectionName = sectionName.trim()
                        ))
                    }
                },
                modifier = Modifier.testTag("confirm_edit_student_button")
            ) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSubjectsView(
    className: String,
    sectionName: String,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val allSectionSubjects by viewModel.allSectionSubjects.collectAsState()
    val sectionSubjects = remember(allSectionSubjects, className, sectionName) {
        allSectionSubjects.filter { it.className == className && it.sectionName == sectionName }
    }

    var showEditDialogForSubject by remember { mutableStateOf<com.example.data.SectionSubject?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Information Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Class,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.subject_register_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.subject_register_desc),
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subject List Scrollable View
            if (sectionSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_subjects_registered),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("seed_subjects_button")
                        ) {
                            Text(stringResource(R.string.add_custom_subject))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sectionSubjects, key = { it.subjectName }) { subject ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = subject.subjectName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.weightage_format, subject.maxMarks),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showEditDialogForSubject = subject },
                                        modifier = Modifier.testTag("edit_subject_${subject.subjectName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_weightage),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { 
                                            viewModel.deleteSectionSubject(className, sectionName, subject.subjectName)
                                        },
                                        modifier = Modifier.testTag("delete_subject_${subject.subjectName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete_subject),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Button to Add New Subjects at the bottom
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 4.dp)
                    .testTag("add_new_subject_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_custom_subject), fontWeight = FontWeight.Bold)
            }
        }

        // Dialog for Adding a Subject
        if (showAddDialog) {
            SubjectInputDialog(
                existingSubject = null,
                onDismiss = { showAddDialog = false },
                onConfirm = { name, max ->
                    viewModel.saveSectionSubject(className, sectionName, name, max)
                    showAddDialog = false
                }
            )
        }

        // Dialog for Editing a Subject
        if (showEditDialogForSubject != null) {
            SubjectInputDialog(
                existingSubject = showEditDialogForSubject,
                onDismiss = { showEditDialogForSubject = null },
                onConfirm = { name, max ->
                    if (showEditDialogForSubject!!.subjectName != name) {
                        viewModel.deleteSectionSubject(className, sectionName, showEditDialogForSubject!!.subjectName)
                    }
                    viewModel.saveSectionSubject(className, sectionName, name, max)
                    showEditDialogForSubject = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectInputDialog(
    existingSubject: com.example.data.SectionSubject?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, maxMarks: Double) -> Unit
) {
    val context = LocalContext.current
    var subjectName by remember { mutableStateOf(existingSubject?.subjectName ?: "") }
    var maxMarksString by remember { mutableStateOf(existingSubject?.maxMarks?.toInt()?.toString() ?: "100") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingSubject == null) stringResource(R.string.add_custom_subject) else stringResource(R.string.edit_subject_details),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { 
                        subjectName = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.subject_name_label)) },
                    placeholder = { Text(stringResource(R.string.subject_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("subject_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = maxMarksString,
                    onValueChange = { 
                        maxMarksString = it 
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.max_marks_label)) },
                    placeholder = { Text(stringResource(R.string.max_marks_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("subject_max_marks_input"),
                    singleLine = true
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = subjectName.trim()
                    val maxVal = maxMarksString.trim().toDoubleOrNull()
                    if (cleanName.isEmpty()) {
                        errorText = context.getString(R.string.error_subject_name_empty)
                    } else if (maxVal == null || maxVal <= 0.0) {
                        errorText = context.getString(R.string.error_weightage_positive)
                    } else {
                        onConfirm(cleanName, maxVal)
                    }
                },
                modifier = Modifier.testTag("subject_dialog_confirm")
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("subject_dialog_dismiss")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
