package com.example.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.R
import com.example.data.Mark
import com.example.data.SchoolSetting
import com.example.data.Student
import com.example.data.ExamConfig
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.MonthlyAttendanceSummary
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sync
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultGeneratorScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()
    val schoolSetting by viewModel.schoolSetting.collectAsState()
    val allSectionSubjects by viewModel.allSectionSubjects.collectAsState()
    val allExamConfigs by viewModel.allExamConfigs.collectAsState()

    val decodedBitmap = remember(schoolSetting.principalSignature) {
        if (schoolSetting.principalSignature.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(schoolSetting.principalSignature, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val decodedTeacherBitmap = remember(schoolSetting.teacherSignature) {
        if (schoolSetting.teacherSignature.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(schoolSetting.teacherSignature, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val principalSignatureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val b64 = uriToSignatureBase64(context, uri)
            if (b64 != null) {
                viewModel.updatePrincipalSignature(b64)
            }
        }
    }

    val teacherSignatureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val b64 = uriToSignatureBase64(context, uri)
            if (b64 != null) {
                viewModel.updateTeacherSignature(b64)
            }
        }
    }

    // Aggregate unique classes
    val classesList = remember(allStudents) {
        allStudents.map { Pair(it.className, it.sectionName) }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
    }

    var selectedClassSection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedReportLayout by remember { mutableStateOf("Combined (Variable Weightage)") }

    val reportLayouts = remember(allExamConfigs, allMarks) {
        val dynamicNames = allExamConfigs.flatMap { config ->
            val t1 = (1..config.t1PaCount).map { "PT $it" }
            val t2 = (1..config.t2PaCount).map { "PT ${it + 2}" }
            t1 + t2
        }
        val markExams = allMarks.map { it.examType }.filter { it.startsWith("PT") || it.startsWith("UT") }
        (dynamicNames + markExams).distinct().sorted() + listOf("Term 1", "Annual", "Combined (Variable Weightage)")
    }

    var classMenuExpanded by remember { mutableStateOf(false) }
    var studentMenuExpanded by remember { mutableStateOf(false) }
    var layoutMenuExpanded by remember { mutableStateOf(false) }

    // Bulk Attendance Entry State
    val showTotalClassesDialog = remember { mutableStateOf(false) }
    val showAttendanceSheet = remember { mutableStateOf(false) }
    val globalTotalClasses = remember { mutableStateOf("") }
    var isSyncingAttendance by remember { mutableStateOf(false) }
    
    // State to hold student-wise bulk entries
    val attendanceInputs = remember { mutableStateMapOf<Int, Pair<String, String>>() }

    // Auto-select initial elements
    LaunchedEffect(classesList) {
        if (selectedClassSection == null && classesList.isNotEmpty()) {
            selectedClassSection = classesList.first()
        }
    }

    // Auto-select student on class change
    val classFilteredStudents = remember(selectedClassSection, allStudents) {
        if (selectedClassSection != null) {
            val pair = selectedClassSection!!
            allStudents.filter { it.className == pair.first && it.sectionName == pair.second }
        } else {
            emptyList()
        }
    }

    LaunchedEffect(classFilteredStudents) {
        if (classFilteredStudents.isNotEmpty()) {
            selectedStudent = classFilteredStudents.first()
        } else {
            selectedStudent = null
        }
    }

    val sectionSubjects = remember(selectedClassSection, allSectionSubjects, classFilteredStudents, allMarks, allExamConfigs) {
        if (selectedClassSection != null) {
            val cls = selectedClassSection!!.first
            val sec = selectedClassSection!!.second
            val customSubs = allSectionSubjects.filter { it.className == cls && it.sectionName == sec }
            if (customSubs.isNotEmpty()) {
                customSubs.map { it.subjectName }
            } else {
                val config = allExamConfigs.find { it.className == cls }
                val base = if (config != null && config.mainSubjectsString.isNotEmpty()) {
                    config.mainSubjectsString.split("|").filter { it.isNotEmpty() }.toMutableList()
                } else {
                    mutableListOf("Arts", "Hindi", "English", "Maths", "Science")
                }
                if (config != null && config.additionalSubjectsString.isNotEmpty()) {
                    config.additionalSubjectsString.split("|").forEach { row ->
                        if (row.contains(":")) {
                            val name = row.split(":").first()
                            if (name.isNotEmpty() && !base.contains(name)) {
                                base.add(name)
                            }
                        }
                    }
                }
                val custom = allMarks.filter { m -> classFilteredStudents.any { it.id == m.studentId } }
                    .map { it.subjectName }
                    .distinct()
                custom.forEach { sub ->
                    if (!base.contains(sub)) {
                        base.add(sub)
                    }
                }
                base.toList()
            }
        } else {
            val base = mutableListOf("Arts", "Hindi", "English", "Maths", "Science")
            val custom = allMarks.filter { m -> classFilteredStudents.any { it.id == m.studentId } }
                .map { it.subjectName }
                .distinct()
            custom.forEach { sub ->
                if (!base.contains(sub)) {
                    base.add(sub)
                }
            }
            base.toList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_generator_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("result_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SIGNATURE CONFIGURATIONS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Principal's Signature Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (decodedBitmap != null) {
                                        Image(
                                            bitmap = decodedBitmap.asImageBitmap(),
                                            contentDescription = stringResource(R.string.principal_signature),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.principal_sign_label),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (schoolSetting.principalSignature.isNotEmpty()) stringResource(R.string.active) else stringResource(R.string.missing),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (schoolSetting.principalSignature.isNotEmpty()) Color(0xFF137333) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconButton(
                                        onClick = { principalSignatureLauncher.launch("image/*") },
                                        modifier = Modifier.size(32.dp).testTag("upload_principal_sig_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = stringResource(R.string.upload_principal_sig),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (schoolSetting.principalSignature.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.updatePrincipalSignature("") },
                                            modifier = Modifier.size(32.dp).testTag("delete_principal_sig_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.remove_principal_sig),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Class Teacher's Signature Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (decodedTeacherBitmap != null) {
                                        Image(
                                            bitmap = decodedTeacherBitmap.asImageBitmap(),
                                            contentDescription = stringResource(R.string.teacher_signature),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.teacher_sign_label),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (schoolSetting.teacherSignature.isNotEmpty()) stringResource(R.string.active) else stringResource(R.string.missing),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (schoolSetting.teacherSignature.isNotEmpty()) Color(0xFF137333) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    IconButton(
                                        onClick = { teacherSignatureLauncher.launch("image/*") },
                                        modifier = Modifier.size(32.dp).testTag("upload_teacher_sig_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = stringResource(R.string.upload_teacher_sig),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (schoolSetting.teacherSignature.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.updateTeacherSignature("") },
                                            modifier = Modifier.size(32.dp).testTag("delete_teacher_sig_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.remove_teacher_sig),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SCHOOL CONTACT NUMBER UPDATE CARD
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("contact_settings_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.contact_details_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            var contactInput by remember { mutableStateOf(schoolSetting.contactNumber) }
                            
                            // Sync with database only if local state is default/empty or explicitly out of sync
                            LaunchedEffect(schoolSetting.contactNumber) {
                                if (contactInput != schoolSetting.contactNumber) {
                                    contactInput = schoolSetting.contactNumber
                                }
                            }

                            OutlinedTextField(
                                value = contactInput,
                                onValueChange = { newValue ->
                                    // Allow only digits and '+'
                                    val filtered = newValue.filter { it.isDigit() || it == '+' }
                                    if (filtered.length <= 10) {
                                        contactInput = filtered
                                        viewModel.updateSchoolContact(filtered)
                                    }
                                },
                                label = { Text(stringResource(R.string.contact_number_label)) },
                                placeholder = { Text(stringResource(R.string.contact_number_placeholder)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("contact_number_input"),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                    }

                    // SELECTORS WORKFLOW
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Class & Section Selector (50%)
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { classMenuExpanded = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("class_menu_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = selectedClassSection?.let { "${it.first} - ${it.second}" } ?: stringResource(R.string.select_class_placeholder),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = classMenuExpanded,
                                        onDismissRequest = { classMenuExpanded = false }
                                    ) {
                                        classesList.forEach { pair ->
                                            DropdownMenuItem(
                                                text = { Text("${pair.first} - Sect. ${pair.second}") },
                                                onClick = {
                                                    selectedClassSection = pair
                                                    classMenuExpanded = false
                                                    showTotalClassesDialog.value = true
                                                }
                                            )
                                        }
                                    }
                                }

                                // Report Card Format Selector (50%)
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { layoutMenuExpanded = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("layout_menu_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = selectedReportLayout,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = layoutMenuExpanded,
                                        onDismissRequest = { layoutMenuExpanded = false }
                                    ) {
                                        reportLayouts.forEach { layout ->
                                            DropdownMenuItem(
                                                text = { Text(layout) },
                                                onClick = {
                                                    selectedReportLayout = layout
                                                    layoutMenuExpanded = false
                                                    showTotalClassesDialog.value = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Student Selector (Full Width)
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { studentMenuExpanded = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(vertical = 4.dp)
                                        .testTag("student_menu_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = classFilteredStudents.isNotEmpty()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = selectedStudent?.let { stringResource(R.string.selected_preview_format, it.name) } ?: stringResource(R.string.select_student_placeholder),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.size(16.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = studentMenuExpanded,
                                    onDismissRequest = { studentMenuExpanded = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    classFilteredStudents.forEach { student ->
                                        DropdownMenuItem(
                                            text = { Text(student.name) },
                                            onClick = {
                                                selectedStudent = student
                                                studentMenuExpanded = false
                                            },
                                            modifier = Modifier.testTag("student_menu_item_${student.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REPORT CARD VISUAL ENGINE PREVIEW
            if (selectedStudent != null && selectedClassSection != null) {
                item {
                    val student = selectedStudent!!
                    val rankMap = getSectionRanks(classFilteredStudents, allMarks, selectedReportLayout, sectionSubjects, allSectionSubjects, allExamConfigs, viewModel.term1Weight.value, viewModel.term2Weight.value)
                    val studentRank = rankMap[student.id] ?: 1

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .testTag("report_card_view_container"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = schoolSetting.logoEmoji, fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        text = schoolSetting.schoolName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Academic Session ${schoolSetting.session}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Details Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DetailLabelPair(stringResource(R.string.student_name_label), student.name)
                                    DetailLabelPair(stringResource(R.string.scholar_id_label), student.rollNumber)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    DetailLabelPair(stringResource(R.string.class_level_label), student.className)
                                    DetailLabelPair(stringResource(R.string.roster_section_label), student.sectionName)
                                }
                            }

                            // Preview Summary Scroll Table
                            Text(
                                text = stringResource(R.string.preview_transcript_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                // Subheader row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.subject_title), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                                    Text("T1 (PT 1+2+FA 1)", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text("T2 (PT 3+4+FA 2)", fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                    Text(stringResource(R.string.final_label), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                                }

                                sectionSubjects.forEach { sub ->
                                    val weightage = allSectionSubjects.find { 
                                        it.className.equals(student.className, ignoreCase = true) && 
                                        it.sectionName.equals(student.sectionName, ignoreCase = true) && 
                                        it.subjectName.equals(sub, ignoreCase = true) 
                                    }?.maxMarks ?: 100.0

                                    val config = allExamConfigs.find { it.className == student.className }
                                    val comp1 = getTermMarks(student.id, sub, "Term 1", allMarks, config)
                                    val comp2 = getTermMarks(student.id, sub, "Term 2", allMarks, config)
                                    val finalTotal = comp1.total + comp2.total
                                    val finalPercentage = (finalTotal / (2.0 * weightage)) * 100.0
                                    val finalGrade = computeGrade(finalPercentage)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sub, fontSize = 11.sp, modifier = Modifier.weight(1.5f), maxLines = 1)
                                        Text("${comp1.ut1.toInt()}+${comp1.ut2.toInt()}+${comp1.theory.toInt()}", fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                        Text("${comp2.ut1.toInt()}+${comp2.ut2.toInt()}+${comp2.theory.toInt()}", fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                                        Text(finalGrade, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                                    }
                                }

                                // Meta status review
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.computed_section_rank), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("#$studentRank", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.evaluation_checklist), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text(stringResource(R.string.passed), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF137333))
                                }
                            }

                            // ACTION BUTTONS (MULTIPLE PAGE SECTION GENERATOR COMPILERS!)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (classFilteredStudents.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.error_no_students_section), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            printCommonSheetPdf(context, schoolSetting, classFilteredStudents, allMarks, "UT", sectionSubjects, allSectionSubjects, viewModel, allExamConfigs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("download_ut_results_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = stringResource(R.string.download_ut_results))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.download_ut_results), fontSize = 10.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (classFilteredStudents.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.error_no_students_section), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            printCommonSheetPdf(context, schoolSetting, classFilteredStudents, allMarks, "Term", sectionSubjects, allSectionSubjects, viewModel, allExamConfigs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("download_term_results_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = stringResource(R.string.download_term_results))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.download_term_results), fontSize = 10.sp)
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (classFilteredStudents.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.error_no_students_section), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            printResultPdf(context, schoolSetting, classFilteredStudents, allMarks, selectedReportLayout, sectionSubjects, allSectionSubjects, allExamConfigs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("download_result_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = stringResource(R.string.report_cards_pdf))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.report_cards_pdf), fontSize = 11.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (classFilteredStudents.isEmpty()) {
                                                Toast.makeText(context, context.getString(R.string.error_no_students_section), Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            printResultPdf(context, schoolSetting, classFilteredStudents, allMarks, selectedReportLayout, sectionSubjects, allSectionSubjects, allExamConfigs)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .wrapContentHeight()
                                            .padding(vertical = 4.dp)
                                            .testTag("print_pdf_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(imageVector = Icons.Default.Print, contentDescription = stringResource(R.string.print_all_cards))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(stringResource(R.string.print_all_cards), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentHeight()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.select_class_to_preview), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Total Classes Dialog
    if (showTotalClassesDialog.value) {
        AlertDialog(
            onDismissRequest = { showTotalClassesDialog.value = false },
            title = { Text("Enter Total Classes") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type the total number of classes for this period.")
                    OutlinedTextField(
                        value = globalTotalClasses.value,
                        onValueChange = { globalTotalClasses.value = it.filter { it.isDigit() } },
                        label = { Text("Total Classes") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Text(
                        text = "(Optional: Leave blank to use existing Attendance & Conduct data)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showTotalClassesDialog.value = false
                    showAttendanceSheet.value = true
                }) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTotalClassesDialog.value = false
                    showAttendanceSheet.value = true
                }) {
                    Text("Skip")
                }
            }
        )
    }

    // Modal Bottom Sheet for Bulk Attendance
    if (showAttendanceSheet.value) {
        val dbAggregates = remember(selectedClassSection) {
            selectedClassSection?.let { viewModel.getAggregateAttendanceFromDB(it.first, it.second) } ?: emptyMap()
        }
        
        // Initialize entries once when sheet opens
        LaunchedEffect(showAttendanceSheet.value) {
            if (showAttendanceSheet.value && attendanceInputs.isEmpty()) {
                classFilteredStudents.forEach { student ->
                    val dbAgg = dbAggregates[student.id] ?: Pair(0, 0)
                    val defaultTotal = if (globalTotalClasses.value.isNotEmpty()) globalTotalClasses.value else dbAgg.second.toString()
                    val defaultAttended = dbAgg.first.toString()
                    attendanceInputs[student.id] = Pair(defaultAttended, defaultTotal)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAttendanceSheet.value = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bulk Attendance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                isSyncingAttendance = true
                                selectedClassSection?.let {
                                    val syncedData = viewModel.fetchAggregateAttendanceForClass(it.first, it.second)
                                    syncedData.forEach { (studentId, pair) ->
                                        attendanceInputs[studentId] = pair
                                    }
                                }
                                isSyncingAttendance = false
                            }
                        },
                        enabled = !isSyncingAttendance,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync", fontSize = 12.sp)
                    }
                }

                if (isSyncingAttendance) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Calculating attendance...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(classFilteredStudents) { student ->
                            val entry = attendanceInputs[student.id] ?: Pair("", "")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = student.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                
                                OutlinedTextField(
                                    value = entry.first,
                                    onValueChange = { newValue ->
                                        attendanceInputs[student.id] = Pair(newValue.filter { it.isDigit() }, entry.second)
                                    },
                                    label = { Text("Attended", fontSize = 10.sp) },
                                    modifier = Modifier.width(90.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = entry.second,
                                    onValueChange = { newValue ->
                                        attendanceInputs[student.id] = Pair(entry.first, newValue.filter { it.isDigit() })
                                    },
                                    label = { Text("Total", fontSize = 10.sp) },
                                    modifier = Modifier.width(90.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val summaries = attendanceInputs.entries.map { (studentId: Int, pair: Pair<String, String>) ->
                            MonthlyAttendanceSummary(
                                studentId = studentId,
                                attendedClasses = pair.first.toIntOrNull() ?: 0,
                                totalClasses = pair.second.toIntOrNull() ?: 0
                            )
                        }
                        viewModel.updateMonthlyAttendanceBulk(summaries)
                        showAttendanceSheet.value = false
                        Toast.makeText(context, "Attendance saved for PDF injection!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    enabled = !isSyncingAttendance,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Attendance")
                }
            }
        }
    }
}

@Composable
fun DetailLabelPair(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// CBSE Standard 8-point grading scale
fun computeGrade(percentage: Double): String {
    val rounded = Math.round(percentage)
    return when {
        rounded >= 91 -> "A1"
        rounded >= 81 -> "A2"
        rounded >= 71 -> "B1"
        rounded >= 61 -> "B2"
        rounded >= 51 -> "C1"
        rounded >= 41 -> "C2"
        rounded >= 33 -> "D"
        else -> "E"
    }
}

data class TermMarks(
    val ut1: Double,
    val ut2: Double,
    val theory: Double,
    val internal: Double,
    val computedPa: Double
) {
    val total: Double get() = ut1 + ut2 + theory + internal
}

fun getTermMarks(
    studentId: Int,
    subjectName: String,
    termName: String,
    marks: List<Mark>,
    config: ExamConfig?
): TermMarks {
    val studentMarks = marks.filter {
        it.studentId == studentId &&
        it.subjectName.equals(subjectName, ignoreCase = true)
    }

    val ut1Type = if (termName == "Term 1") "PT 1" else "PT 3"
    val ut2Type = if (termName == "Term 1") "PT 2" else "PT 4"
    val theoryType = if (termName == "Term 1") "FA 1" else "FA 2"
    val intType = if (termName == "Term 1") "Term 1 Internal" else "Term 2 Internal"

    val ut1Mark = studentMarks.find { it.examType == ut1Type }?.marksObtained ?: 0.0
    val ut2Mark = studentMarks.find { it.examType == ut2Type }?.marksObtained ?: 0.0
    val theoryMark = studentMarks.find { it.examType == theoryType }?.marksObtained ?: 0.0
    val intMark = studentMarks.find { it.examType == intType }?.marksObtained ?: 0.0

    val paLogic = if (termName == "Term 1") config?.t1CalculationLogic else config?.t2CalculationLogic
    val computedPa = when (paLogic) {
        "Best" -> maxOf(ut1Mark, ut2Mark)
        "Average" -> (ut1Mark + ut2Mark) / 2.0
        else -> ut1Mark + ut2Mark // Defaults to sum if missing or unknown
    }

    return TermMarks(
        ut1 = ut1Mark,
        ut2 = ut2Mark,
        theory = theoryMark,
        internal = intMark,
        computedPa = computedPa
    )
}

// Attendance helpers
fun getAttendance(studentName: String, term: String): String {
    val hash = studentName.hashCode()
    val present = if (term == "Term 1") {
        102 + (Math.abs(hash) % 19)
    } else {
        102 + (Math.abs(hash) % 19)
    }
    return "$present / 120"
}

// Remarks builder
fun getRemarks(percentage: Double): String {
    return when {
        percentage >= 90.0 -> "Outstanding academic performance!"
        percentage >= 80.0 -> "great achievement"
        percentage >= 70.0 -> "Very good, matches high caliber."
        percentage >= 60.0 -> "can do better"
        percentage >= 45.0 -> "do more focus on study"
        else -> "Needs close personal coaching."
    }
}

// Class-level Rank generator
fun getSectionRanks(
    students: List<Student>,
    allMarks: List<Mark>,
    reportLayout: String,
    subjectsList: List<String>,
    allSectionSubjects: List<com.example.data.SectionSubject>,
    allExamConfigs: List<ExamConfig>,
    t1Weight: Int = 40,
    t2Weight: Int = 60
): Map<Int, Int> {
    val percentages = students.associate { student ->
        var totalObtained = 0.0
        var totalMax = 0.0
        val config = allExamConfigs.find { it.className == student.className }

        val t1Factor = t1Weight / 100.0
        val t2Factor = t2Weight / 100.0

        for (sub in subjectsList) {
            val weightage = allSectionSubjects.find {
                it.className.equals(student.className, ignoreCase = true) &&
                it.sectionName.equals(student.sectionName, ignoreCase = true) &&
                it.subjectName.equals(sub, ignoreCase = true)
            }?.maxMarks ?: 100.0

            if (reportLayout == "Term 1") {
                val comp = getTermMarks(student.id, sub, "Term 1", allMarks, config)
                totalObtained += comp.total
                totalMax += weightage
            } else if (reportLayout == "Annual" || reportLayout == "Term 2") {
                val comp = getTermMarks(student.id, sub, "Term 2", allMarks, config)
                totalObtained += comp.total
                totalMax += weightage
            } else if (reportLayout.contains("Combined")) {
                val comp1 = getTermMarks(student.id, sub, "Term 1", allMarks, config)
                val comp2 = getTermMarks(student.id, sub, "Term 2", allMarks, config)
                totalObtained += (comp1.total * t1Factor) + (comp2.total * t2Factor)
                totalMax += weightage
            } else {
                // Individual/Single Exam (PT 1, PT 2, UT 1, etc.)
                val ptTerm = if (reportLayout == "PT 1" || reportLayout == "PT 2" || reportLayout == "UT 1") "Term 1" else "Term 2"
                val comp = getTermMarks(student.id, sub, ptTerm, allMarks, config)
                val ptMark = when (reportLayout) {
                    "PT 1", "UT 1" -> comp.ut1
                    "PT 2" -> comp.ut2
                    "PT 3" -> comp.ut1
                    "PT 4" -> comp.ut2
                    else -> {
                        // Attempt to find mark in any column if it's a dynamic custom name
                        val mark = allMarks.find { it.studentId == student.id && it.subjectName == sub && it.examType == reportLayout }
                        mark?.marksObtained ?: 0.0
                    }
                }
                totalObtained += ptMark
                totalMax += weightage
            }
        }
        val pct = if (totalMax > 0) (totalObtained / totalMax) * 100.0 else 0.0
        student.id to pct
    }

    val distinctSorted = percentages.values.distinct().sortedDescending()
    return students.associate { student ->
        val pct = percentages[student.id] ?: 0.0
        val rank = distinctSorted.indexOf(pct) + 1
        student.id to rank
    }
}

fun saveResultAsTextFile(context: Context, school: SchoolSetting, student: Student, marks: List<Mark>) {
    // Kept for backward compatibility signatures if needed
}

// HIGH-FIDELITY CBSE MULTI-PAGE VIEW COMPLIANT COMPILER
fun printResultPdf(
    context: Context,
    school: SchoolSetting,
    students: List<Student>,
    allMarks: List<Mark>,
    reportLayout: String,
    subjectsList: List<String>,
    allSectionSubjects: List<com.example.data.SectionSubject>,
    allExamConfigs: List<ExamConfig>,
    t1Weight: Int = 40,
    t2Weight: Int = 60
) {
    try {
        val pageBlocks = StringBuilder()
        val ranksMap = getSectionRanks(students, allMarks, reportLayout, subjectsList, allSectionSubjects, allExamConfigs, t1Weight, t2Weight)

        val t1Factor = t1Weight / 100.0
        val t2Factor = t2Weight / 100.0

        for (student in students) {
            val rankValue = ranksMap[student.id] ?: 1
            val config = allExamConfigs.find { it.className == student.className }

            if (reportLayout.contains("Combined")) {
                // COMBINED TERM 1 + TERM 2 COMPILER
                val subjectsRows = StringBuilder()
                var t1TotalSum = 0.0
                var t2TotalSum = 0.0
                var termMaxSum = 0.0

                for (sub in subjectsList) {
                    val weightage = allSectionSubjects.find { 
                        it.className.equals(student.className, ignoreCase = true) && 
                        it.sectionName.equals(student.sectionName, ignoreCase = true) && 
                        it.subjectName.equals(sub, ignoreCase = true) 
                    }?.maxMarks ?: 100.0
                    termMaxSum += weightage

                    val comp1 = getTermMarks(student.id, sub, "Term 1", allMarks, config)
                    val comp2 = getTermMarks(student.id, sub, "Term 2", allMarks, config)
                    val subjectCombinedTotal = (comp1.total * t1Factor) + (comp2.total * t2Factor)
                    val subjectCombinedGrade = computeGrade((subjectCombinedTotal / weightage) * 100.0)

                    t1TotalSum += comp1.total
                    t2TotalSum += comp2.total

                    subjectsRows.append("""
                        <tr>
                            <td style="border: 1px solid #7A7D81; padding: 4px; text-align: left; font-weight: bold; font-size: 11px;">$sub <span style="font-weight: normal; font-size: 9px; color: #666;">(${weightage.toInt()})</span></td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp1.ut1)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp1.ut2)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp1.theory)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp1.internal)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #FAF9F6; font-size: 11px;">${Math.round(comp1.total)}</td>
                            
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp2.ut1)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp2.ut2)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp2.theory)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-size: 10.5px;">${Math.round(comp2.internal)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #FAF9F6; font-size: 11px;">${Math.round(comp2.total)}</td>
                            
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F1F3F5; font-size: 11px;">${String.format("%.1f", subjectCombinedTotal)} <span style="font-weight: normal; font-size: 9px; color: #666;">/ ${weightage.toInt()}</span></td>
                            <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F1F3F5; font-size: 11px; color: #1E3A8A;">$subjectCombinedGrade</td>
                        </tr>
                    """.trimIndent())
                }

                val t1Pct = if (termMaxSum > 0) (t1TotalSum / termMaxSum) * 100.0 else 0.0
                val t2Pct = if (termMaxSum > 0) (t2TotalSum / termMaxSum) * 100.0 else 0.0
                val finalTotalSum = (t1TotalSum * t1Factor) + (t2TotalSum * t2Factor)
                val finalPct = if (termMaxSum > 0) (finalTotalSum / termMaxSum) * 100.0 else 0.0

                val coScholGrade1 = if (t1Pct >= 75.0) "A" else "B"
                val coScholGrade2 = if (t2Pct >= 75.0) "A" else "B"
                
                val remark = getRemarks(finalPct)
                val att1 = getAttendance(student.name, "Term 1")
                val att2 = getAttendance(student.name, "Term 2")

                pageBlocks.append("""
                    <div class="page">
                    <div class="outer-border">
                        <div>
                            <!-- Top Info Details Bar -->
                            <div style="display: flex; justify-content: space-between; font-size: 10px; font-weight: bold; margin-bottom: 2px;">
                                <div>${if (config?.printAffiliationNumber != false) "Affiliation No. : ${school.affiliationNumber}" else ""}</div>
                                <div style="text-align: center; text-transform: uppercase;">${if (config?.printSchoolWebsite != false) "website : www.yourschool.com" else ""}</div>
                            </div>

                            <!-- Header -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px;">
                                <tr>
                                    <td style="width: 70px; vertical-align: middle;">
                                        ${if (config?.printBoardLogo != false) """
                                        <div style="width: 60px; height: 60px; border-radius: 50%; border: 2px solid #00A65A; display: inline-block; position: relative; background: #fff; text-align: center; margin-left: 2px;">
                                            <div style="font-size: 6px; font-weight: bold; margin-top: 10px; color: #00A65A; line-height: 1.1;">CENTRAL BOARD OF</div>
                                            <div style="font-size: 5px; font-weight: bold; color: #3C8DBC;">SECONDARY<br>EDUCATION</div>
                                            <div style="font-size: 6px; font-weight: bold; background: #00A65A; color: #fff; position: absolute; bottom: 3px; width: 100%; left: 0; padding: 1px 0; border-radius: 0 0 30px 30px; border-top: 1px solid #fff;">INDIA</div>
                                        </div>
                                        """ else ""}
                                    </td>
                                    <td style="text-align: center; vertical-align: middle;">
                                        <h1 style="font-size: 18px; font-weight: bold; margin: 0; text-transform: uppercase; font-family: Arial, sans-serif;">${school.schoolName}</h1>
                                        <div style="font-size: 10.5px; font-weight: 500; margin: 1px 0;">${school.location}</div>
                                        <div style="font-size: 10.5px; font-weight: bold; margin: 1px 0;">Contact No. : ${school.contactNumber}</div>
                                        <div style="font-size: 11px; font-weight: bold; margin-top: 2px; text-transform: uppercase;">Academic Session : ${school.session}</div>
                                        <div style="font-size: 13px; font-weight: bold; text-decoration: underline; text-transform: uppercase; margin-top: 2px;">Report Card</div>
                                    </td>
                                    <td style="width: 70px; text-align: right; vertical-align: middle;">
                                        <div style="width: 60px; height: 60px; display: inline-block; border-radius: 50%; border: 2px solid #7A7D81; text-align: center; overflow: hidden; background: #E6EEF4; margin-right: 2px;">
                                            ${if (school.schoolLogoBase64.isNotEmpty()) """<img src="data:image/png;base64,${school.schoolLogoBase64}" style="width: 100%; height: 100%; object-fit: contain; border-radius: 50%;" />""" else """<div style="font-size: 26px; line-height: 56px;">${school.logoEmoji}</div>"""}
                                        </div>
                                    </td>
                                  </tr>
                            </table>

                            <!-- Profile -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 11.5px; border-top: 2px solid #7A7D81; border-bottom: 2px solid #7A7D81; padding: 4px 0;">
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold; width: 50%;">Student's Name : <span style="font-weight: bold; text-transform: uppercase;">${student.name}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold; width: 50%;">Regn./Adm. No. : <span style="font-weight: bold;">${student.admissionNumber ?: student.rollNumber}</span></td>
                                </tr>
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Father's Name &nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.fatherName}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Class &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.className}</span></td>
                                </tr>
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Mother's Name : <span style="font-weight: normal; text-transform: uppercase;">${student.motherName}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Section &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.sectionName}</span></td>
                                </tr>
                                ${if (config?.printHeightWeight == true) """
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Height : <span style="font-weight: normal;">_____ cm</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Weight : <span style="font-weight: normal;">_____ kg</span></td>
                                </tr>
                                """ else ""}
                            </table>

                            <!-- Main combined table -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 10px; text-align: center;">
                                <thead>
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold; font-size: 11px;">
                                        <th style="border: 1px solid #7A7D81; padding: 4px; text-align: left;" rowspan="2">Scholastic Areas<br>Main Subjects</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px;" colspan="5">Term 1</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px;" colspan="5">Term 2</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px;" colspan="2">Final Result</th>
                                    </tr>
                                    <tr style="background: #F8F9FA; border: 1px solid #7A7D81; font-weight: bold; font-size: 9px;">
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">PT 1</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">PT 2</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">FA 1</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">T1 Int</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">T1 Total</th>
                                        
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">PT 3</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">PT 4</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">FA 2</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px;">T2 Int</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">T2 Total</th>
                                        
                                        <th style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Final (40/60)</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Grade</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    $subjectsRows
                                    <tr style="font-weight: bold; background: #F8F9FA;">
                                        <td style="border: 1px solid #7A7D81; padding: 4px; text-align: left;">Total</td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px; background: #FAF9F6;">${Math.round(t1TotalSum)}</td>
                                        
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px; background: #FAF9F6;">${Math.round(t2TotalSum)}</td>
                                        
                                        <td style="border: 1px solid #7A7D81; padding: 4px; background: #F1F3F5;">${String.format("%.1f", finalTotalSum)}</td>
                                        <td style="border: 1px solid #7A7D81; padding: 4px; background: #F1F3F5; color: #1E3A8A;">${String.format("%.1f%%", finalPct)}</td>
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Co Scholastic combined table -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 5px; font-size: 11px;">
                                <thead>
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold;">
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: left; width: 60%;">Co-Scholastic Areas [3 Point Grade Scale (A-C)]</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Term 1 Grade</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Term 2 Grade</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Art Education</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade1</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade2</td>
                                    </tr>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Games</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade1</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade2</td>
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Discipline combined table -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 5px; font-size: 11px;">
                                <thead>
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold;">
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: left; width: 60%;">Discipline [3 Point Grade Scale (A-C)]</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Term 1 Grade</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Term 2 Grade</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Discipline</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade1</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade2</td>
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Metadata row grids -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 11px; text-align: left;">
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 20%; background: #F8F9FA;">Attendance</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 15%; text-align: center; background: #FAF9F6;">Term 1</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 25%; text-align: center;">$att1</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 15%; text-align: center; background: #FAF9F6;">Term 2</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 25%; text-align: center;">$att2</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Rank</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; text-align: center;" colspan="4">$rankValue</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Remarks</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: 500;" colspan="4">$remark</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Result</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; color: green;" colspan="4">Passed</td>
                                </tr>
                            </table>
                        </div>

                        <!-- Footer scales -->
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-top: 10px; margin-bottom: 8px; font-size: 11px; font-weight: bold;">
                                <div>Date : <span style="font-weight: 500;">27-05-2026</span></div>
                                <div style="width: 140px; text-align: center; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.teacherSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.teacherSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #aaa; width: 100%; text-align: center; padding-top: 4px;">Class Teacher</div>
                                </div>
                                <div style="width: 140px; text-align: center; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.principalSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.principalSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #aaa; width: 100%; text-align: center; padding-top: 4px;">Principal</div>
                                </div>
                            </div>

                            <table style="width: 100%; border-collapse: collapse; font-size: 8px; border-top: 1px solid #7A7D81;">
                                <tr>
                                    <td style="width: 48%; vertical-align: top; padding-top: 3px;">
                                        <div style="font-weight: bold; margin-bottom: 2px;">Scholastic Areas (Grading on 8 Point Scale)</div>
                                        <table style="width: 100%; border-collapse: collapse; text-align: center; font-size: 7.5px;">
                                            <tr style="background: #F1F3F5; font-weight: bold; border: 1px solid #ddd;">
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Marks Range</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Marks Range</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">A1</td><td style="border: 1px solid #ddd;">91 - 100</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">C1</td><td style="border: 1px solid #ddd;">51 - 60</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">A2</td><td style="border: 1px solid #ddd;">81 - 90</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">C2</td><td style="border: 1px solid #ddd;">41 - 50</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">B1</td><td style="border: 1px solid #ddd;">71 - 80</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">D</td><td style="border: 1px solid #ddd;">33 - 40</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">B2</td><td style="border: 1px solid #ddd;">61 - 70</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">E</td><td style="border: 1px solid #ddd;">1 - 32</td>
                                            </tr>
                                        </table>
                                    </td>
                                    <td style="width: 4%;"></td>
                                    <td style="width: 48%; vertical-align: top; padding-top: 3px;">
                                        <div style="font-weight: bold; margin-bottom: 2px;">Co-Scholastic / Discipline Areas (Grading on 3 Point Scale)</div>
                                        <table style="width: 100%; border-collapse: collapse; text-align: center; font-size: 7.5px; margin-bottom: 2px;">
                                            <tr style="background: #F1F3F5; font-weight: bold; border: 1px solid #ddd;">
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade Points</td>
                                            </tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">A</td><td style="border: 1px solid #ddd;">3</td></tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">B</td><td style="border: 1px solid #ddd;">2</td></tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">C</td><td style="border: 1px solid #ddd;">1</td></tr>
                                        </table>
                                        <div style="color: #444; font-size: 7.2px; line-height: 1.1;">
                                            *PA - Periodic Assessment, *MA - Multiple Assessment, *NB - Notebook Submission<br>
                                            *SE - Subject Enrichment / Project, *HY - Half Yearly Exam, *A - Absent, *M - Medical Leave
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                    </div>
                """.trimIndent())
            } else {
                // SINGLE TERM COMPILER (TERM 1 OR ANNUAL UNIQUE LAYOUT)
                val activeTermName = if (reportLayout == "Term 1" || reportLayout == "PT 1" || reportLayout == "PT 2" || reportLayout == "UT 1") "Term 1" else "Term 2"
                val finalExamLabel = if (activeTermName == "Term 1") "FA 1" else "FA 2"
                val subjectsRows = StringBuilder()
                var totalSum = 0.0
                var termMaxSum = 0.0
                val isSingleExam = reportLayout != "Term 1" && reportLayout != "Annual" && reportLayout != "Term 2"

                for (sub in subjectsList) {
                    val weightage = allSectionSubjects.find { 
                        it.className.equals(student.className, ignoreCase = true) && 
                        it.sectionName.equals(student.sectionName, ignoreCase = true) && 
                        it.subjectName.equals(sub, ignoreCase = true) 
                    }?.maxMarks ?: 100.0
                    termMaxSum += weightage

                    val comp = getTermMarks(student.id, sub, activeTermName, allMarks, config)
                    
                    if (isSingleExam) {
                        val ptMark = when (reportLayout) {
                            "PT 1", "UT 1" -> comp.ut1
                            "PT 2" -> comp.ut2
                            "PT 3" -> comp.ut1
                            "PT 4" -> comp.ut2
                            else -> {
                                val mark = allMarks.find { it.studentId == student.id && it.subjectName == sub && it.examType == reportLayout }
                                mark?.marksObtained ?: 0.0
                            }
                        }
                        totalSum += ptMark
                        subjectsRows.append("""
                        <tr>
                            <td style="border: 1px solid #7A7D81; padding: 5px; text-align: left; font-weight: bold; font-size: 11px;">$sub</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-weight: bold; background: #FAF9F6; font-size: 11px;">${Math.round(ptMark)} <span style="font-weight: normal; font-size: 9px; color: #666;">/ ${weightage.toInt()}</span></td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-weight: bold; font-size: 11px; color: #1E3A8A;">${computeGrade((ptMark / weightage) * 100.0)}</td>
                        </tr>
                        """.trimIndent())
                    } else {
                        totalSum += comp.total
                        subjectsRows.append("""
                        <tr>
                            <td style="border: 1px solid #7A7D81; padding: 5px; text-align: left; font-weight: bold; font-size: 11px;">$sub</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-size: 11px;">${Math.round(comp.ut1)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-size: 11px;">${Math.round(comp.ut2)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-size: 11px;">${Math.round(comp.theory)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-size: 11px;">${Math.round(comp.internal)}</td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-weight: bold; background: #FAF9F6; font-size: 11px;">${Math.round(comp.total)} <span style="font-weight: normal; font-size: 9px; color: #666;">/ ${weightage.toInt()}</span></td>
                            <td style="border: 1px solid #7A7D81; padding: 5px; font-weight: bold; font-size: 11px; color: #1E3A8A;">${computeGrade((comp.total / weightage) * 100.0)}</td>
                        </tr>
                        """.trimIndent())
                    }
                }

                val percentage = if (termMaxSum > 0) (totalSum / termMaxSum) * 100.0 else 0.0
                val coScholGrade = if (percentage >= 75.0) "A" else "B"
                val remark = getRemarks(percentage)
                val attendanceVal = getAttendance(student.name, activeTermName)

                pageBlocks.append("""
                    <div class="page">
                    <div class="outer-border">
                        <div>
                            <!-- Top Info Details Bar -->
                            <div style="display: flex; justify-content: space-between; font-size: 10px; font-weight: bold; margin-bottom: 2px;">
                                <div>${if (config?.printAffiliationNumber != false) "Affiliation No. : ${school.affiliationNumber}" else ""}</div>
                                <div style="text-align: center; text-transform: uppercase;">${if (config?.printSchoolWebsite != false) "website : www.yourschool.com" else ""}</div>
                            </div>

                            <!-- Header -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px;">
                                <tr>
                                    <td style="width: 70px; vertical-align: middle;">
                                        ${if (config?.printBoardLogo != false) """
                                        <div style="width: 60px; height: 60px; border-radius: 50%; border: 2px solid #00A65A; display: inline-block; position: relative; background: #fff; text-align: center; margin-left: 2px;">
                                            <div style="font-size: 6px; font-weight: bold; margin-top: 10px; color: #00A65A; line-height: 1.1;">CENTRAL BOARD OF</div>
                                            <div style="font-size: 5px; font-weight: bold; color: #3C8DBC;">SECONDARY<br>EDUCATION</div>
                                            <div style="font-size: 6px; font-weight: bold; background: #00A65A; color: #fff; position: absolute; bottom: 3px; width: 100%; left: 0; padding: 1px 0; border-radius: 0 0 30px 30px; border-top: 1px solid #fff;">INDIA</div>
                                        </div>
                                        """ else ""}
                                    </td>
                                    <td style="text-align: center; vertical-align: middle;">
                                        <h1 style="font-size: 18px; font-weight: bold; margin: 0; text-transform: uppercase; font-family: Arial, sans-serif;">${school.schoolName}</h1>
                                        <div style="font-size: 10.5px; font-weight: 500; margin: 1px 0;">${school.location}</div>
                                        <div style="font-size: 10.5px; font-weight: bold; margin: 1px 0;">Contact No. : ${school.contactNumber}</div>
                                        <div style="font-size: 11px; font-weight: bold; margin-top: 2px; text-transform: uppercase;">Academic Session : ${school.session}</div>
                                        <div style="font-size: 13px; font-weight: bold; text-decoration: underline; text-transform: uppercase; margin-top: 2px;">Report Card</div>
                                    </td>
                                    <td style="width: 70px; text-align: right; vertical-align: middle;">
                                        <div style="width: 60px; height: 60px; display: inline-block; border-radius: 50%; border: 2px solid #7A7D81; text-align: center; overflow: hidden; background: #E6EEF4; margin-right: 2px;">
                                            ${if (school.schoolLogoBase64.isNotEmpty()) """<img src="data:image/png;base64,${school.schoolLogoBase64}" style="width: 100%; height: 100%; object-fit: contain; border-radius: 50%;" />""" else """<div style="font-size: 26px; line-height: 56px;">${school.logoEmoji}</div>"""}
                                        </div>
                                    </td>
                                  </tr>
                            </table>

                            <!-- Details -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 11.5px; border-top: 2px solid #7A7D81; border-bottom: 2px solid #7A7D81; padding: 4px 0;">
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold; width: 50%;">Student's Name : <span style="font-weight: bold; text-transform: uppercase;">${student.name}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold; width: 50%;">Regn. No. &nbsp;&nbsp;: <span style="font-weight: bold;">.${student.rollNumber}.</span></td>
                                </tr>
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Father's Name &nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.fatherName}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Class &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.className}</span></td>
                                </tr>
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Mother's Name : <span style="font-weight: normal; text-transform: uppercase;">${student.motherName}</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Section &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;: <span style="font-weight: normal; text-transform: uppercase;">${student.sectionName}</span></td>
                                </tr>
                                ${if (config?.printHeightWeight == true) """
                                <tr>
                                    <td style="padding: 3px 0; font-weight: bold;">Height : <span style="font-weight: normal;">_____ cm</span></td>
                                    <td style="padding: 3px 0; font-weight: bold;">Weight : <span style="font-weight: normal;">_____ kg</span></td>
                                </tr>
                                """ else ""}
                            </table>

                            <!-- Subjects Table -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 10.5px; text-align: center;">
                                <thead>
                                    ${if (isSingleExam) """
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold; font-size: 11px;">
                                        <th style="border: 1px solid #7A7D81; padding: 4px; text-align: left;" colspan="1">Scholastic Areas</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px;" colspan="2">$reportLayout</th>
                                    </tr>
                                    <tr style="background: #F8F9FA; border: 1px solid #7A7D81; font-weight: bold; font-size: 9.5px;">
                                        <th style="border: 1px solid #7A7D81; padding: 4px; text-align: left; width: 60%;">Main Subjects</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 25%; font-weight: bold;">Marks Obtained</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 15%; font-weight: bold;">Grade</th>
                                    </tr>
                                    """ else """
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold; font-size: 11px;">
                                        <th style="border: 1px solid #7A7D81; padding: 4px; text-align: left;" colspan="1">Scholastic Areas</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px;" colspan="5">${if (reportLayout == "Annual") "Annual Result" else activeTermName}</th>
                                    </tr>
                                    <tr style="background: #F8F9FA; border: 1px solid #7A7D81; font-weight: bold; font-size: 9.5px;">
                                        <th style="border: 1px solid #7A7D81; padding: 4px; text-align: left; width: 40%;">Main Subjects</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 12%;">${if (activeTermName == "Term 1") "PT 1" else "PT 3"}</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 12%;">${if (activeTermName == "Term 1") "PT 2" else "PT 4"}</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 15%; font-weight: bold;">$finalExamLabel</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 12%; font-weight: bold;">Total</th>
                                        <th style="border: 1px solid #7A7D81; padding: 4px; width: 9%; font-weight: bold;">Grade</th>
                                    </tr>
                                    """}
                                </thead>
                                <tbody>
                                    $subjectsRows
                                    <tr style="font-weight: bold; background: #F8F9FA;">
                                        <td style="border: 1px solid #7A7D81; padding: 5px; text-align: left;">Total</td>
                                        ${if (isSingleExam) """
                                        <td style="border: 1px solid #7A7D81; padding: 5px; background: #FAF9F6;">${Math.round(totalSum)}</td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px;">${String.format("%.2f%%", percentage)}</td>
                                        """ else """
                                        <td style="border: 1px solid #7A7D81; padding: 5px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px;"></td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px; background: #FAF9F6;">${Math.round(totalSum)}</td>
                                        <td style="border: 1px solid #7A7D81; padding: 5px;">${String.format("%.2f%%", percentage)}</td>
                                        """}
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Co scholastic -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 5px; font-size: 11px;">
                                <thead>
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold;">
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: left; width: 80%;">Co-Scholastic Areas [3 Point Grade Scale (A-C)]</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Grade</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Art Education</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade</td>
                                    </tr>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Games</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade</td>
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Discipline -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 5px; font-size: 11px;">
                                <thead>
                                    <tr style="background: #F1F3F5; border: 1px solid #7A7D81; font-weight: bold;">
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: left; width: 80%;">Discipline [3 Point Grade Scale (A-C)]</th>
                                        <th style="border: 1px solid #7A7D81; padding: 3px; text-align: center; width: 20%;">Grade</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; font-weight: bold;">Discipline</td>
                                        <td style="border: 1px solid #7A7D81; padding: 3px; text-align: center; font-weight: bold;">$coScholGrade</td>
                                    </tr>
                                </tbody>
                            </table>

                            <!-- Stats -->
                            <table style="width: 100%; border-collapse: collapse; margin-bottom: 6px; font-size: 11px; text-align: left;">
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 25%; background: #F8F9FA;">Attendance</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 25%; text-align: center; background: #FAF9F6;">$activeTermName</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; width: 50%; text-align: center;">$attendanceVal</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Rank</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; text-align: center;" colspan="2">$rankValue</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Remarks</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: 500;" colspan="2">$remark</td>
                                </tr>
                                <tr>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; background: #F8F9FA;">Result</td>
                                    <td style="border: 1px solid #7A7D81; padding: 4px; font-weight: bold; color: green;" colspan="2">Passed</td>
                                </tr>
                            </table>
                        </div>

                        <!-- Footer -->
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-top: 10px; margin-bottom: 8px; font-size: 11px; font-weight: bold;">
                                <div>Date : <span style="font-weight: 500;">27-05-2026</span></div>
                                <div style="width: 140px; text-align: center; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.teacherSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.teacherSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #aaa; width: 100%; text-align: center; padding-top: 4px;">Class Teacher</div>
                                </div>
                                <div style="width: 140px; text-align: center; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.principalSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.principalSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #aaa; width: 100%; text-align: center; padding-top: 4px;">Principal</div>
                                </div>
                            </div>

                            <table style="width: 100%; border-collapse: collapse; font-size: 8px; border-top: 1px solid #7A7D81;">
                                <tr>
                                    <td style="width: 48%; vertical-align: top; padding-top: 3px;">
                                        <div style="font-weight: bold; margin-bottom: 2px;">Scholastic Areas (Grading on 8 Point Scale)</div>
                                        <table style="width: 100%; border-collapse: collapse; text-align: center; font-size: 7.5px;">
                                            <tr style="background: #F1F3F5; font-weight: bold; border: 1px solid #ddd;">
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Marks Range</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Marks Range</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">A1</td><td style="border: 1px solid #ddd;">91 - 100</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">C1</td><td style="border: 1px solid #ddd;">51 - 60</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">A2</td><td style="border: 1px solid #ddd;">81 - 90</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">C2</td><td style="border: 1px solid #ddd;">41 - 50</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">B1</td><td style="border: 1px solid #ddd;">71 - 80</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">D</td><td style="border: 1px solid #ddd;">33 - 40</td>
                                            </tr>
                                            <tr>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">B2</td><td style="border: 1px solid #ddd;">61 - 70</td>
                                                <td style="border: 1px solid #ddd; font-weight: bold;">E</td><td style="border: 1px solid #ddd;">1 - 32</td>
                                            </tr>
                                        </table>
                                    </td>
                                    <td style="width: 4%;"></td>
                                    <td style="width: 48%; vertical-align: top; padding-top: 3px;">
                                        <div style="font-weight: bold; margin-bottom: 2px;">Co-Scholastic / Discipline Areas (Grading on 3 Point Scale)</div>
                                        <table style="width: 100%; border-collapse: collapse; text-align: center; font-size: 7.5px; margin-bottom: 2px;">
                                            <tr style="background: #F1F3F5; font-weight: bold; border: 1px solid #ddd;">
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade</td>
                                                <td style="border: 1px solid #ddd; padding: 1px;">Grade Points</td>
                                            </tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">A</td><td style="border: 1px solid #ddd;">3</td></tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">B</td><td style="border: 1px solid #ddd;">2</td></tr>
                                            <tr><td style="border: 1px solid #ddd; font-weight: bold;">C</td><td style="border: 1px solid #ddd;">1</td></tr>
                                        </table>
                                        <div style="color: #444; font-size: 7.2px; line-height: 1.1;">
                                            *PA - Periodic Assessment, *MA - Multiple Assessment, *NB - Notebook Submission<br>
                                            *SE - Subject Enrichment / Project, *HY - Half Yearly Exam, *A - Absent, *M - Medical Leave
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                    </div>
                """.trimIndent())
            }
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        color: #000;
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                        background: #fff;
                        -webkit-print-color-adjust: exact;
                    }
                    @page {
                        size: A4 portrait;
                        margin: 6mm 10mm;
                    }
                    .page {
                        page-break-after: always;
                        position: relative;
                        background: white;
                        box-sizing: border-box;
                        height: 280mm;
                        padding: 8px;
                    }
                    .page:last-child {
                        page-break-after: avoid !important;
                    }
                    .outer-border {
                        border: 2.2px solid #7A7D81;
                        padding: 10px;
                        height: 100%;
                        display: flex;
                        flex-direction: column;
                        justify-content: space-between;
                        box-sizing: border-box;
                    }
                </style>
            </head>
            <body>
                $pageBlocks
            </body>
            </html>
        """.trimIndent()

        // Spawn dynamic WebView on Principal Activity main thread
        (context as? android.app.Activity)?.runOnUiThread {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "${school.schoolName.replace(" ", "_")}_Section_${reportLayout.replace(" ", "_")}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    
                    printManager.print(
                        jobName,
                        printAdapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error compiling printable PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Generates a "Common Sheet" PDF where subjects are rows and assessments are columns.
 */
fun printCommonSheetPdf(
    context: Context,
    school: SchoolSetting,
    students: List<Student>,
    allMarks: List<Mark>,
    mode: String, // "UT" or "Term"
    subjectsList: List<String>,
    allSectionSubjects: List<com.example.data.SectionSubject>,
    viewModel: AppViewModel,
    allExamConfigs: List<ExamConfig>
) {
    try {
        val pageBlocks = StringBuilder()
        val scope = (context as? androidx.activity.ComponentActivity)?.lifecycleScope ?: return

        scope.launch {
            for (student in students) {
                val studentMarks = allMarks.filter { it.studentId == student.id }
                val annualResults = if (mode == "Term") viewModel.calculateAnnualResult(student.id) else emptyMap()
                
                val columns = if (mode == "UT") listOf("PT 1", "PT 2", "PT 3", "PT 4") else listOf("Term 1", "Term 2")
                val headerRows = StringBuilder()
                columns.forEach { col ->
                    headerRows.append("""<th style="border: 1px solid #7A7D81; padding: 8px; background: #F1F3F5;">$col</th>""")
                }
                if (mode == "Term") {
                    headerRows.append("""<th style="border: 1px solid #7A7D81; padding: 8px; background: #E6EEF4;">Annual (40/60)</th>""")
                }

                val subjectRows = StringBuilder()
                for (sub in subjectsList) {
                    subjectRows.append("<tr>")
                    subjectRows.append("""<td style="border: 1px solid #7A7D81; padding: 8px; text-align: left; font-weight: bold;">$sub</td>""")
                    
                    for (col in columns) {
                        val mark = studentMarks.find { it.subjectName.equals(sub, ignoreCase = true) && it.examType.equals(col, ignoreCase = true) }
                        val score = mark?.marksObtained?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "-"
                        subjectRows.append("""<td style="border: 1px solid #7A7D81; padding: 8px;">$score</td>""")
                    }
                    
                    if (mode == "Term") {
                        val annualScore = annualResults[sub] ?: 0.0
                        val formattedAnnual = String.format("%.2f", annualScore)
                        subjectRows.append("""<td style="border: 1px solid #7A7D81; padding: 8px; font-weight: bold; background: #F8F9FA;">$formattedAnnual</td>""")
                    }
                    subjectRows.append("</tr>")
                }

                pageBlocks.append("""
                    <div class="page">
                        <div class="outer-border">
                            <div style="text-align: center; margin-bottom: 20px;">
                                <h1 style="margin: 0; font-size: 20px;">${school.schoolName}</h1>
                                <p style="margin: 5px 0; font-size: 14px;">${school.location}</p>
                                <h2 style="margin: 10px 0; text-decoration: underline; font-size: 16px;">Common Sheet: ${if (mode == "UT") "PT" else mode} Results</h2>
                                <div style="display: flex; justify-content: space-between; margin-top: 20px; border-bottom: 2px solid #333; padding-bottom: 10px;">
                                    <span><b>Student:</b> ${student.name}</span>
                                    <span><b>Class:</b> ${student.className} - ${student.sectionName}</span>
                                    <span><b>Roll No:</b> ${student.rollNumber}</span>
                                </div>
                            </div>
                            
                            <table style="width: 100%; border-collapse: collapse; text-align: center; font-size: 14px;">
                                <thead>
                                    <tr>
                                        <th style="border: 1px solid #7A7D81; padding: 8px; text-align: left; background: #F1F3F5;">Subject</th>
                                        $headerRows
                                    </tr>
                                </thead>
                                <tbody>
                                    $subjectRows
                                </tbody>
                            </table>
                            
                            <div style="margin-top: 40px; display: flex; justify-content: space-between;">
                                <div style="text-align: center; width: 200px; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.teacherSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.teacherSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #000; width: 100%; text-align: center; padding-top: 10px;">Class Teacher</div>
                                </div>
                                <div style="text-align: center; width: 200px; display: flex; flex-direction: column; align-items: center;">
                                    <div style="height: 40px; display: flex; align-items: center; justify-content: center; margin-bottom: 2px;">
                                        ${if (school.principalSignature.isNotEmpty()) """<img src="data:image/png;base64,${school.principalSignature}" style="max-height: 40px; max-width: 130px; object-fit: contain;" />""" else ""}
                                    </div>
                                    <div style="border-top: 1px solid #000; width: 100%; text-align: center; padding-top: 10px;">Principal</div>
                                </div>
                            </div>
                        </div>
                    </div>
                """.trimIndent())
            }

            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        body { font-family: sans-serif; margin: 0; padding: 0; }
                        @page { size: A4 portrait; margin: 10mm; }
                        .page { page-break-after: always; padding: 10px; height: 270mm; box-sizing: border-box; }
                        .outer-border { border: 2px solid #333; padding: 20px; height: 100%; box-sizing: border-box; }
                    </style>
                </head>
                <body>
                    $pageBlocks
                </body>
                </html>
            """.trimIndent()

            (context as? android.app.Activity)?.runOnUiThread {
                val webView = WebView(context)
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val jobName = "${school.schoolName.replace(" ", "_")}_${mode}_Results"
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// URI Downscaling Base64 Encoder
fun uriToSignatureBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        val maxDim = 300
        val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val processedBitmap = processSignatureBitmap(scaledBitmap)

        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        
        // Clean up temporary bitmaps
        if (processedBitmap != scaledBitmap) {
            processedBitmap.recycle()
        }
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Extract signature by identifying and removing light paper background and boosting dark/ink contrast
fun processSignatureBitmap(src: Bitmap): Bitmap {
    val width = src.width
    val height = src.height
    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)

    var maxL = 0
    var minL = 255

    // First pass to compute luminance min and max
    for (i in pixels.indices) {
        val color = pixels[i]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val l = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
        if (l > maxL) maxL = l
        if (l < minL) minL = l
    }

    if (maxL <= minL) {
        maxL = 255
        minL = 0
    }

    val range = (maxL - minL).toFloat()
    // Define transition points: background paper is higher range, signature ink is lower range
    val fadeStart = minL + (range * 0.35f)
    val fadeEnd = minL + (range * 0.70f)
    val fadeRange = fadeEnd - fadeStart

    for (i in pixels.indices) {
        val color = pixels[i]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val l = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()

        if (l >= fadeEnd) {
            // Paper background - completely transparent (transparent white background matching white report card)
            pixels[i] = 0x00FFFFFF
        } else if (l <= fadeStart) {
            // solid ink - fully opaque, darken dynamically. Ensure factor doesn't make it brighter, only darker for high contrast
            val factor = (l.toFloat() / fadeEnd).coerceIn(0.1f, 1.0f)
            val rNew = (r * factor).toInt().coerceIn(0, 255)
            val gNew = (g * factor).toInt().coerceIn(0, 255)
            val bNew = (b * factor).toInt().coerceIn(0, 255)
            pixels[i] = (255 shl 24) or (rNew shl 16) or (gNew shl 8) or bNew
        } else {
            // transition edge (anti-aliasing) - smoothly interpolate alpha from 255 to 0
            val t = if (fadeRange > 0) (l - fadeStart) / fadeRange else 0.5f
            val alpha = ((1.0f - t) * 255).toInt().coerceIn(0, 255)
            
            val factor = (l.toFloat() / fadeEnd).coerceIn(0.1f, 1.0f)
            val rNew = (r * factor).toInt().coerceIn(0, 255)
            val gNew = (g * factor).toInt().coerceIn(0, 255)
            val bNew = (b * factor).toInt().coerceIn(0, 255)
            pixels[i] = (alpha shl 24) or (rNew shl 16) or (gNew shl 8) or bNew
        }
    }

    dest.setPixels(pixels, 0, width, 0, 0, width, height)
    return dest
}

