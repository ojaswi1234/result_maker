package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
     * Generates a clean Excel-compatible CSV for a specific class section roster.
     */
    fun generateClassRosterExcel(
        context: Context,
        className: String,
        sectionName: String,
        students: List<Student>
    ): File? {
        return try {
            val fileName = "Roster_${className}_${sectionName}.csv".replace(" ", "_")
            val file = File(context.getExternalFilesDir(null), fileName)
            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, "UTF-8")

            // Write BOM
            writer.write("\uFEFF")

            val headers = listOf("Roll Number", "Student Name", "Father's Name", "Mother's Name", "Class", "Section")
            writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

            for (student in students) {
                val row = listOf(
                    student.rollNumber,
                    student.name,
                    student.fatherName,
                    student.motherName,
                    student.className,
                    student.sectionName
                )
                writer.write(row.joinToString(",") { escapeCsv(it) } + "\n")
            }

            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error generating roster CSV", e)
            null
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
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download/Share Class Roster"))
    }
}
