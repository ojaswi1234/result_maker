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
    val logoColorHex: String = "#4F46E5" // Violet primary
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rollNumber: String,
    val className: String,
    val sectionName: String
)

@Entity(
    tableName = "marks",
    primaryKeys = ["studentId", "subjectName"]
)
data class Mark(
    val studentId: Int,
    val subjectName: String,
    val marksObtained: Double,
    val maxMarks: Double = 100.0
)

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

@Database(
    entities = [SchoolSetting::class, Student::class, Mark::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val schoolSettingDao: SchoolSettingDao
    abstract val studentDao: StudentDao
    abstract val markDao: MarkDao

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
