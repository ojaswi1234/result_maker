import re

with open("/workspaces/result_maker/app/src/main/java/com/example/ui/ExamSettingsScreen.kt", "r") as f:
    content = f.read()

# 1. Add currentStep and other state variables
target_state = """    var selectedClass by remember { mutableStateOf<String?>(null) }

    // Checkbox State Variables"""
replacement_state = """    var selectedClass by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(1) }
    
    // Step 3 State
    var mainSubjectsCount by remember { mutableStateOf("5") }
    var mainSubjectNames by remember { mutableStateOf(List(12) { "" }) }
    
    // Step 4 State
    var additionalSubjectsCount by remember { mutableStateOf("0") }
    var additionalSubjectPairs by remember { mutableStateOf(List(8) { Pair("", "") }) }
    
    // Step 5 State
    var extraClassesSelected by remember { mutableStateOf(setOf<String>()) }

    // Checkbox State Variables"""
content = content.replace(target_state, replacement_state)

# 2. Update LaunchedEffect for new fields
target_effect = """                selectedLogic = existing.t1CalculationLogic
            } else {"""
replacement_effect = """                selectedLogic = existing.t1CalculationLogic
                
                mainSubjectsCount = existing.mainSubjectsCount.toString()
                val loadedMain = existing.mainSubjectsString.split("|").filter { it.isNotEmpty() }
                mainSubjectNames = List(12) { i -> if (i < loadedMain.size) loadedMain[i] else "" }
                
                val loadedAdditional = existing.additionalSubjectsString.split("|").filter { it.isNotEmpty() }
                additionalSubjectsCount = loadedAdditional.size.toString()
                additionalSubjectPairs = List(8) { i ->
                    if (i < loadedAdditional.size) {
                        val parts = loadedAdditional[i].split(":")
                        Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                    } else {
                        Pair("", "")
                    }
                }
            } else {"""
content = content.replace(target_effect, replacement_effect)

target_effect_else = """                selectedLogic = "Average"
            }
        }
    }"""
replacement_effect_else = """                selectedLogic = "Average"
                
                mainSubjectsCount = "5"
                mainSubjectNames = List(12) { "" }
                
                additionalSubjectsCount = "0"
                additionalSubjectPairs = List(8) { Pair("", "") }
            }
        }
    }"""
content = content.replace(target_effect_else, replacement_effect_else)

# 3. Fix TopAppBar back button logic
target_back = """                        onClick = {
                            if (selectedClass != null) {
                                selectedClass = null
                            } else {
                                onBack()
                            }
                        },"""
replacement_back = """                        onClick = {
                            if (currentStep > 1) {
                                currentStep -= 1
                                if (currentStep == 1) {
                                    selectedClass = null
                                }
                            } else {
                                onBack()
                            }
                        },"""
content = content.replace(target_back, replacement_back)

# 4. Change Screen 1 to use currentStep
target_screen1 = "if (selectedClass == null) {"
replacement_screen1 = "if (currentStep == 1 && selectedClass == null) {"
content = content.replace(target_screen1, replacement_screen1)

target_class_click = ".clickable { selectedClass = className }"
replacement_class_click = ".clickable { selectedClass = className; currentStep = 2 }"
content = content.replace(target_class_click, replacement_class_click)

# 5. Change Screen 2 to check currentStep == 2
target_screen2 = "} else {"
replacement_screen2 = "} else if (currentStep == 2 && selectedClass != null) {"
content = content.replace(target_screen2, replacement_screen2)

# 6. Change NEXT button on Screen 2
target_next_screen2 = """                    // NEXT BUTTON (SOLID TEAL CYAN BACKGROUND AS IN IMAGE 1)
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
                    }"""
replacement_next_screen2 = """                    // NEXT BUTTON (SOLID TEAL CYAN BACKGROUND AS IN IMAGE 1)
                    Button(
                        onClick = {
                            currentStep = 3
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
                    }"""
content = content.replace(target_next_screen2, replacement_next_screen2)

# 7. Add Step 3, 4, 5 layouts
# I'll append them right before the final `                } \n                }\n            }\n        }\n    }\n}`
new_steps = """            } else if (currentStep == 3 && selectedClass != null) {
                // STEP 3: Main Subjects
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
                                        .background(themeCyan)
                                        .padding(10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text("Main Subjects (Marks will be awarded in PA/Term End/MA/SE/Portfolio)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                                HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                                // Count Row
                                Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("Total Main Subjects (1 to 12)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                                    Box(
                                        modifier = Modifier
                                            .weight(0.3f)
                                            .fillMaxHeight()
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = mainSubjectsCount,
                                            onValueChange = { mainSubjectsCount = it },
                                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                                val count = (mainSubjectsCount.toIntOrNull() ?: 5).coerceIn(1, 12)
                                for (i in 0 until count) {
                                    Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.3f)
                                                .fillMaxHeight()
                                                .background(Color.White)
                                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text("Subject ${i + 1}", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                                        Box(
                                            modifier = Modifier
                                                .weight(0.7f)
                                                .fillMaxHeight()
                                                .background(Color.White),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            BasicTextField(
                                                value = mainSubjectNames[i],
                                                onValueChange = { newVal ->
                                                    val newList = mainSubjectNames.toMutableList()
                                                    newList[i] = newVal
                                                    mainSubjectNames = newList
                                                },
                                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center),
                                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                                            )
                                        }
                                    }
                                    if (i < count - 1) {
                                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier.fillMaxWidth(0.45f).testTag("wizard_previous_btn"),
                                    border = BorderStroke(1.dp, themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("PREVIOUS", color = themeCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { currentStep = 4 },
                                    modifier = Modifier.fillMaxWidth(0.818f).testTag("wizard_next_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("NEXT", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (currentStep == 4 && selectedClass != null) {
                // STEP 4: Additional Subjects
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
                                        .background(themeCyan)
                                        .padding(10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text("Additional Subjects (Marks will be awarded in Term End Only)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                                HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                                // Count Row
                                Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(0.7f)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text("Total Additional Subjects (0 to 8)", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                                    Box(
                                        modifier = Modifier
                                            .weight(0.3f)
                                            .fillMaxHeight()
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicTextField(
                                            value = additionalSubjectsCount,
                                            onValueChange = { additionalSubjectsCount = it },
                                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)

                                val count = (additionalSubjectsCount.toIntOrNull() ?: 0).coerceIn(0, 8)
                                for (i in 0 until count) {
                                    Row(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(vertical = 4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.2f)
                                                .fillMaxHeight()
                                                .background(Color.White)
                                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text("Sub ${i + 1}", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                                .background(Color.White),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (additionalSubjectPairs[i].first.isEmpty()) {
                                                Text("Subject Nam", color = Color(0xFF999999), modifier = Modifier.padding(start=8.dp), fontSize=15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            BasicTextField(
                                                value = additionalSubjectPairs[i].first,
                                                onValueChange = { newVal ->
                                                    val newList = additionalSubjectPairs.toMutableList()
                                                    newList[i] = newList[i].copy(first = newVal)
                                                    additionalSubjectPairs = newList
                                                },
                                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center),
                                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                                            )
                                        }
                                        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF7A7D81)))
                                        Box(
                                            modifier = Modifier
                                                .weight(0.3f)
                                                .fillMaxHeight()
                                                .background(Color.White),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (additionalSubjectPairs[i].second.isEmpty()) {
                                                Text("Max", color = Color(0xFF999999), modifier = Modifier.padding(start=8.dp), fontSize=15.sp, fontWeight = FontWeight.Bold)
                                            }
                                            BasicTextField(
                                                value = additionalSubjectPairs[i].second,
                                                onValueChange = { newVal ->
                                                    val newList = additionalSubjectPairs.toMutableList()
                                                    newList[i] = newList[i].copy(second = newVal)
                                                    additionalSubjectPairs = newList
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center),
                                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                                            )
                                        }
                                    }
                                    if (i < count - 1) {
                                        HorizontalDivider(color = Color(0xFF7A7D81), thickness = 1.dp)
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 3 },
                                    modifier = Modifier.fillMaxWidth(0.45f).testTag("wizard_previous_btn"),
                                    border = BorderStroke(1.dp, themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("PREVIOUS", color = themeCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { currentStep = 5 },
                                    modifier = Modifier.fillMaxWidth(0.818f).testTag("wizard_next_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("NEXT", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (currentStep == 5 && selectedClass != null) {
                // STEP 5: Print Preferences + Save
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
                            // Checkboxes
                            var printSchoolWebsite by remember { mutableStateOf(true) }
                            var printAffiliationNumber by remember { mutableStateOf(true) }
                            var printBoardLogo by remember { mutableStateOf(true) }
                            var printHeightWeight by remember { mutableStateOf(false) }

                            LaunchedEffect(selectedClass) {
                                val existing = allExamConfigs.find { it.className == selectedClass }
                                if (existing != null) {
                                    printSchoolWebsite = existing.printSchoolWebsite
                                    printAffiliationNumber = existing.printAffiliationNumber
                                    printBoardLogo = existing.printBoardLogo
                                    printHeightWeight = existing.printHeightWeight
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = printSchoolWebsite, onCheckedChange = { printSchoolWebsite = it })
                                Text("Print School Website", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = printAffiliationNumber, onCheckedChange = { printAffiliationNumber = it })
                                Text("Print Affiliation Number", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = printBoardLogo, onCheckedChange = { printBoardLogo = it })
                                Text("Print Board Logo", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = printHeightWeight, onCheckedChange = { printHeightWeight = it })
                                Text("Print Height and Weight", fontSize = 14.sp)
                            }

                            // Classes selector box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFCCCCCC)),
                                color = Color.White
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Select Classes for the same Exam Settings",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    classesList.forEach { cls ->
                                        val isCurrent = cls == selectedClass
                                        val isSelected = isCurrent || extraClassesSelected.contains(cls)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (!isCurrent) {
                                                        if (checked) extraClassesSelected = extraClassesSelected + cls
                                                        else extraClassesSelected = extraClassesSelected - cls
                                                    }
                                                },
                                                enabled = !isCurrent
                                            )
                                            Text(
                                                text = cls,
                                                fontSize = 14.sp,
                                                color = if (isCurrent) Color.Gray else Color.Black
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 4 },
                                    modifier = Modifier.fillMaxWidth(0.45f).testTag("wizard_previous_btn"),
                                    border = BorderStroke(1.dp, themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("PREVIOUS", color = themeCyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val mCount = (mainSubjectsCount.toIntOrNull() ?: 5).coerceIn(1, 12)
                                        val mString = mainSubjectNames.take(mCount).joinToString("|")
                                        
                                        val aCount = (additionalSubjectsCount.toIntOrNull() ?: 0).coerceIn(0, 8)
                                        val aString = additionalSubjectPairs.take(aCount).joinToString("|") { "${it.first}:${it.second}" }
                                        
                                        val finalConfig = ExamConfig(
                                            className = selectedClass!!,
                                            isConfigured = true,
                                            mainSubjectsCount = mCount,
                                            mainSubjectsString = mString,
                                            additionalSubjectsString = aString,
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
                                            printSchoolWebsite = printSchoolWebsite,
                                            printAffiliationNumber = printAffiliationNumber,
                                            printBoardLogo = printBoardLogo,
                                            printHeightWeight = printHeightWeight,
                                            hasMultipleAssessment = hasMultipleAssessment,
                                            multipleAssessmentMarks = multipleAssessmentMarks.toDoubleOrNull() ?: 5.0,
                                            hasNotebookSubmission = hasNotebookSubmission,
                                            notebookSubmissionMarks = notebookSubmissionMarks.toDoubleOrNull() ?: 5.0,
                                            hasSubjectEnrichment = hasSubjectEnrichment,
                                            subjectEnrichmentMarks = subjectEnrichmentMarks.toDoubleOrNull() ?: 5.0,
                                            hasPaWeightage = hasPaWeightage,
                                            paWeightageMarks = paWeightageMarks.toDoubleOrNull() ?: 10.0
                                        )

                                        val configsToSave = mutableListOf(finalConfig)
                                        extraClassesSelected.forEach { extraCls ->
                                            configsToSave.add(finalConfig.copy(className = extraCls))
                                        }

                                        // viewModel needs a way to save bulk or we just loop
                                        configsToSave.forEach {
                                            viewModel.saveExamConfig(it)
                                        }
                                        
                                        Toast.makeText(context, "Configurations saved for ${configsToSave.size} classes", Toast.LENGTH_SHORT).show()
                                        
                                        selectedClass = null
                                        currentStep = 1
                                        extraClassesSelected = setOf()
                                    },
                                    modifier = Modifier.fillMaxWidth(0.818f).testTag("wizard_save_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeCyan),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("SAVE SETTINGS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
"""

# Replace closing brackets area
import sys
content = content.replace("                }\n                }\n            }\n        }\n    }\n}", new_steps + "            }\n        }\n    }\n}")

with open("/workspaces/result_maker/app/src/main/java/com/example/ui/ExamSettingsScreen.kt", "w") as f:
    f.write(content)
