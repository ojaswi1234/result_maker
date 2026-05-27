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
import com.example.viewmodel.AppViewModel
import com.example.data.Student
import com.example.data.Mark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultAnalysisScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()

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

    val overallSchoolPassPercentage = remember(allStudents, allMarks) {
        if (allStudents.isEmpty() || allMarks.isEmpty()) 0.0
        else {
            val studentAverages = allStudents.map { student ->
                val sMarks = allMarks.filter { it.studentId == student.id }
                if (sMarks.isNotEmpty()) sMarks.sumOf { it.marksObtained } / sMarks.size else 0.0
            }
            val passes = studentAverages.count { it >= 40.0 }
            (passes.toDouble() / allStudents.size) * 100.0
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analysis_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
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
