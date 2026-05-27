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
import com.example.data.Mark
import com.example.data.SchoolSetting
import com.example.data.Student
import com.example.viewmodel.AppViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultGeneratorScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allStudents by viewModel.allStudents.collectAsState()
    val allMarks by viewModel.allMarks.collectAsState()
    val schoolSetting by viewModel.schoolSetting.collectAsState()

    // Aggregate unique classes
    val classesList = remember(allStudents) {
        allStudents.map { Pair(it.className, it.sectionName) }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
    }

    var selectedClassSection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    var classMenuExpanded by remember { mutableStateOf(false) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

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

    // Load selected student mark lists
    val studentMarks = remember(selectedStudent, allMarks) {
        if (selectedStudent != null) {
            allMarks.filter { it.studentId == selectedStudent!!.id }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Card Architect", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("result_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // SELECTORS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Class & Section Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { classMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("class_menu_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = selectedClassSection?.let { "${it.first} - ${it.second}" } ?: "Select Class",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand")
                        }
                    }

                    DropdownMenu(
                        expanded = classMenuExpanded,
                        onDismissRequest = { classMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
                    ) {
                        classesList.forEach { pair ->
                            DropdownMenuItem(
                                text = { Text("${pair.first} - Sect. ${pair.second}") },
                                onClick = {
                                    selectedClassSection = pair
                                    classMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Student Selector
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { studentMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("student_menu_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = classFilteredStudents.isNotEmpty()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = selectedStudent?.name ?: "Select Student",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand")
                        }
                    }

                    DropdownMenu(
                        expanded = studentMenuExpanded,
                        onDismissRequest = { studentMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.45f)
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

            // REPORT CARD VISUAL ENGINE
            if (selectedStudent != null) {
                val student = selectedStudent!!

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("report_card_view_container"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // School Letterhead Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = schoolSetting.logoEmoji, fontSize = 24.sp)
                            }

                            Column {
                                Text(
                                    text = schoolSetting.schoolName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Academic Session ${schoolSetting.session}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Student Details Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DetailLabelPair("Student Name", student.name)
                                DetailLabelPair("Scholar ID", student.rollNumber)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DetailLabelPair("Class/Level", student.className)
                                DetailLabelPair("Roster Section", student.sectionName)
                            }
                        }

                        // Marks Tabular View
                        Text(
                            text = "OFFICIAL TRANSCRIPT SHEET",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (studentMarks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                                    Text("No marks found. Use Marks Entry to add scores.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        } else {
                            val totalObtained = studentMarks.sumOf { it.marksObtained }
                            val totalMax = studentMarks.sumOf { it.maxMarks }
                            val overallPercentage = if (totalMax > 0) (totalObtained / totalMax) * 100.0 else 0.0
                            val passStatus = if (overallPercentage >= 40.0) "PASSED" else "NEEDS RE-EVALUATION"

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Table Header Row
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Subject", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                        Text("Score", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text("Grade", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                }

                                items(studentMarks) { mark ->
                                    val percent = (mark.marksObtained / mark.maxMarks) * 100.0
                                    val grade = computeGrade(percent)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(mark.subjectName, fontSize = 13.sp, modifier = Modifier.weight(2f))
                                        Text("${mark.marksObtained.toInt()}/${mark.maxMarks.toInt()}", fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text(grade, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = if (grade == "F") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // Overall aggregate summary block
                                item {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = if (passStatus == "PASSED") Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Aggregate Totals:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                            Text("${totalObtained.toInt()} / ${totalMax.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Overall Percentage:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                            Text(String.format("%.1f%%", overallPercentage), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Result Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                            Text(
                                                text = passStatus,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (passStatus == "PASSED") Color(0xFF137333) else Color(0xFFC5221F)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ACTION BUTTONS (WORKING DOWNLOAD AND PRINT)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // working download button inside card
                            Button(
                                onClick = {
                                    if (studentMarks.isEmpty()) {
                                        Toast.makeText(context, "No marks available to save", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    saveResultAsTextFile(context, schoolSetting, student, studentMarks)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("download_result_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download Report")
                                }
                            }

                            // Dynamic Print/PDF generation
                            Button(
                                onClick = {
                                    if (studentMarks.isEmpty()) {
                                        Toast.makeText(context, "No transcripts to compile PDF", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    printResultPdf(context, schoolSetting, student, studentMarks)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("print_pdf_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(imageVector = Icons.Default.Print, contentDescription = "Generate PDF")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Print / Save PDF")
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select class & student to inspect academic portfolios.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun DetailLabelPair(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

fun computeGrade(percentage: Double): String {
    return when {
        percentage >= 90.0 -> "A+"
        percentage >= 80.0 -> "A"
        percentage >= 70.0 -> "B+"
        percentage >= 60.0 -> "B"
        percentage >= 50.0 -> "C"
        percentage >= 40.0 -> "D"
        else -> "F"
    }
}

// REAL IMPLEMENTATION OF THE DOWNLOAD FUNCTIONALITY (Saves beautiful formatted academic transcript locally)
fun saveResultAsTextFile(context: Context, school: SchoolSetting, student: Student, marks: List<Mark>) {
    try {
        val totalObtained = marks.sumOf { it.marksObtained }
        val totalMax = marks.sumOf { it.maxMarks }
        val overallPercent = (totalObtained / totalMax) * 100.0

        val dataString = StringBuilder().apply {
            append("=========================================\n")
            append("      ACADEMIC TRANSCRIPT REPORT CARD    \n")
            append("=========================================\n")
            append("SCHOOL: ${school.schoolName}\n")
            append("SESSION: ${school.session}\n")
            append("-----------------------------------------\n")
            append("STUDENT: ${student.name}\n")
            append("SCHOLAR ID: ${student.rollNumber}\n")
            append("CLASS: ${student.className} - SECTION ${student.sectionName}\n")
            append("-----------------------------------------\n")
            append(String.format("%-20s %-10s %-10s\n", "Subject", "Score", "Grade"))
            marks.forEach { mark ->
                val p = (mark.marksObtained / mark.maxMarks) * 100.0
                append(String.format("%-20s %-10s %-10s\n", mark.subjectName, "${mark.marksObtained.toInt()}/${mark.maxMarks.toInt()}", computeGrade(p)))
            }
            append("-----------------------------------------\n")
            append(String.format("Aggregate Totals: %d/%d\n", totalObtained.toInt(), totalMax.toInt()))
            append(String.format("Overall Percentage: %.1f%%\n", overallPercent))
            append("STATUS: ${if (overallPercent >= 40.0) "PASSED" else "NEEDS RE-EVALUATION"}\n")
            append("=========================================\n")
            append("Authorized Signature: Principal Office\n")
        }.toString()

        // Write directly to app private documents directory
        val fileName = "${student.name.replace(" ", "_")}_report_card.txt"
        val directory = context.getExternalFilesDir(null)
        val file = File(directory, fileName)
        FileOutputStream(file).use { out ->
            out.write(dataString.toByteArray())
        }

        Toast.makeText(
            context,
            "Success! Saved transcript locally to Documents directory as a raw text file.",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving transcript: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// REAL IMPLEMENTATION OF THE SYSTEM PDF GENERATION FLOW (Spawns default print view)
fun printResultPdf(context: Context, school: SchoolSetting, student: Student, marks: List<Mark>) {
    try {
        val totalObtained = marks.sumOf { it.marksObtained }
        val totalMax = marks.sumOf { it.maxMarks }
        val overallPercent = (totalObtained / totalMax) * 100.0
        val passStatus = if (overallPercent >= 40.0) "PASSED" else "NEEDS RE-EVALUATION"

        val marksRows = StringBuilder()
        marks.forEach { mark ->
            val p = (mark.marksObtained / mark.maxMarks) * 100.0
            val grade = computeGrade(p)
            marksRows.append("""
                <tr>
                    <td>${mark.subjectName}</td>
                    <td style="text-align: right;">${mark.marksObtained.toInt()} / ${mark.maxMarks.toInt()}</td>
                    <td style="text-align: right; font-weight: bold; color: ${if (grade == "F") "#C5221F" else "#4F46E5"};">$grade</td>
                </tr>
            """.trimIndent())
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; padding: 30px; color: #333; }
                    .header { text-align: center; border-bottom: 3px double #ddd; padding-bottom: 10px; margin-bottom: 20.dp; }
                    .school-logo { font-size: 50px; margin: 0; }
                    .school-name { font-size: 26px; font-weight: bold; margin: 5px 0 2px 0; text-transform: uppercase; letter-spacing: 1px; }
                    .session { font-size: 14px; color: #666; margin: 0; }
                    .title { font-size: 18px; font-weight: bold; margin: 15px 0; text-align: center; text-decoration: underline; color: #4F46E5; }
                    .student-info { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
                    .student-info td { padding: 6px 12px; border: 1px solid #eee; }
                    .student-info .label { font-weight: bold; color: #555; background-color: #fcfcfc; width: 25%; }
                    .marks-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
                    .marks-table th { background-color: #4F46E5; color: white; padding: 10px; text-align: left; font-size: 14px; }
                    .marks-table td { padding: 10px; border-bottom: 1px solid #ddd; font-size: 13px; }
                    .marks-table tr:nth-child(even) { background-color: #f9f9f9; }
                    .summary-box { background-color: ${if (passStatus == "PASSED") "#E6F4EA" else "#FCE8E6"}; border: 1px solid #ddd; padding: 15px; border-radius: 8px; margin-bottom: 30px; }
                    .summary-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
                    .summary-row.total { font-size: 16px; font-weight: bold; border-top: 1px solid #ccc; padding-top: 8px; margin-top: 5px; }
                    .status-tag { font-weight: bold; color: ${if (passStatus == "PASSED") "#137333" else "#C5221F"}; }
                    .signatures { display: flex; justify-content: space-between; margin-top: 60px; font-size: 13px; }
                    .sig-line { border-top: 1px solid #888; width: 200px; text-align: center; padding-top: 5px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="school-logo">${school.logoEmoji}</div>
                    <div class="school-name">${school.schoolName}</div>
                    <div class="session">Academic Year Session ${school.session}</div>
                </div>
                
                <div class="title">ACADEMIC PERFORMANCE TRANSCRIPT</div>
                
                <table class="student-info">
                    <tr>
                        <td class="label">Student Name</td>
                        <td>${student.name}</td>
                        <td class="label">Class / Grade</td>
                        <td>${student.className}</td>
                    </tr>
                    <tr>
                        <td class="label">Scholar ID</td>
                        <td>${student.rollNumber}</td>
                        <td class="label">Roster Section</td>
                        <td>${student.sectionName}</td>
                    </tr>
                </table>
                
                <table class="marks-table">
                    <thead>
                        <tr>
                            <th style="width: 50%;">Subject Module</th>
                            <th style="width: 25%; text-align: right;">Marks Obtained / Max</th>
                            <th style="width: 25%; text-align: right;">Letter Grade</th>
                        </tr>
                    </thead>
                    <tbody>
                        $marksRows
                    </tbody>
                </table>
                
                <div class="summary-box">
                    <div class="summary-row">
                        <span>Aggregate Totals:</span>
                        <strong>${totalObtained.toInt()} / ${totalMax.toInt()}</strong>
                    </div>
                    <div class="summary-row">
                        <span>Cumulative Percentage Marks:</span>
                        <strong>${String.format("%.1f%%", overallPercent)}</strong>
                    </div>
                    <div class="summary-row total">
                        <span>Academic Standing Status:</span>
                        <span class="status-tag">$passStatus</span>
                    </div>
                </div>
                
                <div class="signatures">
                    <div class="sig-line">Class Coordinator</div>
                    <div class="sig-line">Head Principal / Director</div>
                </div>
            </body>
            </html>
        """.trimIndent()

        // Run on main thread to initiate Webview Print process
        (context as? android.app.Activity)?.runOnUiThread {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "${school.schoolName.replace(" ", "_")}_Result_${student.name.replace(" ", "_")}"
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
