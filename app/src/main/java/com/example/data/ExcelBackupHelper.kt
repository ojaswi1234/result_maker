package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import jxl.Workbook
import jxl.WorkbookSettings
import jxl.write.Label
import jxl.write.WritableWorkbook
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelBackupHelper {
    private const val TAG = "ExcelBackupHelper"
    private const val BACKUP_FILE_NAME = "Live_App_Data_Backup.csv"
    private val mutex = Mutex()

    // Helper to escape CSV values according to RFC 4180
    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }

    /**
     * Performs a live export of all database tables.
     * Appends a complete diagnostic snapshot to the CSV backup file.
     * If an error occurs, it appends a dedicated error log row.
     */
    suspend fun performLiveBackup(
        context: Context,
        database: AppDatabase,
        operationType: String,
        errorMsg: String? = null
    ) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
            val backupFile = File(context.getExternalFilesDir(null), BACKUP_FILE_NAME)
            val fileExists = backupFile.exists()

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timestamp = sdf.format(Date())

            val fos = FileOutputStream(backupFile, true)
            // Use UTF-8 with BOM so Microsoft Excel processes emojis/Unicode characters correctly out-of-the-box
            val writer = OutputStreamWriter(fos, "UTF-8")
            if (!fileExists) {
                // Write Excel BOM
                writer.write("\uFEFF")
                // Define Header Row
                val headers = listOf(
                    "Date & Time",
                    "Event/Operation Type",
                    "Entity Type",
                    "ID/Key",
                    "Field 1 (Name/Key)",
                    "Field 2 (Value/Roll)",
                    "Field 3 (Class/Marks)",
                    "Field 4 (Section/Term)",
                    "Field 5 (Extra/Type)",
                    "Field 6 (Extra)",
                    "Status",
                    "Error Message"
                )
                writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")
            }

            if (errorMsg != null) {
                // Log the explicit error
                val errorRow = listOf(
                    timestamp,
                    operationType,
                    "ERROR_LOG",
                    "",
                    "Error Occurred",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "ERROR",
                    errorMsg
                )
                writer.write(errorRow.joinToString(",") { escapeCsv(it) } + "\n")
                writer.flush()
                writer.close()
                return@withContext
            }

            // Fetch a fresh snapshot of all database tables
            val schoolSetting = database.schoolSettingDao.getSettings() ?: SchoolSetting()
            val students = database.studentDao.getAllStudentsFlow().first()
            val marks = database.markDao.getAllMarksFlow().first()
            val examConfigs = database.examConfigDao.getAllExamConfigsFlow().first()
            val sectionSubjects = database.sectionSubjectDao.getAllSectionSubjectsFlow().first()
            val attendanceRecords = database.attendanceDao.getAllAttendanceFlow().first()
            val disciplineRecords = database.disciplineDao.getAllDisciplineFlow().first()

            // 1. Export School Settings snapshot
            val schoolSettingRow = listOf(
                timestamp,
                operationType,
                "SchoolSetting",
                schoolSetting.id.toString(),
                schoolSetting.schoolName,
                schoolSetting.session,
                schoolSetting.location,
                schoolSetting.logoEmoji,
                schoolSetting.logoColorHex,
                "Has Signature: " + (schoolSetting.principalSignature.isNotEmpty() && schoolSetting.teacherSignature.isNotEmpty()),
                "SUCCESS",
                ""
            )
            writer.write(schoolSettingRow.joinToString(",") { escapeCsv(it) } + "\n")

            // 2. Export Students snapshots
            if (students.isEmpty()) {
                val studentRow = listOf(
                    timestamp,
                    operationType,
                    "Student",
                    "EMPTY",
                    "No students registered yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(studentRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (student in students) {
                    val studentRow = listOf(
                        timestamp,
                        operationType,
                        "Student",
                        student.id.toString(),
                        student.name,
                        student.rollNumber,
                        student.className,
                        student.sectionName,
                        "",
                        "",
                        "SUCCESS",
                        ""
                    )
                    writer.write(studentRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            // 3. Export Marks snapshots
            if (marks.isEmpty()) {
                val markRow = listOf(
                    timestamp,
                    operationType,
                    "Mark",
                    "EMPTY",
                    "No marks entered yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(markRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (mark in marks) {
                    val markRow = listOf(
                        timestamp,
                        operationType,
                        "Mark",
                        "${mark.studentId}_${mark.subjectName}_${mark.termName}",
                        mark.studentId.toString(),
                        mark.subjectName,
                        mark.marksObtained.toString(),
                        mark.termName,
                        mark.examType,
                        mark.maxMarks.toString(),
                        "SUCCESS",
                        ""
                    )
                    writer.write(markRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            // 4. Export Section Subjects snapshots
            if (sectionSubjects.isEmpty()) {
                val subjectRow = listOf(
                    timestamp,
                    operationType,
                    "SectionSubject",
                    "EMPTY",
                    "No subjects mapped yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(subjectRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (sub in sectionSubjects) {
                    val subjectRow = listOf(
                        timestamp,
                        operationType,
                        "SectionSubject",
                        "${sub.className}_${sub.sectionName}_${sub.subjectName}",
                        sub.className,
                        sub.sectionName,
                        sub.subjectName,
                        sub.maxMarks.toString(),
                        "",
                        "",
                        "SUCCESS",
                        ""
                    )
                    writer.write(subjectRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            // 5. Export Exam Configurations snapshots
            if (examConfigs.isEmpty()) {
                val configRow = listOf(
                    timestamp,
                    operationType,
                    "ExamConfig",
                    "EMPTY",
                    "No class exam configs defined yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(configRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (config in examConfigs) {
                    val configRow = listOf(
                        timestamp,
                        operationType,
                        "ExamConfig",
                        config.className,
                        config.isConfigured.toString(),
                        config.t1CalculationLogic,
                        config.t2CalculationLogic,
                        config.additionalSubjectsString,
                        config.paWeightageMarks.toString(),
                        "hasMultipleAssessment: ${config.hasMultipleAssessment}",
                        "SUCCESS",
                        ""
                    )
                    writer.write(configRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            // 6. Export Attendance snapshots
            if (attendanceRecords.isEmpty()) {
                val attendanceRow = listOf(
                    timestamp,
                    operationType,
                    "AttendanceRecord",
                    "EMPTY",
                    "No attendance records yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(attendanceRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (record in attendanceRecords) {
                    val attendanceRow = listOf(
                        timestamp,
                        operationType,
                        "AttendanceRecord",
                        record.id.toString(),
                        record.studentId.toString(),
                        record.date,
                        record.status,
                        record.termName,
                        "",
                        "",
                        "SUCCESS",
                        ""
                    )
                    writer.write(attendanceRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            // 7. Export Discipline snapshots
            if (disciplineRecords.isEmpty()) {
                val disciplineRow = listOf(
                    timestamp,
                    operationType,
                    "DisciplineRecord",
                    "EMPTY",
                    "No discipline records yet",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "SUCCESS",
                    ""
                )
                writer.write(disciplineRow.joinToString(",") { escapeCsv(it) } + "\n")
            } else {
                for (record in disciplineRecords) {
                    val disciplineRow = listOf(
                        timestamp,
                        operationType,
                        "DisciplineRecord",
                        record.id.toString(),
                        record.studentId.toString(),
                        record.date,
                        record.incidentDescription,
                        record.grade,
                        record.termName,
                        "",
                        "SUCCESS",
                        ""
                    )
                    writer.write(disciplineRow.joinToString(",") { escapeCsv(it) } + "\n")
                }
            }

            writer.flush()
            writer.close()
            Log.d(TAG, "Database successfully backed up dynamically to $BACKUP_FILE_NAME")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write backup snapshot: ${e.message}", e)
        }
            }
        }
    }

    /**
     * Generates a true Microsoft Excel (.xls) file for a specific class section roster.
     */
    fun generateClassRosterExcel(
        context: Context,
        className: String,
        sectionName: String,
        students: List<Student>
    ): File? {
        return try {
            val fileName = "Roster_${className}_${sectionName}.xls".replace(" ", "_")
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val wbSettings = WorkbookSettings()
            wbSettings.locale = Locale.getDefault()
            
            val workbook: WritableWorkbook = Workbook.createWorkbook(file, wbSettings)
            val sheet = workbook.createSheet("Class Roster", 0)

            val headers = listOf("Roll Number", "Admission No", "Student Name", "Father's Name", "Mother's Name", "Mobile No", "Class", "Section")
            headers.forEachIndexed { index, header ->
                sheet.addCell(Label(index, 0, header))
            }

            students.forEachIndexed { rowIndex, student ->
                sheet.addCell(Label(0, rowIndex + 1, student.rollNumber))
                sheet.addCell(Label(1, rowIndex + 1, student.admissionNumber ?: ""))
                sheet.addCell(Label(2, rowIndex + 1, student.name))
                sheet.addCell(Label(3, rowIndex + 1, student.fatherName))
                sheet.addCell(Label(4, rowIndex + 1, student.motherName))
                sheet.addCell(Label(5, rowIndex + 1, student.mobileNumber ?: ""))
                sheet.addCell(Label(6, rowIndex + 1, student.className))
                sheet.addCell(Label(7, rowIndex + 1, student.sectionName))
            }

            workbook.write()
            workbook.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating roster Excel", e)
            null
        }
    }

    /**
     * Generates a true Microsoft Excel (.xls) file for attendance records of a class section.
     */
    fun exportAttendanceToExcel(
        context: Context,
        className: String,
        sectionName: String,
        attendanceRecords: List<AttendanceRecord>,
        students: List<Student>
    ): File? {
        return try {
            val fileName = "Attendance_${className}_${sectionName}.xls".replace(" ", "_")
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val wbSettings = WorkbookSettings()
            wbSettings.locale = Locale.getDefault()
            
            val workbook: WritableWorkbook = Workbook.createWorkbook(file, wbSettings)
            val sheet = workbook.createSheet("Attendance Log", 0)

            // Add Title Row
            sheet.addCell(Label(0, 0, "Class: $className - Section: $sectionName"))

            val headers = listOf("Date", "Student Name", "Roll No", "Status", "Term", "Total Classes Attended", "Total Classes")
            headers.forEachIndexed { index, header ->
                sheet.addCell(Label(index, 1, header))
            }

            // Pre-calculate aggregates for efficiency
            val studentAggregates = attendanceRecords.groupBy { it.studentId }.mapValues { (_, records) ->
                val attended = records.count { it.status.equals("Present", ignoreCase = true) }
                val total = records.size
                Pair(attended, total)
            }

            // Map records by date and student for organized view if needed
            attendanceRecords.sortedByDescending { it.date }.forEachIndexed { rowIndex, record ->
                val student = students.find { it.id == record.studentId }
                val agg = studentAggregates[record.studentId] ?: Pair(0, 0)
                
                sheet.addCell(Label(0, rowIndex + 2, record.date))
                sheet.addCell(Label(1, rowIndex + 2, student?.name ?: "Unknown"))
                sheet.addCell(Label(2, rowIndex + 2, student?.rollNumber ?: "N/A"))
                sheet.addCell(Label(3, rowIndex + 2, record.status))
                sheet.addCell(Label(4, rowIndex + 2, record.termName))
                sheet.addCell(Label(5, rowIndex + 2, agg.first.toString()))
                sheet.addCell(Label(6, rowIndex + 2, agg.second.toString()))
            }

            workbook.write()
            workbook.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating attendance Excel", e)
            null
        }
    }

    /**
     * Downloads a file directly to the device's public Downloads folder.
     */
    fun downloadFile(context: Context, file: File) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            
            // For true "download" experience, we move the file to a public directory
            // or use DownloadManager to "add" it to the system downloads.
            // On modern Android, we can trigger a system download notification by using addCompletedDownload.
            
            downloadManager.addCompletedDownload(
                file.name,
                "Class Roster for ${file.name.substringAfter("_").substringBefore(".xls")}",
                true,
                "application/vnd.ms-excel",
                file.absolutePath,
                file.length(),
                true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering direct download", e)
            // Fallback to share if direct download fails
            shareFile(context, file)
        }
    }

    /**
     * Launches a platform share intent for a generated file.
     */
    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".xls")) "application/vnd.ms-excel" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download/Share Class Roster"))
    }
}
