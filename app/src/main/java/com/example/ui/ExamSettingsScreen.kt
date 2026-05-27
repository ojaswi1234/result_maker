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
import com.example.data.ExamConfig
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val allStudents by viewModel.allStudents.collectAsState()
    val allExamConfigs by viewModel.allExamConfigs.collectAsState()
    val activeRole by viewModel.activeRole.collectAsState()

    // Extract list of all unique classes matching students
    val classesList = remember(allStudents) {
        allStudents.map { it.className }.distinct().sorted()
    }

    // Step state: 1, 2, 3, 4
    var currentStep by remember { mutableStateOf(1) }
    
    // Active class being configured
    var selectedClass by remember { mutableStateOf<String?>(null) }
    
    // Config states
    var isConfigured by remember { mutableStateOf(false) }
    
    // Step 2 state: Additional Subjects List
    var additionalSubjectsCount by remember { mutableStateOf(1) }
    var additionalSubjects by remember { mutableStateOf(listOf(Pair("Moral Science", 100.0))) }
    
    // Step 3 state: PA counts
    var t1PaCount by remember { mutableStateOf(2) }
    var t1MaxMarks1 by remember { mutableStateOf("20") }
    var t1MaxMarks2 by remember { mutableStateOf("20") }
    var t1MaxMarks3 by remember { mutableStateOf("20") }
    var t1Logic by remember { mutableStateOf("Average") } // Average, Best, Average of Best 2
    
    var t2PaCount by remember { mutableStateOf(2) }
    var t2MaxMarks1 by remember { mutableStateOf("20") }
    var t2MaxMarks2 by remember { mutableStateOf("20") }
    var t2MaxMarks3 by remember { mutableStateOf("20") }
    var t2Logic by remember { mutableStateOf("Average") }

    // Step 4 state: Preferences
    var printWebsite by remember { mutableStateOf(true) }
    var printAffiliation by remember { mutableStateOf(true) }
    var printLogo by remember { mutableStateOf(true) }
    var printHeightWeight by remember { mutableStateOf(true) }
    
    // Bulk Config
    var showBulkDialog by remember { mutableStateOf(false) }
    val bulkSelectedClasses = remember { mutableStateListOf<String>() }

    // Load configuration when class changes
    LaunchedEffect(selectedClass, allExamConfigs) {
        selectedClass?.let { cls ->
            val existing = allExamConfigs.find { it.className == cls }
            if (existing != null) {
                isConfigured = existing.isConfigured
                
                // Parse subjects
                val subs = mutableListOf<Pair<String, Double>>()
                if (existing.additionalSubjectsString.isNotEmpty()) {
                    existing.additionalSubjectsString.split("|").forEach { row ->
                        if (row.contains(":")) {
                            val parts = row.split(":")
                            val name = parts[0]
                            val score = parts[1].toDoubleOrNull() ?: 100.0
                            subs.add(Pair(name, score))
                        }
                    }
                }
                if (subs.isEmpty()) {
                    subs.add(Pair("Moral Science", 100.0))
                }
                additionalSubjects = subs
                additionalSubjectsCount = subs.size
                
                // PA T1
                t1PaCount = existing.t1PaCount
                t1MaxMarks1 = existing.t1PaMaxMarks1.toInt().toString()
                t1MaxMarks2 = existing.t1PaMaxMarks2.toInt().toString()
                t1MaxMarks3 = existing.t1PaMaxMarks3.toInt().toString()
                t1Logic = existing.t1CalculationLogic
                
                // PA T2
                t2PaCount = existing.t2PaCount
                t2MaxMarks1 = existing.t2PaMaxMarks1.toInt().toString()
                t2MaxMarks2 = existing.t2PaMaxMarks2.toInt().toString()
                t2MaxMarks3 = existing.t2PaMaxMarks3.toInt().toString()
                t2Logic = existing.t2CalculationLogic
                
                // Print settings
                printWebsite = existing.printSchoolWebsite
                printAffiliation = existing.printAffiliationNumber
                printLogo = existing.printBoardLogo
                printHeightWeight = existing.printHeightWeight
            } else {
                // Set default configs
                isConfigured = false
                additionalSubjects = listOf(Pair("Moral Science", 100.0))
                additionalSubjectsCount = 1
                t1PaCount = 2
                t1MaxMarks1 = "20"
                t1MaxMarks2 = "20"
                t1MaxMarks3 = "20"
                t1Logic = "Average"
                t2PaCount = 2
                t2MaxMarks1 = "20"
                t2MaxMarks2 = "20"
                t2MaxMarks3 = "20"
                t2Logic = "Average"
                printWebsite = true
                printAffiliation = true
                printLogo = true
                printHeightWeight = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exam Settings Module", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("exam_settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    // Role banner indicator
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
            // STEP PROGRESS HEADER (Visible when class is selected)
            if (selectedClass != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Configuring $selectedClass",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Step $currentStep of 4",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { currentStep / 4f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        val stepName = when(currentStep) {
                            1 -> "Select Classroom & Check Status"
                            2 -> "Configure Additional Subjects"
                            3 -> "Configure Periodic Assessments (PA)"
                            else -> "Report Card Preferences & Apply Options"
                        }
                        Text(
                            text = stepName,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // MAIN INTERACTIVE CONTENT AREA BASED ON STEPS
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (selectedClass == null) {
                    // STEP 1 FIRST STATE: Class selector
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 1: Choose Class for Configuration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        if (classesList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No classes available. Add students first to populate class list.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(classesList) { className ->
                                    val config = allExamConfigs.find { it.className == className }
                                    val isClassConfigured = config?.isConfigured ?: false
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedClass = className
                                                currentStep = 2
                                            }
                                            .testTag("class_config_item_$className"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isClassConfigured) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.School, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                Column {
                                                    Text(className, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text("Click to view or edit structure", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }

                                            // Configuration Badge
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (isClassConfigured) Color(0xFFE6F4EA) else Color(0xFFF1F3F4),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                if (isClassConfigured) Color(0xFF137333) else Color(0xFF70757A),
                                                                CircleShape
                                                            )
                                                    )
                                                    Text(
                                                        text = if (isClassConfigured) "Configured" else "Pending",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isClassConfigured) Color(0xFF137333) else Color(0xFF70757A)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // SUB-STEPS WIZARD PANEL IF CLASS IS SELECTED
                    when (currentStep) {
                        2 -> {
                            // STEP 2: ADDITIONAL SUBJECTS CONFIGURATION
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Additional Subjects (evaluated only in Term-End exams)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Number of extra subjects (0-8):", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    
                                    IconButton(
                                        onClick = { if (additionalSubjectsCount > 0) additionalSubjectsCount-- },
                                        enabled = additionalSubjectsCount > 0 && activeRole != "Principal/Coordinator",
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Remove, contentDescription = "")
                                    }
                                    
                                    Text(
                                        text = additionalSubjectsCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.width(20.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    IconButton(
                                        onClick = { if (additionalSubjectsCount < 8) additionalSubjectsCount++ },
                                        enabled = additionalSubjectsCount < 8 && activeRole != "Principal/Coordinator",
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = "")
                                    }
                                }

                                // Sync size of additional subjects list to count
                                val currentList = additionalSubjects.toMutableList()
                                if (currentList.size < additionalSubjectsCount) {
                                    while (currentList.size < additionalSubjectsCount) {
                                        val idx = currentList.size + 1
                                        currentList.add(Pair("Additional Subject $idx", 50.0))
                                    }
                                } else if (currentList.size > additionalSubjectsCount) {
                                    while (currentList.size > additionalSubjectsCount) {
                                        currentList.removeAt(currentList.lastIndex)
                                    }
                                }
                                additionalSubjects = currentList

                                Divider()

                                if (additionalSubjectsCount == 0) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No additional subjects configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(additionalSubjects.size) { index ->
                                            val currentSub = additionalSubjects[index]
                                            var isRowEnabled by remember { mutableStateOf(true) }
                                            
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isRowEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Toggle checkbox to enable/disable specific subject row
                                                    Checkbox(
                                                        checked = isRowEnabled,
                                                        onCheckedChange = { isRowEnabled = it },
                                                        enabled = activeRole != "Principal/Coordinator"
                                                    )
                                                    
                                                    OutlinedTextField(
                                                        value = currentSub.first,
                                                        onValueChange = { newName ->
                                                            val updatedList = additionalSubjects.toMutableList()
                                                            updatedList[index] = Pair(newName, currentSub.second)
                                                            additionalSubjects = updatedList
                                                        },
                                                        enabled = isRowEnabled && activeRole != "Principal/Coordinator",
                                                        label = { Text("Subject Name") },
                                                        modifier = Modifier.weight(2f).testTag("add_sub_name_$index"),
                                                        singleLine = true
                                                    )

                                                    OutlinedTextField(
                                                        value = if(currentSub.second > 0) currentSub.second.toInt().toString() else "",
                                                        onValueChange = { newVal ->
                                                            val numeric = newVal.toDoubleOrNull() ?: 100.0
                                                            val updatedList = additionalSubjects.toMutableList()
                                                            updatedList[index] = Pair(currentSub.first, numeric)
                                                            additionalSubjects = updatedList
                                                        },
                                                        enabled = isRowEnabled && activeRole != "Principal/Coordinator",
                                                        label = { Text("Max Marks") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f).testTag("add_sub_max_$index"),
                                                        singleLine = true
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            // STEP 3: PERIODIC ASSESSMENT (PA) CONFIGURATION [TERM 1 & TERM 2]
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Setup Periodic Assessments (PA) to match school evaluation system for Term 1 & Term 2",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // TERM 1 SETTINGS CARD
                                item {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(imageVector = Icons.Default.EventNote, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                                                Text("Term 1 PA Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Number of PAs:", fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    (1..3).forEach { num ->
                                                        ElevatedFilterChip(
                                                            selected = t1PaCount == num,
                                                            onClick = { if (activeRole != "Principal/Coordinator") t1PaCount = num },
                                                            label = { Text(num.toString()) },
                                                            modifier = Modifier.testTag("t1_pa_chip_$num")
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                OutlinedTextField(
                                                    value = t1MaxMarks1,
                                                    onValueChange = { t1MaxMarks1 = it },
                                                    enabled = t1PaCount >= 1 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 1 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t1_pa1_marks"),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = t1MaxMarks2,
                                                    onValueChange = { t1MaxMarks2 = it },
                                                    enabled = t1PaCount >= 2 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 2 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t1_pa2_marks"),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = t1MaxMarks3,
                                                    onValueChange = { t1MaxMarks3 = it },
                                                    enabled = t1PaCount >= 3 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 3 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t1_pa3_marks"),
                                                    singleLine = true
                                                )
                                            }

                                            Divider()

                                            // Calculation Mode Selector
                                            Text("Calculation Logic for Report Card:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("Average", "Best", "Best of 2").forEach { mode ->
                                                    ElevatedFilterChip(
                                                        selected = t1Logic == mode,
                                                        onClick = { if (activeRole != "Principal/Coordinator") t1Logic = mode },
                                                        label = { Text(mode) },
                                                        modifier = Modifier.weight(1f).testTag("t1_logic_$mode")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // TERM 2 SETTINGS CARD
                                item {
                                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(imageVector = Icons.Default.EventNote, contentDescription = "", tint = MaterialTheme.colorScheme.primary)
                                                Text("Term 2 PA Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                Text("Number of PAs:", fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    (1..3).forEach { num ->
                                                        ElevatedFilterChip(
                                                            selected = t2PaCount == num,
                                                            onClick = { if (activeRole != "Principal/Coordinator") t2PaCount = num },
                                                            label = { Text(num.toString()) },
                                                            modifier = Modifier.testTag("t2_pa_chip_$num")
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                OutlinedTextField(
                                                    value = t2MaxMarks1,
                                                    onValueChange = { t2MaxMarks1 = it },
                                                    enabled = t2PaCount >= 1 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 1 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t2_pa1_marks"),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = t2MaxMarks2,
                                                    onValueChange = { t2MaxMarks2 = it },
                                                    enabled = t2PaCount >= 2 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 2 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t2_pa2_marks"),
                                                    singleLine = true
                                                )
                                                OutlinedTextField(
                                                    value = t2MaxMarks3,
                                                    onValueChange = { t2MaxMarks3 = it },
                                                    enabled = t2PaCount >= 3 && activeRole != "Principal/Coordinator",
                                                    label = { Text("PA 3 Max") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f).testTag("t2_pa3_marks"),
                                                    singleLine = true
                                                )
                                            }

                                            Divider()

                                            // Calculation Mode Selector
                                            Text("Calculation Logic for Report Card:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("Average", "Best", "Best of 2").forEach { mode ->
                                                    ElevatedFilterChip(
                                                        selected = t2Logic == mode,
                                                        onClick = { if (activeRole != "Principal/Coordinator") t2Logic = mode },
                                                        label = { Text(mode) },
                                                        modifier = Modifier.weight(1f).testTag("t2_logic_$mode")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        4 -> {
                            // STEP 4: PRINTING PREFERENCES & BULK CONFIG
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = "Preferences & Printing Controls", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Toggle Print Parameters for Report Cards:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = printWebsite,
                                                onCheckedChange = { if (activeRole != "Principal/Coordinator") printWebsite = it },
                                                modifier = Modifier.testTag("pref_website")
                                            )
                                            Text("Print School Website URL", fontSize = 14.sp)
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = printAffiliation,
                                                onCheckedChange = { if (activeRole != "Principal/Coordinator") printAffiliation = it },
                                                modifier = Modifier.testTag("pref_affiliation")
                                            )
                                            Text("Print Affiliation and Registered Number", fontSize = 14.sp)
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = printLogo,
                                                onCheckedChange = { if (activeRole != "Principal/Coordinator") printLogo = it },
                                                modifier = Modifier.testTag("pref_logo")
                                            )
                                            Text("Print Board/Co-curricular Affiliation Logo", fontSize = 14.sp)
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = printHeightWeight,
                                                onCheckedChange = { if (activeRole != "Principal/Coordinator") printHeightWeight = it },
                                                modifier = Modifier.testTag("pref_hw")
                                            )
                                            Text("Print Student Physical Height & Weight parameters", fontSize = 14.sp)
                                        }
                                    }
                                }

                                Divider()

                                // BULK SYNC ACCORDION TRIGGER
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Bulk Configuration Copy", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Replicate these settings to other classes instantly.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    // Load other classes
                                                    bulkSelectedClasses.clear()
                                                    classesList.forEach { if(it != selectedClass) bulkSelectedClasses.add(it) }
                                                    showBulkDialog = true
                                                },
                                                enabled = activeRole != "Principal/Coordinator",
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Bulk Options")
                                            }
                                        }
                                    }
                                }

                                if (activeRole == "Principal/Coordinator") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = "", tint = MaterialTheme.colorScheme.tertiary)
                                        Text("Viewing as School Coordinator. Changes are read-only.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Configurations approved successfully!", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("coordinator_approve_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Approve Configurations")
                                    }
                                } else {
                                    // Save Button
                                    Button(
                                        onClick = {
                                            if (activeRole != "Admin") {
                                                Toast.makeText(context, "Permission Denied: Requires Admin role to configured exams.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            
                                            // Serialize additional subjects
                                            val addSubsStr = additionalSubjects.joinToString("|") { "${it.first}:${it.second}" }
                                            
                                            val finalConfig = ExamConfig(
                                                className = selectedClass!!,
                                                isConfigured = true,
                                                additionalSubjectsString = addSubsStr,
                                                t1PaCount = t1PaCount,
                                                t1PaMaxMarks1 = t1MaxMarks1.toDoubleOrNull() ?: 20.0,
                                                t1PaMaxMarks2 = t1MaxMarks2.toDoubleOrNull() ?: 20.0,
                                                t1PaMaxMarks3 = t1MaxMarks3.toDoubleOrNull() ?: 20.0,
                                                t1CalculationLogic = t1Logic,
                                                t2PaCount = t2PaCount,
                                                t2PaMaxMarks1 = t2MaxMarks1.toDoubleOrNull() ?: 20.0,
                                                t2PaMaxMarks2 = t2MaxMarks2.toDoubleOrNull() ?: 20.0,
                                                t2PaMaxMarks3 = t2MaxMarks3.toDoubleOrNull() ?: 20.0,
                                                t2CalculationLogic = t2Logic,
                                                printSchoolWebsite = printWebsite,
                                                printAffiliationNumber = printAffiliation,
                                                printBoardLogo = printLogo,
                                                printHeightWeight = printHeightWeight
                                            )
                                            
                                            viewModel.saveExamConfig(finalConfig)
                                            Toast.makeText(context, "Config recorded for $selectedClass", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("save_config_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save and Apply Configuration")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // NAVIGATION BUTTONS WIZARD FOOTER
            if (selectedClass != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            if (currentStep > 2) {
                                currentStep--
                            } else {
                                selectedClass = null
                                currentStep = 1
                            }
                        },
                        modifier = Modifier.testTag("wizard_prev_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }
                    }

                    if (currentStep < 4) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.testTag("wizard_next_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Next Step")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "")
                            }
                        }
                    }
                }
            }
        }
        
        // BULK CONFIG MODAL
        if (showBulkDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDialog = false },
                title = { Text("Bulk Copy Preferences", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Choose which other classes will copy the settings of $selectedClass instantly:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Warning: This will overwrite any existing configurations for the selected classes.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        classesList.filter { it != selectedClass }.forEach { otherClass ->
                            val isChecked = bulkSelectedClasses.contains(otherClass)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) bulkSelectedClasses.add(otherClass) else bulkSelectedClasses.remove(otherClass)
                                    }
                                )
                                Text(otherClass)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            // Copy in bulk
                            val addSubsStr = additionalSubjects.joinToString("|") { "${it.first}:${it.second}" }
                            val bulkConfigs = bulkSelectedClasses.map { cls ->
                                ExamConfig(
                                    className = cls,
                                    isConfigured = true,
                                    additionalSubjectsString = addSubsStr,
                                    t1PaCount = t1PaCount,
                                    t1PaMaxMarks1 = t1MaxMarks1.toDoubleOrNull() ?: 20.0,
                                    t1PaMaxMarks2 = t1MaxMarks2.toDoubleOrNull() ?: 20.0,
                                    t1PaMaxMarks3 = t1MaxMarks3.toDoubleOrNull() ?: 20.0,
                                    t1CalculationLogic = t1Logic,
                                    t2PaCount = t2PaCount,
                                    t2PaMaxMarks1 = t2MaxMarks1.toDoubleOrNull() ?: 20.0,
                                    t2PaMaxMarks2 = t2MaxMarks2.toDoubleOrNull() ?: 20.0,
                                    t2PaMaxMarks3 = t2MaxMarks3.toDoubleOrNull() ?: 20.0,
                                    t2CalculationLogic = t2Logic,
                                    printSchoolWebsite = printWebsite,
                                    printAffiliationNumber = printAffiliation,
                                    printBoardLogo = printLogo,
                                    printHeightWeight = printHeightWeight
                                )
                            }
                            viewModel.saveExamConfigsBulk(bulkConfigs)
                            Toast.makeText(context, "Copied settings to ${bulkSelectedClasses.size} classes", Toast.LENGTH_SHORT).show()
                            showBulkDialog = false
                        }
                    ) {
                        Text("Overwrite & Core Copy")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
