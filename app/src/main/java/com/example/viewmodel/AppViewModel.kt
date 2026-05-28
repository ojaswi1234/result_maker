package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val email: String, val name: String, val photoUrl: String?) : AuthState
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SchoolRepository(database)

    // Current logged-in user state
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Active configuration details (School settings)
    val schoolSetting: StateFlow<SchoolSetting> = repository.schoolSetting
        .map { it ?: SchoolSetting() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SchoolSetting()
        )

    // All students
    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All marks
    val allMarks: StateFlow<List<Mark>> = repository.allMarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Run database initialization background job
        viewModelScope.launch {
            // Check if settings need to be initialized
            val currentSettings = repository.getSchoolSettingDirect()
            
            // Check if students are empty, if so, populate with some starter data
            repository.allStudents.first().let { students ->
                if (students.isEmpty()) {
                    createStarterData()
                }
            }
        }
    }

    private suspend fun createStarterData() {
        val starterStudents = listOf(
            Student(name = "Liam Smith", rollNumber = "S101", className = "IV - Demo", sectionName = "A"),
            Student(name = "Olivia Johnson", rollNumber = "S102", className = "IV - Demo", sectionName = "A"),
            Student(name = "Noah Williams", rollNumber = "S103", className = "IX - Demo", sectionName = "A"),
            Student(name = "Emma Brown", rollNumber = "S104", className = "IX - Demo", sectionName = "B"),
            Student(name = "Sophia Garcia", rollNumber = "S201", className = "XI - Demo", sectionName = "A"),
            Student(name = "James Martinez", rollNumber = "S202", className = "XI - Demo", sectionName = "A")
        )

        val studentIds = mutableListOf<Int>()
        for (st in starterStudents) {
            val id = repository.insertStudent(st)
            studentIds.add(id.toInt())
        }

        // Add starter marks for Liam Smith (Math: 88, Science: 92, English: 85, History: 78)
        if (studentIds.size >= 4) {
            val liamId = studentIds[0]
            val oliviaId = studentIds[1]
            val noahId = studentIds[2]
            val emmaId = studentIds[3]

            // Liam Marks
            repository.saveMark(Mark(liamId, "Mathematics", 88.0))
            repository.saveMark(Mark(liamId, "Science", 92.0))
            repository.saveMark(Mark(liamId, "English", 85.0))
            repository.saveMark(Mark(liamId, "History", 78.0))

            // Olivia Marks
            repository.saveMark(Mark(oliviaId, "Mathematics", 95.0))
            repository.saveMark(Mark(oliviaId, "Science", 89.0))
            repository.saveMark(Mark(oliviaId, "English", 94.0))
            repository.saveMark(Mark(oliviaId, "History", 91.0))

            // Noah Marks
            repository.saveMark(Mark(noahId, "Mathematics", 72.0))
            repository.saveMark(Mark(noahId, "Science", 75.0))
            repository.saveMark(Mark(noahId, "English", 80.0))
            repository.saveMark(Mark(noahId, "History", 85.0))

            // Emma Marks
            repository.saveMark(Mark(emmaId, "Mathematics", 65.0))
            repository.saveMark(Mark(emmaId, "Science", 68.0))
            repository.saveMark(Mark(emmaId, "English", 70.0))
            repository.saveMark(Mark(emmaId, "History", 72.0))
        }
    }

    // Google Sign-In Simulation
    fun signInWithGoogle(email: String, displayName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticated(
                email = email,
                name = displayName,
                photoUrl = null
            )
            onComplete()
        }
    }

    fun logout(onComplete: () -> Unit) {
        _authState.value = AuthState.Unauthenticated
        onComplete()
    }

    // Helper wrapper to execute database actions, handle live backup & catch errors in the Excel sheet
    private fun executeDbAction(operationType: String, action: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action()
                ExcelBackupHelper.performLiveBackup(getApplication(), database, operationType)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    // Record exact error message & operation type in the excel sheet with current time as requested!
                    ExcelBackupHelper.performLiveBackup(getApplication(), database, operationType, errorMsg = e.message ?: "Unknown database error")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    // Update School Settings
    fun updateSchoolDetails(name: String, session: String, location: String, emoji: String, colorHex: String) {
        executeDbAction("UPDATE_SCHOOL_DETAILS") {
            val current = repository.getSchoolSettingDirect()
            val updated = SchoolSetting(
                id = 1,
                schoolName = name,
                session = session,
                logoEmoji = emoji,
                logoColorHex = colorHex,
                location = location,
                principalSignature = current.principalSignature,
                teacherSignature = current.teacherSignature
            )
            repository.updateSchoolSetting(updated)
        }
    }

    // Update Principal Signature
    fun updatePrincipalSignature(signatureB64: String) {
        executeDbAction("UPDATE_PRINCIPAL_SIGNATURE") {
            val current = repository.getSchoolSettingDirect()
            val updated = current.copy(principalSignature = signatureB64)
            repository.updateSchoolSetting(updated)
        }
    }

    // Update Teacher Signature
    fun updateTeacherSignature(signatureB64: String) {
        executeDbAction("UPDATE_TEACHER_SIGNATURE") {
            val current = repository.getSchoolSettingDirect()
            val updated = current.copy(teacherSignature = signatureB64)
            repository.updateSchoolSetting(updated)
        }
    }

    // Add Student
    fun addStudent(name: String, rollNumber: String, className: String, sectionName: String) {
        executeDbAction("ADD_STUDENT") {
            val cleanClass = className.trim()
            val cleanSection = sectionName.trim()
            val cleanName = name.trim()
            val cleanRoll = rollNumber.trim()
            if (cleanClass.isNotEmpty() && cleanSection.isNotEmpty() && cleanName.isNotEmpty() && cleanRoll.isNotEmpty()) {
                repository.insertStudent(
                    Student(
                        name = cleanName,
                        rollNumber = cleanRoll,
                        className = cleanClass,
                        sectionName = cleanSection
                    )
                )
            }
        }
    }

    // Active mock role simulator: "Admin", "Teacher", "Principal/Coordinator"
    private val _activeRole = MutableStateFlow("Admin")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    fun updateActiveRole(role: String) {
        _activeRole.value = role
    }

    // All exam configurations
    val allExamConfigs: StateFlow<List<ExamConfig>> = repository.allExamConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveExamConfig(config: ExamConfig) {
        executeDbAction("SAVE_EXAM_CONFIG") {
            repository.saveExamConfig(config)
        }
    }

    fun saveExamConfigsBulk(configs: List<ExamConfig>) {
        executeDbAction("SAVE_EXAM_CONFIGS_BULK") {
            repository.saveExamConfigsBulk(configs)
        }
    }

    // Edit Student
    fun updateStudent(student: Student) {
        executeDbAction("UPDATE_STUDENT") {
            repository.updateStudent(student)
        }
    }

    // Delete Student
    fun deleteStudent(student: Student) {
        executeDbAction("DELETE_STUDENT") {
            repository.deleteStudent(student)
        }
    }

    // Save student mark (standard overload)
    fun saveMark(studentId: Int, subject: String, marks: Double, maxMarks: Double = 100.0) {
        executeDbAction("SAVE_MARK_SIMPLE") {
            repository.saveMark(
                Mark(
                    studentId = studentId,
                    subjectName = subject,
                    marksObtained = marks,
                    maxMarks = maxMarks
                )
            )
        }
    }

    // Overloaded Save student mark supporting term and specific assessment mode
    fun saveMark(studentId: Int, subject: String, termName: String, examType: String, marks: Double, maxMarks: Double = 100.0) {
        executeDbAction("SAVE_MARK_DETAILED") {
            repository.saveMark(
                Mark(
                    studentId = studentId,
                    subjectName = subject,
                    termName = termName,
                    examType = examType,
                    marksObtained = marks,
                    maxMarks = maxMarks
                )
            )
        }
    }

    // Helper for subject choices
    val availableSubjects = listOf(
        "Mathematics",
        "Science",
        "English",
        "History",
        "Computer Science",
        "Geography",
        "Arts"
    )

    // Section Subjects management
    val allSectionSubjects: StateFlow<List<SectionSubject>> = repository.allSectionSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun initializeDefaultSubjectsForSectionIfNeeded(className: String, sectionName: String) {
        executeDbAction("INITIALIZE_SECTION_SUBJECTS") {
            val existing = repository.getSubjectsForSection(className, sectionName)
            if (existing.isEmpty()) {
                val defaults = listOf(
                    SectionSubject(className, sectionName, "Mathematics", 100.0),
                    SectionSubject(className, sectionName, "Science", 100.0),
                    SectionSubject(className, sectionName, "English", 100.0),
                    SectionSubject(className, sectionName, "History", 100.0)
                )
                repository.saveSectionSubjectsBulk(defaults)
            }
        }
    }

    fun saveSectionSubject(className: String, sectionName: String, subjectName: String, maxMarks: Double) {
        executeDbAction("SAVE_SECTION_SUBJECT") {
            repository.saveSectionSubject(
                SectionSubject(
                    className = className.trim(),
                    sectionName = sectionName.trim(),
                    subjectName = subjectName.trim(),
                    maxMarks = maxMarks
                )
            )
        }
    }

    fun deleteSectionSubject(className: String, sectionName: String, subjectName: String) {
        executeDbAction("DELETE_SECTION_SUBJECT") {
            repository.deleteSectionSubjectByKeys(className.trim(), sectionName.trim(), subjectName.trim())
        }
    }
}
