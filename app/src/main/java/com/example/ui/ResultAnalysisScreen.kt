package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import android.widget.Toast
import com.example.viewmodel.AppViewModel
import com.example.data.Student
import com.example.data.Mark
import com.example.data.SchoolSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultAnalysisScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val schoolSetting by viewModel.schoolSetting.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()

    var districtName by remember { mutableStateOf("Central District") }

    // Grouping classes and sections
    val sections = remember(allStudents) {
        allStudents.map { Pair(it.className, it.sectionName) }
            .distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
    }

    var selectedSection by remember(sections) { mutableStateOf(sections.firstOrNull()) }
    var searchSectionQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Filtered sections based on search query
    val filteredSections = remember(sections, searchSectionQuery) {
        if (searchSectionQuery.isBlank()) {
            sections
        } else {
            sections.filter {
                it.first.contains(searchSectionQuery, ignoreCase = true) ||
                it.second.contains(searchSectionQuery, ignoreCase = true)
            }
        }
    }

    // Filter students and marks belonging to the actively selected Class & Section
    val sectionStudents = remember(allStudents, selectedSection) {
        selectedSection?.let { sec ->
            allStudents.filter { it.className == sec.first && it.sectionName == sec.second }
        } ?: emptyList()
    }

    val sectionMarks = remember(allMarks, sectionStudents) {
        val studentIds = sectionStudents.map { it.id }.toSet()
        allMarks.filter { it.studentId in studentIds }
    }

    // Analytics calculations strictly for this selected section
    val totalStudents = sectionStudents.size
    val totalMarksRecorded = sectionMarks.size

    val subjectAverages = remember(sectionMarks) {
        sectionMarks.groupBy { it.subjectName }
            .mapValues { (_, marks) ->
                if (marks.isNotEmpty()) marks.sumOf { it.marksObtained } / marks.size else 0.0
            }
            .toList()
            .sortedByDescending { it.second }
    }

    val studentAnalysisList = remember(sectionStudents, sectionMarks) {
        sectionStudents.map { student ->
            val sMarks = sectionMarks.filter { it.studentId == student.id }
            val totalObtained = sMarks.sumOf { it.marksObtained }
            val totalMax = sMarks.sumOf { it.maxMarks }
            val percentage = if (totalMax > 0.0) (totalObtained / totalMax) * 100.0 else 0.0
            StudentAnalysis(
                student = student,
                percentage = percentage,
                marksCount = sMarks.size,
                totalObtained = totalObtained,
                totalMax = totalMax
            )
        }.filter { it.marksCount > 0 }
    }

    val overallSchoolPassPercentage = remember(studentAnalysisList) {
        if (studentAnalysisList.isEmpty()) 0.0
        else {
            val passes = studentAnalysisList.count { it.percentage >= 33.0 }
            (passes.toDouble() / studentAnalysisList.size) * 100.0
        }
    }

    // Top Performers List for the selected section (Ranked by average/percentage)
    val topStudents = remember(sectionStudents, sectionMarks) {
        sectionStudents.map { student ->
            val sMarks = sectionMarks.filter { it.studentId == student.id }
            val average = if (sMarks.isNotEmpty()) {
                sMarks.sumOf { it.marksObtained } / sMarks.size
            } else 0.0
            Triple(student, average, sMarks.size)
        }
        .filter { it.third > 0 } // Must have marks
        .sortedByDescending { it.second }
        .take(5)
    }

    // Expandable categories states
    var showBelow33 by remember { mutableStateOf(false) }
    var showRange33to59 by remember { mutableStateOf(false) }
    var showRange60to74 by remember { mutableStateOf(false) }
    var showRange75to89 by remember { mutableStateOf(false) }
    var showRange90to95 by remember { mutableStateOf(false) }
    var showRange95andAbove by remember { mutableStateOf(false) }

    val below33 = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage < 33.0 } }
    val range33to59 = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage >= 33.0 && it.percentage < 60.0 } }
    val range60to74 = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage >= 60.0 && it.percentage < 75.0 } }
    val range75to89 = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage >= 75.0 && it.percentage < 90.0 } }
    val range90to95 = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage >= 90.0 && it.percentage < 95.0 } }
    val range95andAbove = remember(studentAnalysisList) { studentAnalysisList.filter { it.percentage >= 95.0 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analysis_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    if (selectedSection != null && sectionStudents.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                exportAnalyticsToExcel(
                                    context = context,
                                    schoolName = schoolSetting?.schoolName ?: "Global Academy",
                                    session = schoolSetting?.session ?: "2025 - 2026",
                                    districtName = districtName,
                                    sectionStudents = sectionStudents,
                                    sectionMarks = sectionMarks,
                                    selectedSection = selectedSection
                                )
                            },
                            modifier = Modifier.testTag("analysis_share_excel_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Export Excel Report")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Section Performance Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // SEARCHABLE DROPDOWN
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .testTag("section_selector_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Select Class & Section",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (selectedSection != null) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Display selected section button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .clickable { dropdownExpanded = !dropdownExpanded }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val displayName = selectedSection?.let { "${it.first} (Section ${it.second})" } ?: "No Sections Found"
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (dropdownExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = searchSectionQuery,
                                onValueChange = { searchSectionQuery = it },
                                placeholder = { Text("Type to search class/section...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_section_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                if (filteredSections.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No matching sections found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        items(filteredSections) { sec ->
                                            val isSelected = sec == selectedSection
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                                    .clickable {
                                                        selectedSection = sec
                                                        dropdownExpanded = false
                                                        searchSectionQuery = ""
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${sec.first} (Section ${sec.second})",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
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
                }
            }

            // SCORECARDS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScorecardWidget(
                        value = totalStudents.toString(),
                        label = "Section Strength",
                        badgeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    ScorecardWidget(
                        value = String.format("%.0f%%", overallSchoolPassPercentage),
                        label = "Section Pass Rate",
                        badgeColor = Color(0xFF0F9D58),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // DOWNLOAD EXCEL BANNER
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .testTag("export_excel_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF107C41).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export CSV Excel Icon",
                                    tint = Color(0xFF107C41),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Marksheet Excel-Book Compiler",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Generates class matching marksheet based exactly on the requested template",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // DISTRICT TEXT FIELD
                        OutlinedTextField(
                            value = districtName,
                            onValueChange = { districtName = it },
                            label = { Text("District / Region Name") },
                            placeholder = { Text("e.g. West Delhi, G B NAGAR") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("district_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                Text(
                                    text = "XLSX/CSV",
                                    fontSize = 9.sp,
                                    color = Color(0xFF107C41),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0xFF107C41).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        )

                        // DOWNLOAD BUTTON WITH EXCEL STYLING
                        Button(
                            onClick = {
                                if (selectedSection == null) {
                                    Toast.makeText(context, "Please select Class & Section first.", Toast.LENGTH_SHORT).show()
                                } else if (sectionStudents.isEmpty()) {
                                    Toast.makeText(context, "No rostered students found for this section.", Toast.LENGTH_SHORT).show()
                                } else if (sectionMarks.isEmpty()) {
                                    Toast.makeText(context, "No grades entered yet for this section.", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportAnalyticsToExcel(
                                        context = context,
                                        schoolName = schoolSetting?.schoolName ?: "Global Academy",
                                        session = schoolSetting?.session ?: "2025 - 2026",
                                        districtName = districtName,
                                        sectionStudents = sectionStudents,
                                        sectionMarks = sectionMarks,
                                        selectedSection = selectedSection
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                                .testTag("download_excel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF107C41),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compile & Download Excel Sheet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // SUBJECT PERFORMANCE GRAPH CHART
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .testTag("analysis_subject_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Subject Average Score Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (subjectAverages.isEmpty()) {
                            Text("No marks recorded yet for this section.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            subjectAverages.forEach { (subject, average) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = subject, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(text = String.format("%.1f", average), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((average / 100.0).coerceIn(0.0, 1.0).toFloat())
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                            MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PERFORMANCE GRADE DISTRIBUTION
            item {
                Text(
                    text = "Section Grade Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                CategorySectionWidget(
                    title = "Elite Performance",
                    rangeText = "95% & Above",
                    students = range95andAbove,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = Color(0xFFD4AF37),
                    categoryIcon = Icons.Default.Star,
                    expanded = showRange95andAbove,
                    onToggleExpand = { showRange95andAbove = !showRange95andAbove }
                )
            }

            item {
                CategorySectionWidget(
                    title = "Superb Performance",
                    rangeText = "90 - 95%",
                    students = range90to95,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = Color(0xFF8B5CF6),
                    categoryIcon = Icons.Default.WorkspacePremium,
                    expanded = showRange90to95,
                    onToggleExpand = { showRange90to95 = !showRange90to95 }
                )
            }

            item {
                CategorySectionWidget(
                    title = "Distinction Mark",
                    rangeText = "75 - 89%",
                    students = range75to89,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = MaterialTheme.colorScheme.primary,
                    categoryIcon = Icons.Default.Assessment,
                    expanded = showRange75to89,
                    onToggleExpand = { showRange75to89 = !showRange75to89 }
                )
            }

            item {
                CategorySectionWidget(
                    title = "Average Mark",
                    rangeText = "60 - 74%",
                    students = range60to74,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = Color(0xFF0EA5E9),
                    categoryIcon = Icons.Default.School,
                    expanded = showRange60to74,
                    onToggleExpand = { showRange60to74 = !showRange60to74 }
                )
            }

            item {
                CategorySectionWidget(
                    title = "Passing Mark",
                    rangeText = "33 - 59%",
                    students = range33to59,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = Color(0xFF10B981),
                    categoryIcon = Icons.Default.CheckCircle,
                    expanded = showRange33to59,
                    onToggleExpand = { showRange33to59 = !showRange33to59 }
                )
            }

            item {
                CategorySectionWidget(
                    title = "Needs Improvement",
                    rangeText = "Below 33%",
                    students = below33,
                    totalGradedStudents = studentAnalysisList.size,
                    colorAccent = Color(0xFFEF4444),
                    categoryIcon = Icons.Default.ErrorOutline,
                    expanded = showBelow33,
                    onToggleExpand = { showBelow33 = !showBelow33 }
                )
            }

            // HONOR ROLL (TOP 5)
            item {
                Text(
                    text = "Section Honor Roll (Top 5 Performers)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (topStudents.isEmpty()) {
                item {
                    Text("No student rankings available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                items(topStudents) { (student, average, subjectCount) ->
                    ListItem(
                        headlineContent = {
                            Text(student.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        },
                        supportingContent = {
                            Text("Class ${student.className} • ${student.sectionName} ($subjectCount Subjects)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                              ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Rank Winner",
                                    tint = Color(0xFFF4B400),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = String.format("%.1f%%", average),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Award Star",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySectionWidget(
    title: String,
    rangeText: String,
    students: List<StudentAnalysis>,
    totalGradedStudents: Int,
    colorAccent: Color,
    categoryIcon: androidx.compose.ui.graphics.vector.ImageVector,
    expanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val pctOfClass = if (totalGradedStudents > 0) (students.size.toDouble() / totalGradedStudents) * 100.0 else 0.0

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (expanded) colorAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colorAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = colorAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = rangeText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Badge(
                        containerColor = colorAccent,
                        contentColor = Color.White,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "${students.size} ${if (students.size == 1) "Student" else "Students"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (students.isNotEmpty()) {
                        Text(
                            text = String.format("%.0f%% of roster", pctOfClass),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                if (students.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            .padding(vertical = 16.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No students in this performance range.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        students.sortedByDescending { sa -> sa.percentage }.forEach { sa ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sa.student.rollNumber.takeLast(3),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = sa.student.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Roll: ${sa.student.rollNumber} • Class ${sa.student.className} • Sec ${sa.student.sectionName}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format("%.1f%%", sa.percentage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = colorAccent
                                    )
                                    Text(
                                        text = "${sa.totalObtained.toInt()}/${sa.totalMax.toInt()}",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun ScorecardWidget(
    value: String,
    label: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }
        }
    }
}

data class StudentAnalysis(
    val student: Student,
    val percentage: Double,
    val marksCount: Int,
    val totalObtained: Double,
    val totalMax: Double
)

fun exportAnalyticsToExcel(
    context: android.content.Context,
    schoolName: String,
    session: String,
    districtName: String,
    sectionStudents: List<Student>,
    sectionMarks: List<Mark>,
    selectedSection: Pair<String, String>?
) {
    try {
        if (selectedSection == null) {
            Toast.makeText(context, "Please select a section first.", Toast.LENGTH_SHORT).show()
            return
        }

        val className = selectedSection.first
        val sectionName = selectedSection.second

        // Extract list of unique subjects recorded for this section
        val subjectsList = sectionMarks.map { it.subjectName }
            .distinct()
            .sorted()

        // Match maximum marks dynamically for each subject
        val subjectMaxMarks = subjectsList.associateWith { name ->
            sectionMarks.filter { it.subjectName == name }.maxOfOrNull { it.maxMarks } ?: 40.0
        }

        val sbOutput = java.lang.StringBuilder()

        // Line 1: School Header
        sbOutput.append("\"${schoolName.uppercase().replace("\"", "\"\"")}\"\n")

        // Line 2: Report Title matching the exact PDF layout
        val examType = sectionMarks.firstOrNull()?.examType ?: "UT 1"
        sbOutput.append("\"MARKS STATEMENT $examType CLASS-${className.uppercase()} $sectionName $session\"\n")

        // Line 3: MAXIMUM MARKS row
        sbOutput.append("MAXIMUM MARKS,,")
        subjectsList.forEach { col ->
            val mx = subjectMaxMarks[col] ?: 40.0
            sbOutput.append(if (mx % 1.0 == 0.0) "${mx.toInt()}," else "$mx,")
        }
        sbOutput.append(",,\n") // Empty cells under Total, %age, RANK

        // Line 4: Table Row Header Column Names
        sbOutput.append("Roll No,NAME OF STUDENTS,")
        subjectsList.forEach { sub ->
            sbOutput.append("\"${sub.uppercase().replace("\"", "\"\"")}\",")
        }
        sbOutput.append("Total,%age,RANK\n")

        // Sort students by Roll number for organized list layout
        val sortedStudents = sectionStudents.sortedWith(compareBy({ it.rollNumber }, { it.name }))

        // Compute Roster totals & percentages
        val studentTotalObtained = sortedStudents.associateWith { s ->
            val sMarks = sectionMarks.filter { it.studentId == s.id }
            sMarks.sumOf { it.marksObtained }
        }

        val studentPercentages = sortedStudents.associateWith { s ->
            val sMarks = sectionMarks.filter { it.studentId == s.id }
            val totalObtained = sMarks.sumOf { it.marksObtained }
            val totalMax = sMarks.sumOf { it.maxMarks }
            if (totalMax > 0.0) (totalObtained / totalMax) * 100.0 else 0.0
        }

        val studentRanks = sortedStudents.associateWith { s ->
            val curPct = studentPercentages[s] ?: 0.0
            val higher = studentPercentages.values.count { it > curPct }
            higher + 1
        }

        // Student Data Rows
        sortedStudents.forEach { s ->
            sbOutput.append("\"${s.rollNumber.replace("\"", "\"\"")}\",")
            sbOutput.append("\"${s.name.replace("\"", "\"\"")}\",")

            val sMarks = sectionMarks.filter { it.studentId == s.id }

            // Dynamic subject marks columns
            subjectsList.forEach { sub ->
                val record = sMarks.find { it.subjectName == sub }
                if (record != null) {
                    val score = record.marksObtained
                    sbOutput.append(if (score % 1.0 == 0.0) "${score.toInt()}," else "$score,")
                } else {
                    sbOutput.append(",") // blank cell represents absent/unentered
                }
            }

            // Total Column
            val tot = studentTotalObtained[s] ?: 0.0
            sbOutput.append(if (tot % 1.0 == 0.0) "${tot.toInt()}," else "$tot,")

            // Percentage % Cell
            val pct = studentPercentages[s] ?: 0.0
            sbOutput.append(String.format(java.util.Locale.US, "%.2f,", pct))

            // Rank Cell
            val rk = studentRanks[s] ?: 1
            sbOutput.append("$rk\n")
        }

        // Add visual layout separation
        sbOutput.append("\n\n")

        // Summary Subjects Row matching PDF
        sbOutput.append(",,")
        subjectsList.forEach { sub ->
            sbOutput.append("\"${sub.uppercase().replace("\"", "\"\"")}\",")
        }
        sbOutput.append("OALL\n")

        val subjectAppearedCount = subjectsList.associateWith { sub ->
            sectionMarks.count { it.subjectName == sub }
        }

        val subjectTotalObt = subjectsList.associateWith { sub ->
            sectionMarks.filter { it.subjectName == sub }.sumOf { it.marksObtained }
        }

        // 1st Summary Row: Total Marks Obtained
        sbOutput.append("Total,,")
        subjectsList.forEach { sub ->
            val sumMarks = subjectTotalObt[sub] ?: 0.0
            sbOutput.append(if (sumMarks % 1.0 == 0.0) "${sumMarks.toInt()}," else "$sumMarks,")
        }
        val sumPct = studentPercentages.values.sum()
        sbOutput.append(String.format(java.util.Locale.US, "%.2f\n", sumPct))

        // 2nd Summary Row: Average
        sbOutput.append("Average,,")
        subjectsList.forEach { sub ->
            val appCount = subjectAppearedCount[sub] ?: 0
            val maxMarkSub = subjectMaxMarks[sub] ?: 40.0
            val sumMarks = subjectTotalObt[sub] ?: 0.0
            if (appCount > 0) {
                val averagePercentVal = (sumMarks / (appCount * maxMarkSub)) * 100.0
                sbOutput.append(String.format(java.util.Locale.US, "%.2f,", averagePercentVal))
            } else {
                sbOutput.append("#DIV/0!,")
            }
        }
        val sectionStrength = sortedStudents.size
        val avgOverall = if (sectionStrength > 0) sumPct / sectionStrength else 0.0
        sbOutput.append(String.format(java.util.Locale.US, "%.2f\n", avgOverall))

        // 3rd Summary Row: Appeared students
        sbOutput.append("Appeared,,")
        subjectsList.forEach { sub ->
            val appCount = subjectAppearedCount[sub] ?: 0
            sbOutput.append("$appCount,")
        }
        sbOutput.append("$sectionStrength\n")

        // 4th Summary Row: Below 33%
        sbOutput.append("Below 33,,")
        subjectsList.forEach { sub ->
            val marksCountBelow = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                if (mx > 0) (it.marksObtained / mx) * 100.0 < 33.0 else true
            }
            sbOutput.append("$marksCountBelow,")
        }
        val countOallBelow33 = studentPercentages.values.count { it < 33.0 }
        sbOutput.append("$countOallBelow33\n")

        // 5th Summary Row: 33-59%
        sbOutput.append("33-59,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 33.0 && pt < 60.0
            }
            sbOutput.append("$sCount,")
        }
        val countOall33to59 = studentPercentages.values.count { it >= 33.0 && it < 60.0 }
        sbOutput.append("$countOall33to59\n")

        // 6th Summary Row: 60-74%
        sbOutput.append("60-74,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 60.0 && pt < 75.0
            }
            sbOutput.append("$sCount,")
        }
        val countOall60to74 = studentPercentages.values.count { it >= 60.0 && it < 75.0 }
        sbOutput.append("$countOall60to74\n")

        // 7th Summary Row: 75-89%
        sbOutput.append("75-89,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 75.0 && pt < 90.0
            }
            sbOutput.append("$sCount,")
        }
        val countOall75to89 = studentPercentages.values.count { it >= 75.0 && it < 90.0 }
        sbOutput.append("$countOall75to89\n")

        // 8th Summary Row: 90-95%
        sbOutput.append("90-95,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 90.0 && pt < 95.0
            }
            sbOutput.append("$sCount,")
        }
        val countOall90to95 = studentPercentages.values.count { it >= 90.0 && it < 95.0 }
        sbOutput.append("$countOall90to95\n")

        // 9th Summary Row: 95% & Above
        sbOutput.append("95 & Above,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 95.0
            }
            sbOutput.append("$sCount,")
        }
        val countOall95 = studentPercentages.values.count { it >= 95.0 }
        sbOutput.append("$countOall95\n")

        // 10th Summary Row: Passed
        sbOutput.append("Passed,,")
        subjectsList.forEach { sub ->
            val sCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 33.0
            }
            sbOutput.append("$sCount,")
        }
        val countOallPassed = studentPercentages.values.count { it >= 33.0 }
        sbOutput.append("$countOallPassed\n")

        // 11th Summary Row: Pass Percentage
        sbOutput.append("Pass %,,")
        subjectsList.forEach { sub ->
            val appCount = subjectAppearedCount[sub] ?: 0
            val pCount = sectionMarks.filter { it.subjectName == sub }.count {
                val mx = it.maxMarks
                val pt = if (mx > 0) (it.marksObtained / mx) * 100.0 else 0.0
                pt >= 33.0
            }
            if (appCount > 0) {
                val pctPassed = (pCount.toDouble() / appCount) * 100.0
                sbOutput.append(String.format(java.util.Locale.US, "%.1f,", pctPassed))
            } else {
                sbOutput.append("#DIV/0!,")
            }
        }
        if (sectionStrength > 0) {
            val passPctVal = (countOallPassed.toDouble() / sectionStrength) * 100.0
            sbOutput.append(String.format(java.util.Locale.US, "%.1f\n", passPctVal))
        } else {
            sbOutput.append("0.0\n")
        }

        // 12th Block: Verify and Sign verification table rows at bottom
        sbOutput.append("\n\n")
        sbOutput.append("Verify and sign\n")
        sbOutput.append("subject teachers,Signature\n")
        subjectsList.forEach { sub ->
            sbOutput.append("\"${sub.uppercase().replace("\"", "\"\"")}\"\n")
        }
        sbOutput.append("Class teacher\n")
        sbOutput.append("Exam Incharge\n")
        sbOutput.append("PRINCIPAL\n")

        // Compile and write file inside user-accessible Downloads repository directory
        val fileContent = sbOutput.toString()
        val cleanSchoolName = schoolName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanClass = className.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanSecStr = sectionName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanDistrict = districtName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${cleanSchoolName}_${cleanDistrict}_CLASS_${cleanClass}_SEC_${cleanSecStr}_MarksStatement.csv"

        val resolver = context.contentResolver
        var savedDownloads = false

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/comma-separated-values")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            var fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (fileUri == null) {
                // handle collision naming
                val rSfx = (1000..9999).random()
                contentValues.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName.replace(".csv", "_$rSfx.csv"))
                fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }
            if (fileUri != null) {
                resolver.openOutputStream(fileUri).use { os ->
                    if (os != null) {
                        os.write(fileContent.toByteArray(Charsets.UTF_8))
                        os.flush()
                        savedDownloads = true
                    }
                }
            }
        } else {
            try {
                val ddir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val destFile = java.io.File(ddir, fileName)
                destFile.writeText(fileContent, Charsets.UTF_8)
                savedDownloads = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback local sharing FileProvider copy
        val cacheFile = java.io.File(context.cacheDir, fileName)
        cacheFile.writeText(fileContent, Charsets.UTF_8)

        val localUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(android.content.Intent.EXTRA_STREAM, localUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Marksheet Excel: Class $className - Sec $sectionName ($districtName)")
            putExtra(android.content.Intent.EXTRA_TEXT, "Hello Admin, enclosed please find the marks statement report sheet for Class $className - Section $sectionName, $districtName region.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Marksheet Excel"))

        if (savedDownloads) {
            Toast.makeText(context, "Successfully downloaded: $fileName", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Excel report compiled to share!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to compile Excel: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
