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
    data class VerificationPending(val email: String) : AuthState
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
        val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isDemoCleaned = sharedPrefs.getBoolean("is_demo_cleaned_v7", false)

        // Session Persistence Check
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val loginTime = sharedPrefs.getLong("login_timestamp", 0L)
            val oneWeekMillis = 7L * 24 * 60 * 60 * 1000
            
            if (System.currentTimeMillis() - loginTime > oneWeekMillis) {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                _authState.value = AuthState.Unauthenticated
            } else {
                _authState.value = AuthState.Authenticated(
                    email = currentUser.email ?: "",
                    name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User",
                    photoUrl = null
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check if settings need to be initialized or fetched
            repository.getSchoolSettingDirect()
        }
    }

    // Firebase Google Auth
    fun signInWithGoogle(idToken: String, onLoginSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null && user.email != null) {
                        
                        // Save login timestamp
                        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit().putLong("login_timestamp", System.currentTimeMillis()).apply()

                        _authState.value = AuthState.Authenticated(
                            email = user.email!!,
                            name = user.displayName ?: user.email!!.substringBefore("@"),
                            photoUrl = null
                        )
                        onLoginSuccess()
                    } else {
                        onError("Google Login failed: No user email returned")
                    }
                } else {
                    onError("Google Login failed: ${task.exception?.message}")
                }
            }
    }

    // Passwordless Email Link Authentication
    fun sendPasswordlessLink(email: String, context: android.content.Context, onError: (String) -> Unit) {
        val actionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
            .setUrl("https://school-result-maker.firebaseapp.com/")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(getApplication<Application>().packageName, true, "24")
            .build()
            
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("pending_email", email).apply()
                    _authState.value = AuthState.VerificationPending(email)
                } else {
                    onError("Failed to send link: ${task.exception?.message}")
                }
            }
    }

    fun markAuthenticated(email: String, onComplete: () -> Unit) {
        // Save login timestamp
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putLong("login_timestamp", System.currentTimeMillis()).apply()

        _authState.value = AuthState.Authenticated(email, email.substringBefore("@"), null)
        onComplete()
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
    fun updateSchoolDetails(name: String, session: String, location: String, emoji: String, colorHex: String, affiliationNumber: String, schoolLogoBase64: String) {
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
                teacherSignature = current.teacherSignature,
                contactNumber = current.contactNumber,
                affiliationNumber = affiliationNumber,
                schoolLogoBase64 = schoolLogoBase64
            )
            repository.updateSchoolSetting(updated)
        }
    }

    // Update School Contact Number
    fun updateSchoolContact(contactNumber: String) {
        executeDbAction("UPDATE_SCHOOL_CONTACT") {
            val current = repository.getSchoolSettingDirect()
            val updated = current.copy(contactNumber = contactNumber)
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
    fun addStudent(name: String, rollNumber: String, className: String, sectionName: String, fatherName: String = "", motherName: String = "") {
        executeDbAction("ADD_STUDENT") {
            val cleanClass = className.trim()
            val cleanSection = sectionName.trim()
            val cleanName = name.trim()
            val cleanRoll = rollNumber.trim()
            val cleanFather = fatherName.trim()
            val cleanMother = motherName.trim()
            if (cleanClass.isNotEmpty() && cleanSection.isNotEmpty() && cleanName.isNotEmpty() && cleanRoll.isNotEmpty()) {
                repository.insertStudent(
                    Student(
                        name = cleanName,
                        rollNumber = cleanRoll,
                        className = cleanClass,
                        sectionName = cleanSection,
                        fatherName = cleanFather,
                        motherName = cleanMother
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
        // Enforce logical grouping: Term 1 (UT 1, 2, Term 1) & Term 2 (UT 3, 4, Term 2)
        val finalTermName = when (examType) {
            "UT 1", "UT 2", "Term 1" -> "Term 1"
            "UT 3", "UT 4", "Term 2" -> "Term 2"
            else -> termName
        }

        executeDbAction("SAVE_MARK_DETAILED") {
            repository.saveMark(
                Mark(
                    studentId = studentId,
                    subjectName = subject,
                    termName = finalTermName,
                    examType = examType,
                    marksObtained = marks,
                    maxMarks = maxMarks
                )
            )
        }
    }

    /**
     * Calculates weighted Annual Result for a student across all subjects.
     * Annual = (40% of Term 1 Total) + (60% of Term 2 Total)
     */
    suspend fun calculateAnnualResult(studentId: Int): Map<String, Double> {
        val marks = repository.getMarksForStudent(studentId)
        val subjects = marks.map { it.subjectName }.distinct()
        val results = mutableMapOf<String, Double>()

        for (subject in subjects) {
            val subjectMarks = marks.filter { it.subjectName == subject }
            
            val t1Marks = subjectMarks.filter { it.termName == "Term 1" }
            val t2Marks = subjectMarks.filter { it.termName == "Term 2" }

            val t1Total = t1Marks.sumOf { it.marksObtained }
            val t2Total = t2Marks.sumOf { it.marksObtained }

            val annualScore = (t1Total * 0.40) + (t2Total * 0.60)
            results[subject] = annualScore
        }
        return results
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
