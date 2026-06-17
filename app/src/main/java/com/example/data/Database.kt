package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "school_settings")
data class SchoolSetting(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "Global Academy",
    val session: String = "2025 - 2026",
    val logoEmoji: String = "🏫",
    val logoColorHex: String = "#4F46E5", // Violet primary
    val location: String = "New Delhi, India",
    val principalSignature: String = "",
    val teacherSignature: String = "",
    val contactNumber: String = "9XXXXXXXXX",
    val affiliationNumber: String = "",
    val schoolLogoBase64: String = ""
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rollNumber: String,
    val className: String,
    val sectionName: String,
    val fatherName: String = "",
    val motherName: String = "",
    val admissionNumber: String? = "",
    val mobileNumber: String? = ""
)

@Entity(
    tableName = "marks",
    primaryKeys = ["studentId", "subjectName", "termName", "examType"]
)
data class Mark(
    val studentId: Int,
    val subjectName: String,
    val marksObtained: Double,
    val termName: String = "Term 1",
    val examType: String = "Term Exam",
    val maxMarks: Double = 100.0
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String, // ISO date string
    val status: String, // "Present", "Absent", "Leave"
    val termName: String = "Term 1"
)

@Entity(tableName = "discipline_records")
data class DisciplineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String,
    val incidentDescription: String,
    val grade: String, // "A", "B", "C"
    val termName: String = "Term 1"
)

@Entity(tableName = "exam_configs")
data class ExamConfig(
    @PrimaryKey val className: String, // e.g. "Grade 10"
    val isConfigured: Boolean = false,
    
    // Step 2: Additional subjects (serialize as: "Subject Name:Max Marks|Subject Name:Max Marks")
    val additionalSubjectsString: String = "", 
    
    // Step 3: PA Settings Term 1
    val t1PaCount: Int = 1, // 1 to 3
    val t1PaMaxMarks1: Double = 20.0,
    val t1PaMaxMarks2: Double = 20.0,
    val t1PaMaxMarks3: Double = 20.0,
    val t1CalculationLogic: String = "Average", // "Average", "Best", "Best of Choice"
    
    // Step 3: PA Settings Term 2
    val t2PaCount: Int = 1, // 1 to 3
    val t2PaMaxMarks1: Double = 20.0,
    val t2PaMaxMarks2: Double = 20.0,
    val t2PaMaxMarks3: Double = 20.0,
    val t2CalculationLogic: String = "Average", // "Average", "Best", "Best of Choice"
    
    // Step 4: Printing Preferences
    val printSchoolWebsite: Boolean = true,
    val printAffiliationNumber: Boolean = true,
    val printBoardLogo: Boolean = true,
    val printHeightWeight: Boolean = true,

    // Image 2 Parameters: Internal assessment components and values
    val hasMultipleAssessment: Boolean = false,
    val multipleAssessmentMarks: Double = 5.0,
    val hasNotebookSubmission: Boolean = true,
    val notebookSubmissionMarks: Double = 5.0,
    val hasSubjectEnrichment: Boolean = true,
    val subjectEnrichmentMarks: Double = 5.0,
    val hasPaWeightage: Boolean = true,
    val paWeightageMarks: Double = 10.0
)

@Dao
interface ExamConfigDao {
    @Query("SELECT * FROM exam_configs ORDER BY className ASC")
    fun getAllExamConfigsFlow(): Flow<List<ExamConfig>>

    @Query("SELECT * FROM exam_configs WHERE className = :className LIMIT 1")
    suspend fun getExamConfigForClass(className: String): ExamConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: ExamConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBulk(configs: List<ExamConfig>)
}

@Dao
interface SchoolSettingDao {
    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SchoolSetting?>

    @Query("SELECT * FROM school_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): SchoolSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: SchoolSetting)
}

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY className, sectionName, name ASC")
    fun getAllStudentsFlow(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE className = :className AND sectionName = :section ORDER BY name ASC")
    fun getStudentsByClassAndSectionFlow(className: String, section: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE className = :className AND sectionName = :section ORDER BY name ASC")
    suspend fun getStudentsByClassAndSection(className: String, section: String): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Int): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: Student): Long

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteById(studentId: Int)
}

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks")
    fun getAllMarksFlow(): Flow<List<Mark>>

    @Query("SELECT * FROM marks WHERE studentId = :studentId")
    fun getMarksForStudentFlow(studentId: Int): Flow<List<Mark>>

    @Query("SELECT * FROM marks WHERE studentId = :studentId")
    suspend fun getMarksForStudent(studentId: Int): List<Mark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mark: Mark)

    @Query("DELETE FROM marks WHERE studentId = :studentId AND subjectName = :subjectName")
    suspend fun deleteMark(studentId: Int, subjectName: String)

    @Query("DELETE FROM marks WHERE studentId = :studentId")
    suspend fun deleteMarksForStudent(studentId: Int)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND termName = :termName")
    fun getAttendanceForStudentFlow(studentId: Int, termName: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND termName = :termName")
    suspend fun getAttendanceForStudent(studentId: Int, termName: String): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)
}

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM discipline_records")
    fun getAllDisciplineFlow(): Flow<List<DisciplineRecord>>

    @Query("SELECT * FROM discipline_records WHERE studentId = :studentId AND termName = :termName")
    fun getDisciplineForStudentFlow(studentId: Int, termName: String): Flow<List<DisciplineRecord>>

    @Query("SELECT * FROM discipline_records WHERE studentId = :studentId AND termName = :termName")
    suspend fun getDisciplineForStudent(studentId: Int, termName: String): List<DisciplineRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DisciplineRecord)

    @Delete
    suspend fun delete(record: DisciplineRecord)
}

@Entity(
    tableName = "section_subjects",
    primaryKeys = ["className", "sectionName", "subjectName"]
)
data class SectionSubject(
    val className: String,
    val sectionName: String,
    val subjectName: String,
    val maxMarks: Double = 100.0
)

@Dao
interface SectionSubjectDao {
    @Query("SELECT * FROM section_subjects ORDER BY className, sectionName, subjectName ASC")
    fun getAllSectionSubjectsFlow(): Flow<List<SectionSubject>>

    @Query("SELECT * FROM section_subjects WHERE className = :className AND sectionName = :section ORDER BY subjectName ASC")
    fun getSubjectsForSectionFlow(className: String, section: String): Flow<List<SectionSubject>>

    @Query("SELECT * FROM section_subjects WHERE className = :className AND sectionName = :section ORDER BY subjectName ASC")
    suspend fun getSubjectsForSection(className: String, section: String): List<SectionSubject>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(subject: SectionSubject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBulk(subjects: List<SectionSubject>)

    @Delete
    suspend fun delete(subject: SectionSubject)

    @Query("DELETE FROM section_subjects WHERE className = :className AND sectionName = :section AND subjectName = :subjectName")
    suspend fun deleteByKeys(className: String, section: String, subjectName: String)
}

@Database(
    entities = [SchoolSetting::class, Student::class, Mark::class, ExamConfig::class, SectionSubject::class, AttendanceRecord::class, DisciplineRecord::class],
    version = 9, // Incremented version to add Admission and Mobile fields to Student
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val schoolSettingDao: SchoolSettingDao
    abstract val studentDao: StudentDao
    abstract val markDao: MarkDao
    abstract val examConfigDao: ExamConfigDao
    abstract val sectionSubjectDao: SectionSubjectDao
    abstract val attendanceDao: AttendanceDao
    abstract val disciplineDao: DisciplineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_result_maker_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
