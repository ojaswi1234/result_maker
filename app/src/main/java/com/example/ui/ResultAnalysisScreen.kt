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
import androidx.compose.material.icons.filled.Translate
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

    // Analytics calculations
    val totalStudents = allStudents.size
    val totalMarksRecorded = allMarks.size

    val subjectAverages = remember(allMarks) {
        allMarks.groupBy { it.subjectName }
            .mapValues { (_, marks) ->
                if (marks.isNotEmpty()) marks.sumOf { it.marksObtained } / marks.size else 0.0
            }
            .toList()
            .sortedByDescending { it.second }
    }

    val studentAnalysisList = remember(allStudents, allMarks) {
        allStudents.map { student ->
            val sMarks = allMarks.filter { it.studentId == student.id }
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

    // Top Performers List (Ranked by accumulated percentage)
    val topStudents = remember(allStudents, allMarks) {
        allStudents.map { student ->
            val sMarks = allMarks.filter { it.studentId == student.id }
            val average = if (sMarks.isNotEmpty()) {
                sMarks.sumOf { it.marksObtained } / sMarks.size
            } else 0.0
            Triple(student, average, sMarks.size)
        }
        .filter { it.third > 0 } // Must have marks entered
        .sortedByDescending { it.second }
        .take(5) // Get top 5 honor students
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
                    if (studentAnalysisList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                exportAnalyticsToExcel(
                                    context = context,
                                    schoolName = schoolSetting.schoolName,
                                    session = schoolSetting.session,
                                    analysisList = studentAnalysisList,
                                    districtName = districtName,
                                    allStudents = allStudents
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
                    text = "School Overview Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // SCORECARDS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScorecardWidget(
                        value = totalStudents.toString(),
                        label = "Total Roster",
                        badgeColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    ScorecardWidget(
                        value = String.format("%.0f%%", overallSchoolPassPercentage),
                        label = "Passing Rate",
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
                                    text = "Administrative Excel Sheet Exporter",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Generate and download administrative report with section breakdowns",
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
                            placeholder = { Text("e.g. West Delhi, Bengaluru Urban") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("district_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                Text(
                                    text = "XLSX/CSV Format",
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
                                if (studentAnalysisList.isEmpty()) {
                                    Toast.makeText(context, "No student analytics available to export. Input marks first.", Toast.LENGTH_SHORT).show()
                                } else {
                                    exportAnalyticsToExcel(
                                        context = context,
                                        schoolName = schoolSetting.schoolName,
                                        session = schoolSetting.session,
                                        analysisList = studentAnalysisList,
                                        districtName = districtName,
                                        allStudents = allStudents
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("download_excel_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF107C41), // Beautiful Excel dark green green color accent
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

            // SUBJECT PERFORMANCE PERFORMANCE GRAPH CHART (PURE COMPOSE DYNAMIC BARS)
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
                            text = "Subject Mean Score Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (subjectAverages.isEmpty()) {
                            Text("No grade marks recorded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    
                                    // Custom visual bar chart representation
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
                                                .fillMaxWidth((average / 100.0).toFloat())
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

            // NEW SECTION: STUDENT MARK PERCENTAGE CATEGORIES
            item {
                Text(
                    text = "Performance Grade Distribution",
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
                    colorAccent = Color(0xFFD4AF37), // Golden
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
                    colorAccent = Color(0xFF8B5CF6), // Bright Violet
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
                    colorAccent = Color(0xFF0EA5E9), // Sky Blue
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
                    colorAccent = Color(0xFF10B981), // Green
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
                    colorAccent = Color(0xFFEF4444), // Crimson Red
                    categoryIcon = Icons.Default.ErrorOutline,
                    expanded = showBelow33,
                    onToggleExpand = { showBelow33 = !showBelow33 }
                )
            }

            // HONOR ROLL: TOP PERFORMERS
            item {
                Text(
                    text = "Presidential Honor Roll (Top 5 Performers)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (topStudents.isEmpty()) {
                item {
                    Text("No student rankings available yet. Input standard marks first.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
                // Colored Indicator Badge with Icon
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
                
                // Count Pill & class share
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
                                    // Visual Roll badge
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
                                            text = "Roll: ${sa.student.rollNumber} • Class ${sa.student.className} • Section ${sa.student.sectionName}",
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
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
    analysisList: List<StudentAnalysis>,
    districtName: String,
    allStudents: List<Student>
) {
    try {
        val csvHeader = "Sr. no.,District,Class & Section,Strength(total no. of students registered),appeared/present,absent,passed,failed,pass percentage,Below 33%,33 - 59%,60 - 74%,75 - 89%,90 - 95%,95% & above\n"
        val csvLines = java.lang.StringBuilder()
        
        var index = 1
        val sortedGroups = allStudents.groupBy { Pair(it.className, it.sectionName) }
            .toList()
            .sortedWith(compareBy({ it.first.first }, { it.first.second }))

        sortedGroups.forEach { (classSection, sectionStudents) ->
            val className = classSection.first
            val sectionName = classSection.second
            val appearedStudents = analysisList.filter { 
                it.student.className == className && it.student.sectionName == sectionName 
            }
            
            val strength = sectionStudents.size
            val appeared = appearedStudents.size
            val absent = strength - appeared
            val passed = appearedStudents.count { it.percentage >= 33.0 }
            val failed = appeared - passed
            val passPct = if (appeared > 0) (passed.toDouble() / appeared) * 100.0 else 0.0
            
            val below33Count = appearedStudents.count { it.percentage < 33.0 }
            val r33to59Count = appearedStudents.count { it.percentage >= 33.0 && it.percentage < 60.0 }
            val r60to74Count = appearedStudents.count { it.percentage >= 60.0 && it.percentage < 75.0 }
            val r75to89Count = appearedStudents.count { it.percentage >= 75.0 && it.percentage < 90.0 }
            val r90to95Count = appearedStudents.count { it.percentage >= 90.0 && it.percentage < 95.0 }
            val r95andAboveCount = appearedStudents.count { it.percentage >= 95.0 }
            
            csvLines.append("$index,")
                .append("\"${districtName.replace("\"", "\"\"")}\",")
                .append("\"$className - Section $sectionName\",")
                .append("$strength,")
                .append("$appeared,")
                .append("$absent,")
                .append("$passed,")
                .append("$failed,")
                .append(String.format(java.util.Locale.US, "%.2f%%", passPct)).append(",")
                .append("$below33Count,")
                .append("$r33to59Count,")
                .append("$r60to74Count,")
                .append("$r75to89Count,")
                .append("$r90to95Count,")
                .append("$r95andAboveCount\n")
                
            index++
        }
        
        // Add Overall Summary row
        val totalStrength = allStudents.size
        val totalAppeared = analysisList.size
        val totalAbsent = totalStrength - totalAppeared
        val totalPassed = analysisList.count { it.percentage >= 33.0 }
        val totalFailed = totalAppeared - totalPassed
        val totalPassPct = if (totalAppeared > 0) (totalPassed.toDouble() / totalAppeared) * 100.0 else 0.0

        val totalB33 = analysisList.count { it.percentage < 33.0 }
        val total33to59 = analysisList.count { it.percentage >= 33.0 && it.percentage < 60.0 }
        val total60to74 = analysisList.count { it.percentage >= 60.0 && it.percentage < 75.0 }
        val total75to89 = analysisList.count { it.percentage >= 75.0 && it.percentage < 90.0 }
        val total90to95 = analysisList.count { it.percentage >= 90.0 && it.percentage < 95.0 }
        val total95andAbove = analysisList.count { it.percentage >= 95.0 }

        csvLines.append("Total,")
            .append("\"${districtName.replace("\"", "\"\"")}\",")
            .append("\"All Sections Combined\",")
            .append("$totalStrength,")
            .append("$totalAppeared,")
            .append("$totalAbsent,")
            .append("$totalPassed,")
            .append("$totalFailed,")
            .append(String.format(java.util.Locale.US, "%.2f%%", totalPassPct)).append(",")
            .append("$totalB33,")
            .append("$total33to59,")
            .append("$total60to74,")
            .append("$total75to89,")
            .append("$total90to95,")
            .append("$total95andAbove\n")

        val fileContent = "School: $schoolName\nSession: $session\nGenerated On: 2026-05-28\n\n$csvHeader$csvLines"
        val cleanSchoolName = schoolName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val cleanDistrict = districtName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "${cleanSchoolName}_${cleanDistrict}_Result_Analysis.csv"

        // Step 1: Write to Android device public Downloads folder via MediaStore resolver
        val resolver = context.contentResolver
        var wasSavedToDownloads = false
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/comma-separated-values")
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            var fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (fileUri == null) {
                // handle potential naming conflicts
                val randomSuffix = (1000..9999).random()
                contentValues.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName.replace(".csv", "_$randomSuffix.csv"))
                fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }
            if (fileUri != null) {
                resolver.openOutputStream(fileUri).use { os ->
                    if (os != null) {
                        os.write(fileContent.toByteArray(Charsets.UTF_8))
                        os.flush()
                        wasSavedToDownloads = true
                    }
                }
            }
        } else {
            // Legacy Storage API write
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val destFile = java.io.File(downloadsDir, fileName)
                destFile.writeText(fileContent, Charsets.UTF_8)
                wasSavedToDownloads = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Always also save copy to local cache directory as fallback and to share via FileProvider
        val cacheFile = java.io.File(context.cacheDir, fileName)
        cacheFile.writeText(fileContent, Charsets.UTF_8)
        
        val localFileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        // Compile action intent
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(android.content.Intent.EXTRA_STREAM, localFileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "$schoolName Result Analysis Exporter ($districtName)")
            putExtra(android.content.Intent.EXTRA_TEXT, "Attached please find the comprehensive summary statistics sheet for $schoolName, $districtName district.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = android.content.Intent.createChooser(shareIntent, "Share Result Sheet")
        context.startActivity(chooserIntent)

        if (wasSavedToDownloads) {
            Toast.makeText(context, "Successfully downloaded to Downloads folder: $fileName", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Report compiled successfully! Opened share options.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to compile Excel sheet: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
