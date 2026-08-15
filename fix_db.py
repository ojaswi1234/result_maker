import re

file_path = "/workspaces/result_maker/app/src/main/java/com/example/data/Database.kt"
with open(file_path, "r") as f:
    content = f.read()
#this is just a test file
# 1. Update imports
target_import = """import androidx.room.*
import kotlinx.coroutines.flow.Flow"""
replacement_import = """import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow"""
content = content.replace(target_import, replacement_import)

# 2. Update ExamConfig fields
target_fields = """    val additionalSubjectsString: String = "", 
    
    // Step 3: PA Settings Term 1"""
replacement_fields = """    val additionalSubjectsString: String = "", 
    
    // Step 3: Main subjects
    val mainSubjectsCount: Int = 5,
    val mainSubjectsString: String = "",
    
    // Step 3: PA Settings Term 1"""
content = content.replace(target_fields, replacement_fields)

# 3. Update printHeightWeight
target_print = "val printHeightWeight: Boolean = true,"
replacement_print = "val printHeightWeight: Boolean = false,"
content = content.replace(target_print, replacement_print)

# 4. Update Database version and add migration
target_db = """@Database(
    entities = [SchoolSetting::class, Student::class, Mark::class, ExamConfig::class, SectionSubject::class, AttendanceRecord::class, DisciplineRecord::class],
    version = 9, // Incremented version to add Admission and Mobile fields to Student
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {"""
replacement_db = """@Database(
    entities = [SchoolSetting::class, Student::class, Mark::class, ExamConfig::class, SectionSubject::class, AttendanceRecord::class, DisciplineRecord::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {"""
content = content.replace(target_db, replacement_db)

target_builder = """.fallbackToDestructiveMigration()
                .build()"""
replacement_builder = """val MIGRATION_9_10 = object : Migration(9, 10) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE exam_configs ADD COLUMN mainSubjectsCount INTEGER NOT NULL DEFAULT 5")
                        database.execSQL("ALTER TABLE exam_configs ADD COLUMN mainSubjectsString TEXT NOT NULL DEFAULT ''")
                    }
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_result_maker_db"
                )
                .addMigrations(MIGRATION_9_10)
                .fallbackToDestructiveMigration()
                .build()"""

target_builder_old = """val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_result_maker_db"
                )
                .fallbackToDestructiveMigration()
                .build()"""

content = content.replace(target_builder_old, replacement_builder)

with open(file_path, "w") as f:
    f.write(content)
