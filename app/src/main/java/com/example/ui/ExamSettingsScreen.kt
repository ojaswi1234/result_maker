package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ExamConfig
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val allExamConfigs by viewModel.allExamConfigs.collectAsState()

    // Dynamic distinct sorted classes from pupils database
    val databaseClasses = remember(allStudents) {
        allStudents.map { it.className }.distinct()
    }
    
    val classesList = remember(databaseClasses) {
        databaseClasses.sorted()
    }

    var selectedClass by remember { mutableStateOf<String?>(null) }

    // Checkbox State Variables
    var hasMultipleAssessment by remember { mutableStateOf(false) }
    var multipleAssessmentMarks by remember { mutableStateOf("5") }
    
    var hasNotebookSubmission by remember { mutableStateOf(true) }
    var notebookSubmissionMarks by remember { mutableStateOf("5") }
    
    var hasSubjectEnrichment by remember { mutableStateOf(true) }
    var subjectEnrichmentMarks by remember { mutableStateOf("5") }
    
    var hasPaWeightage by remember { mutableStateOf(true) }
    var paWeightageMarks by remember { mutableStateOf("10") }

    // PA Term 1 State
    var t1PaCount by remember { mutableStateOf("1") }
    var t1MaxMarks1 by remember { mutableStateOf("30") }
    var t1MaxMarks2 by remember { mutableStateOf("") }
    var t1MaxMarks3 by remember { mutableStateOf("") }

    // PA Term 2 State
    var t2PaCount by remember { mutableStateOf("1") }
    var t2MaxMarks1 by remember { mutableStateOf("50") }
    var t2MaxMarks2 by remember { mutableStateOf("") }
    var t2MaxMarks3 by remember { mutableStateOf("") }

    // Calculation Logic: "Average", "Best", "Best of 2" (maps to Average of All PAs, Best of All PAs, Average of Best 2 PAs)
    var selectedLogic by remember { mutableStateOf("Average") }

    // Load existing config when class changes
    LaunchedEffect(selectedClass) {
        selectedClass?.let { cls ->
            val existing = allExamConfigs.find { it.className == cls }
            if (existing != null) {
                hasMultipleAssessment = existing.hasMultipleAssessment
                multipleAssessmentMarks = existing.multipleAssessmentMarks.toInt().toString()
                
                hasNotebookSubmission = existing.hasNotebookSubmission
                notebookSubmissionMarks = existing.notebookSubmissionMarks.toInt().toString()
                
                hasSubjectEnrichment = existing.hasSubjectEnrichment
                subjectEnrichmentMarks = existing.subjectEnrichmentMarks.toInt().toString()
                
                hasPaWeightage = existing.hasPaWeightage
                paWeightageMarks = existing.paWeightageMarks.toInt().toString()

                t1PaCount = existing.t1PaCount.toString()
                t1MaxMarks1 = existing.t1PaMaxMarks1.toInt().toString()
                t1MaxMarks2 = if (existing.t1PaMaxMarks2 > 0) existing.t1PaMaxMarks2.toInt().toString() else ""
                t1MaxMarks3 = if (existing.t1PaMaxMarks3 > 0) existing.t1PaMaxMarks3.toInt().toString() else ""

                t2PaCount = existing.t2PaCount.toString()
                t2MaxMarks1 = existing.t2PaMaxMarks1.toInt().toString()
                t2MaxMarks2 = if (existing.t2PaMaxMarks2 > 0) existing.t2PaMaxMarks2.toInt().toString() else ""
                t2MaxMarks3 = if (existing.t2PaMaxMarks3 > 0) existing.t2PaMaxMarks3.toInt().toString() else ""

                selectedLogic = existing.t1CalculationLogic
            } else {
                // Load Defaults
                hasMultipleAssessment = false
                multipleAssessmentMarks = "5"
                hasNotebookSubmission = true
                notebookSubmissionMarks = "5"
                hasSubjectEnrichment = true
                subjectEnrichmentMarks = "5"
                hasPaWeightage = true
                paWeightageMarks = "10"

                t1PaCount = "1"
                t1MaxMarks1 = "30"
                t1MaxMarks2 = ""
                t1MaxMarks3 = ""

                t2PaCount = "1"
                t2MaxMarks1 = "50"
                t2MaxMarks2 = ""
                t2MaxMarks3 = ""

                selectedLogic = "Average"
            }
        }
    }

    val themeCyan = Color(0xFF139DC4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Exams Setting -...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedClass != null) {
                                selectedClass = null
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("exam_settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeCyan
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            if (selectedClass == null) {
                // SCREEN 1: Available Classes list layout
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFCCCCCC))
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Available Classes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                items(classesList) { className ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { selectedClass = className }
                            .testTag("class_tile_$className"),
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(0.5.dp, Color(0xFFDDDDDD))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = className,
                                fontSize = 17.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // SCREEN 2: Combined Settings details page
                item {
                    Column {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFF999999))
                        ) {
                            Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Settings of $selectedClass",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF444444)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // CHECKBOXES TABLE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF7A7D81))
                            .background(Color.White)
                    ) {
                        // ROW 1: MA
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = hasMultipleAssessment,
                                    onCheckedChange = { hasMultipleAssessment = it }
                                )
                                Text(stringResource(R.string.ma_multiple_assessment), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = multipleAssessmentMarks,
                                        onValueChange = { multipleAssessmentMarks = it },
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = " Marks",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // ROW 2: Notebook
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = hasNotebookSubmission,
                                    onCheckedChange = { hasNotebookSubmission = it }
                                )
                                Text(stringResource(R.string.notebook_submission), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = notebookSubmissionMarks,
                                        onValueChange = { notebookSubmissionMarks = it },
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = " Marks",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // ROW 3: Subject Enrichment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = hasSubjectEnrichment,
                                    onCheckedChange = { hasSubjectEnrichment = it }
                                )
                                Text(stringResource(R.string.subject_enrichment_project), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = subjectEnrichmentMarks,
                                        onValueChange = { subjectEnrichmentMarks = it },
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = " Marks",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // ROW 4: PA Weightage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = hasPaWeightage,
                                    onCheckedChange = { hasPaWeightage = it }
                                )
                                Text(stringResource(R.string.pa_weightage), fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = paWeightageMarks,
                                        onValueChange = { paWeightageMarks = it },
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(32.dp)
                                    )
                                    Text(
                                        text = " Marks",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    // T1 PANEL
                    val t1CountInt = t1PaCount.toIntOrNull() ?: 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF7A7D81))
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .background(Color.White)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.pa_term_1), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Number of PA
                        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(themeCyan)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "Number of PA/s (1 to 3)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t1PaCount,
                                    onValueChange = { t1PaCount = it },
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA1
                        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(Color.White)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(stringResource(R.string.max_marks_pa1), color = Color(0xFF222222), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t1MaxMarks1,
                                    onValueChange = { t1MaxMarks1 = it },
                                    textStyle = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color.Black),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA2
                        val hasPA2 = t1CountInt >= 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(if (hasPA2) Color.White else Color(0xFFF5F5F5))
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Max Marks in PA2",
                                    color = if (hasPA2) Color(0xFF222222) else Color(0xFF999999), 
                                    fontSize = 13.sp,
                                    fontWeight = if (hasPA2) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(if (hasPA2) Color.White else Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t1MaxMarks2,
                                    onValueChange = { t1MaxMarks2 = it },
                                    enabled = hasPA2,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp, 
                                        textAlign = TextAlign.Center, 
                                        fontWeight = if (hasPA2) FontWeight.Bold else FontWeight.Normal, 
                                        color = if (hasPA2) Color.Black else Color(0xFF999999)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA3
                        val hasPA3 = t1CountInt >= 3
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(if (hasPA3) Color.White else Color(0xFFF5F5F5))
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Max Marks in PA3", 
                                    color = if (hasPA3) Color(0xFF222222) else Color(0xFF999999), 
                                    fontSize = 13.sp,
                                    fontWeight = if (hasPA3) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(if (hasPA3) Color.White else Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t1MaxMarks3,
                                    onValueChange = { t1MaxMarks3 = it },
                                    enabled = hasPA3,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp, 
                                        textAlign = TextAlign.Center, 
                                        fontWeight = if (hasPA3) FontWeight.Bold else FontWeight.Normal, 
                                        color = if (hasPA3) Color.Black else Color(0xFF999999)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // T2 PANEL
                    val t2CountInt = t2PaCount.toIntOrNull() ?: 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF7A7D81))
                            .background(Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .background(Color.White)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.pa_term_2), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Number of PA
                        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(themeCyan)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "Number of PA/s (1 to 3)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t2PaCount,
                                    onValueChange = { t2PaCount = it },
                                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA1
                        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(Color.White)
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(stringResource(R.string.max_marks_pa1), color = Color(0xFF222222), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t2MaxMarks1,
                                    onValueChange = { t2MaxMarks1 = it },
                                    textStyle = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = Color.Black),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA2
                        val hasPA2_2 = t2CountInt >= 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(if (hasPA2_2) Color.White else Color(0xFFF5F5F5))
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Max Marks in PA2", 
                                    color = if (hasPA2_2) Color(0xFF222222) else Color(0xFF999999), 
                                    fontSize = 13.sp,
                                    fontWeight = if (hasPA2_2) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(if (hasPA2_2) Color.White else Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t2MaxMarks2,
                                    onValueChange = { t2MaxMarks2 = it },
                                    enabled = hasPA2_2,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp, 
                                        textAlign = TextAlign.Center, 
                                        fontWeight = if (hasPA2_2) FontWeight.Bold else FontWeight.Normal, 
                                        color = if (hasPA2_2) Color.Black else Color(0xFF999999)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                        // Max Marks PA3
                        val hasPA3_2 = t2CountInt >= 3
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.6f)
                                    .fillMaxHeight()
                                    .background(if (hasPA3_2) Color.White else Color(0xFFF5F5F5))
                                    .padding(start = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = "Max Marks in PA3", 
                                    color = if (hasPA3_2) Color(0xFF222222) else Color(0xFF999999), 
                                    fontSize = 13.sp,
                                    fontWeight = if (hasPA3_2) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                            Box(
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                                    .background(if (hasPA3_2) Color.White else Color(0xFFEBEBEB)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = t2MaxMarks3,
                                    onValueChange = { t2MaxMarks3 = it },
                                    enabled = hasPA3_2,
                                    textStyle = TextStyle(
                                        fontSize = 15.sp, 
                                        textAlign = TextAlign.Center, 
                                        fontWeight = if (hasPA3_2) FontWeight.Bold else FontWeight.Normal, 
                                        color = if (hasPA3_2) Color.Black else Color(0xFF999999)
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // CALCULATION LOGIC RADIO BUTTONS BLOCK
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Calculation of PA in Each Term in Mark Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF444444),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.Top
                        ) {
                            // Option 1: Average
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, 
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLogic = "Average" }
                            ) {
                                Text(
                                    text = "Average of\nAll PAs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                RadioButton(
                                    selected = selectedLogic == "Average",
                                    onClick = { selectedLogic = "Average" }
                                )
                            }

                            // Option 2: Best
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, 
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLogic = "Best" }
                            ) {
                                Text(
                                    text = "Best of\nAll PAs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                RadioButton(
                                    selected = selectedLogic == "Best",
                                    onClick = { selectedLogic = "Best" }
                                )
                            }

                            // Option 3: Average of Best 2
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, 
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLogic = "Best of 2" }
                            ) {
                                Text(
                                    text = "Average of\nBest 2 PAs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color.Black,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                RadioButton(
                                    selected = selectedLogic == "Best of 2",
                                    onClick = { selectedLogic = "Best of 2" }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // NEXT BUTTON (SOLID TEAL CYAN BACKGROUND AS IN IMAGE 1)
                    Button(
                        onClick = {
                            val finalConfig = ExamConfig(
                                className = selectedClass!!,
                                isConfigured = true,
                                additionalSubjectsString = "", // standard baseline configuration
                                t1PaCount = t1PaCount.toIntOrNull() ?: 1,
                                t1PaMaxMarks1 = t1MaxMarks1.toDoubleOrNull() ?: 30.0,
                                t1PaMaxMarks2 = t1MaxMarks2.toDoubleOrNull() ?: 0.0,
                                t1PaMaxMarks3 = t1MaxMarks3.toDoubleOrNull() ?: 0.0,
                                t1CalculationLogic = selectedLogic,
                                t2PaCount = t2PaCount.toIntOrNull() ?: 1,
                                t2PaMaxMarks1 = t2MaxMarks1.toDoubleOrNull() ?: 50.0,
                                t2PaMaxMarks2 = t2MaxMarks2.toDoubleOrNull() ?: 0.0,
                                t2PaMaxMarks3 = t2MaxMarks3.toDoubleOrNull() ?: 0.0,
                                t2CalculationLogic = selectedLogic,
                                printSchoolWebsite = true,
                                printAffiliationNumber = true,
                                printBoardLogo = true,
                                printHeightWeight = true,
                                hasMultipleAssessment = hasMultipleAssessment,
                                multipleAssessmentMarks = multipleAssessmentMarks.toDoubleOrNull() ?: 5.0,
                                hasNotebookSubmission = hasNotebookSubmission,
                                notebookSubmissionMarks = notebookSubmissionMarks.toDoubleOrNull() ?: 5.0,
                                hasSubjectEnrichment = hasSubjectEnrichment,
                                subjectEnrichmentMarks = subjectEnrichmentMarks.toDoubleOrNull() ?: 5.0,
                                hasPaWeightage = hasPaWeightage,
                                paWeightageMarks = paWeightageMarks.toDoubleOrNull() ?: 10.0
                            )

                            viewModel.saveExamConfig(finalConfig)
                            Toast.makeText(context, "Configurations saved for $selectedClass", Toast.LENGTH_SHORT).show()
                            selectedClass = null // reset selection back to classes list
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeCyan),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(vertical = 12.dp)
                            .testTag("wizard_next_btn")
                    ) {
                        Text(
                            text = "NEXT",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                }
                }
            }
        }
    }
}
