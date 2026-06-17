package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val email: String, val name: String, val photoUrl: String?) : AuthState
    data class VerificationPending(val email: String) : AuthState
}

data class MonthlyAttendanceSummary(
    val studentId: Int,
    val attendedClasses: Int,
    val totalClasses: Int
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SchoolRepository(database)

    // Bulk Attendance Entry State
    private val _monthlyAttendanceSummaries = MutableStateFlow<Map<Int, MonthlyAttendanceSummary>>(emptyMap())
    val monthlyAttendanceSummaries: StateFlow<Map<Int, MonthlyAttendanceSummary>> = _monthlyAttendanceSummaries.asStateFlow()

    fun updateMonthlyAttendanceBulk(summaries: List<MonthlyAttendanceSummary>) {
        val current = _monthlyAttendanceSummaries.value.toMutableMap()
        summaries.forEach { summary ->
            current[summary.studentId] = summary
        }
        _monthlyAttendanceSummaries.value = current
    }

    /**
     * Fetches aggregate attendance from the database for a specific class section.
     * Calculated as (Attended Count, Total Record Count).
     */
    suspend fun fetchAggregateAttendanceForClass(className: String, sectionName: String): Map<Int, Pair<String, String>> {
        kotlinx.coroutines.delay(800) // Artificial delay for fancy UX loader
        val studentsInSection = allStudents.value.filter { it.className == className && it.sectionName == sectionName }
        val studentIds = studentsInSection.map { it.id }.toSet()
        val records = allAttendance.value.filter { it.studentId in studentIds }
        
        return studentsInSection.associate { student ->
            val studentRecords = records.filter { it.studentId == student.id }
            val attended = studentRecords.count { it.status.equals("Present", ignoreCase = true) }
            student.id to Pair(attended.toString(), studentRecords.size.toString())
        }
    }

    /**
     * Fetches aggregate attendance from the database for a specific class section.
     */
    fun getAggregateAttendanceFromDB(className: String, sectionName: String): Map<Int, Pair<Int, Int>> {
        val studentsInSection = allStudents.value.filter { it.className == className && it.sectionName == sectionName }
        val studentIds = studentsInSection.map { it.id }.toSet()
        val records = allAttendance.value.filter { it.studentId in studentIds }
        
        return studentsInSection.associate { student ->
            val studentRecords = records.filter { it.studentId == student.id }
            val attended = studentRecords.count { it.status.equals("Present", ignoreCase = true) }
            student.id to Pair(attended, studentRecords.size)
        }
    }

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

    // Role-Based Auth & Hierarchy States
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()

    private val _coordinatorId = MutableStateFlow<String?>(null)
    val coordinatorId: StateFlow<String?> = _coordinatorId.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<com.example.data.JoinRequest>>(emptyList())
    val pendingRequests: StateFlow<List<com.example.data.JoinRequest>> = _pendingRequests.asStateFlow()

    private val _isWaitingForApproval = MutableStateFlow(false)
    val isWaitingForApproval: StateFlow<Boolean> = _isWaitingForApproval.asStateFlow()

    fun updateRole(role: String?) {
        _currentUserRole.value = role
    }

    fun updateCoordinatorId(id: String?) {
        _coordinatorId.value = id
    }

    fun setWaitingForApproval(waiting: Boolean) {
        _isWaitingForApproval.value = waiting
    }

    // Socket.io Placeholders
    fun requestAccess(coordId: String, mobile: String) {
        // TODO: Implement Socket.io emit('request_join')
        println("Socket.io: Emitting request_join for coordId: $coordId, mobile: $mobile")
    }

    fun approveRequest(requestId: String) {
        // TODO: Implement Socket.io emit('approve_request')
        println("Socket.io: Emitting approve_request for requestId: $requestId")
        _pendingRequests.value = _pendingRequests.value.filter { it.requestId != requestId }
    }

    fun denyRequest(requestId: String) {
        // TODO: Implement Socket.io emit('deny_request')
        println("Socket.io: Emitting deny_request for requestId: $requestId")
        _pendingRequests.value = _pendingRequests.value.filter { it.requestId != requestId }
    }

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

    // All attendance records
    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.allAttendance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All discipline records
    val allDiscipline: StateFlow<List<DisciplineRecord>> = repository.allDiscipline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Term Weightage Settings
    private val _term1Weight = MutableStateFlow(40)
    val term1Weight: StateFlow<Int> = _term1Weight.asStateFlow()

    private val _term2Weight = MutableStateFlow(60)
    val term2Weight: StateFlow<Int> = _term2Weight.asStateFlow()

    init {
        val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isDemoCleaned = sharedPrefs.getBoolean("is_demo_cleaned_v7", false)

        _term1Weight.value = sharedPrefs.getInt("term1_weight", 40)
        _term2Weight.value = sharedPrefs.getInt("term2_weight", 60)

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
    fun addStudent(
        name: String, 
        rollNumber: String, 
        className: String, 
        sectionName: String, 
        fatherName: String = "", 
        motherName: String = "",
        admissionNumber: String = "",
        mobileNumber: String = ""
    ) {
        executeDbAction("ADD_STUDENT") {
            val cleanClass = className.trim()
            val cleanSection = sectionName.trim()
            val cleanName = name.trim()
            val cleanRoll = rollNumber.trim()
            val cleanFather = fatherName.trim()
            val cleanMother = motherName.trim()
            val cleanAdmission = admissionNumber.trim()
            val cleanMobile = mobileNumber.trim()
            if (cleanClass.isNotEmpty() && cleanSection.isNotEmpty() && cleanName.isNotEmpty() && cleanRoll.isNotEmpty()) {
                repository.insertStudent(
                    Student(
                        name = cleanName,
                        rollNumber = cleanRoll,
                        className = cleanClass,
                        sectionName = cleanSection,
                        fatherName = cleanFather,
                        motherName = cleanMother,
                        admissionNumber = cleanAdmission,
                        mobileNumber = cleanMobile
                    )
                )
            }
        }
    }

    // Active mock role simulator: "Admin", "Teacher", "Principal/Coordinator"
    private val _activeRole = MutableStateFlow("Admin")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    fun updateTermWeights(t1: Int, t2: Int) {
        _term1Weight.value = t1
        _term2Weight.value = t2
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putInt("term1_weight", t1)
            .putInt("term2_weight", t2)
            .apply()
    }

    fun updateActiveRole(role: String) {
        _activeRole.value = role
    }

    // Shared Selection State for Grading
    private val _selectedClassSection = MutableStateFlow<String?>(null)
    val selectedClassSection: StateFlow<String?> = _selectedClassSection.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String?>(null)
    val selectedSubject: StateFlow<String?> = _selectedSubject.asStateFlow()

    private val _activeExamType = MutableStateFlow("PT 1")
    val activeExamType: StateFlow<String> = _activeExamType.asStateFlow()

    fun updateSelectedClassSection(value: String?) {
        _selectedClassSection.value = value
        // Reset subject when class changes
        _selectedSubject.value = null
    }

    fun updateSelectedSubject(value: String?) {
        _selectedSubject.value = value
    }

    fun updateActiveExamType(value: String) {
        _activeExamType.value = value
    }

    // Helper to get max marks for a specific assessment type
    fun getMaxMarksForAssessment(className: String, sectionName: String, subjectName: String?, assessmentType: String): Double {
        val config = allExamConfigs.value.find { it.className == className }

        return when (assessmentType) {
            "PT 1" -> config?.t1PaMaxMarks1 ?: 20.0
            "PT 2" -> config?.t1PaMaxMarks2 ?: 20.0
            "FA 1" -> {
                val secSub = allSectionSubjects.value.find { it.className == className && it.sectionName == sectionName && it.subjectName == subjectName }
                secSub?.maxMarks ?: 80.0
            }
            "Term 1 Internal" -> 20.0
            "PT 3" -> config?.t2PaMaxMarks1 ?: 20.0
            "PT 4" -> config?.t2PaMaxMarks2 ?: 20.0
            "FA 2" -> {
                val secSub = allSectionSubjects.value.find { it.className == className && it.sectionName == sectionName && it.subjectName == subjectName }
                secSub?.maxMarks ?: 80.0
            }
            "Term 2 Internal" -> 20.0
            else -> 100.0
        }
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
        // Enforce logical grouping: Term 1 (PT 1, 2, Term 1) & Term 2 (PT 3, 4, Term 2)
        val finalTermName = when (examType) {
            "PT 1", "PT 2", "Half Yearly", "Term 1 Internal" -> "Term 1"
            "PT 3", "PT 4", "Annual Exam", "Term 2 Internal" -> "Term 2"
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
     * Batch updates marks for all students in a specific class section for a given test type and subject.
     */
    fun batchUpdateMarks(className: String, sectionName: String, subjectName: String, examType: String, newValue: String, maxMarks: Double = 100.0) {
        val marksValue = newValue.toDoubleOrNull() ?: return
        executeDbAction("BATCH_UPDATE_MARKS") {
            val students = repository.getStudentsForSection(className, sectionName)
            val termName = when (examType) {
                "PT 1", "PT 2", "Half Yearly", "Term 1 Internal" -> "Term 1"
                "PT 3", "PT 4", "Annual Exam", "Term 2 Internal" -> "Term 2"
                else -> "Term 1"
            }
            students.forEach { student ->
                repository.saveMark(
                    Mark(
                        studentId = student.id,
                        subjectName = subjectName,
                        termName = termName,
                        examType = examType,
                        marksObtained = marksValue,
                        maxMarks = maxMarks
                    )
                )
            }
        }
    }

    /**
     * Calculates weighted Annual Result for a student across all subjects.
     * Annual = (Term 1 Total * (term1Weight / 100.0)) + (Term 2 Total * (term2Weight / 100.0))
     */
    suspend fun calculateAnnualResult(studentId: Int): Map<String, Double> {
        val marks = repository.getMarksForStudent(studentId)
        val subjects = marks.map { it.subjectName }.distinct()
        val results = mutableMapOf<String, Double>()

        val t1Factor = _term1Weight.value / 100.0
        val t2Factor = _term2Weight.value / 100.0

        for (subject in subjects) {
            val subjectMarks = marks.filter { it.subjectName == subject }
            
            val t1Marks = subjectMarks.filter { it.termName == "Term 1" }
            val t2Marks = subjectMarks.filter { it.termName == "Term 2" }

            val t1Total = t1Marks.sumOf { it.marksObtained }
            val t2Total = t2Marks.sumOf { it.marksObtained }

            val annualScore = (t1Total * t1Factor) + (t2Total * t2Factor)
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
        "Arts",
        application.getString(R.string.subject_art_education),
        application.getString(R.string.subject_games_health)
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
    // Delete Section Subject
    fun deleteSectionSubject(className: String, sectionName: String, subjectName: String) {
        executeDbAction("DELETE_SECTION_SUBJECT") {
            repository.deleteSectionSubjectByKeys(className.trim(), sectionName.trim(), subjectName.trim())
        }
    }

    /**
     * Retrieves the marks for a specific student, subject, and exam type.
     */
    fun getMarksForExam(studentId: Int, subjectName: String, examType: String): Double? {
        val marks = allMarks.value
        return marks.find { 
            it.studentId == studentId && 
            it.subjectName == subjectName && 
            it.examType == examType 
        }?.marksObtained
    }

    /**
     * Updates marks for a specific student, subject, and exam type.
     */
    fun updateMarksForExam(studentId: Int, subjectName: String, examType: String, newValue: String, maxMarks: Double = 100.0) {
        val marksValue = newValue.toDoubleOrNull() ?: return
        val termName = when (examType) {
            "PT 1", "PT 2", "Half Yearly", "Term 1 Internal" -> "Term 1"
            "PT 3", "PT 4", "Annual Exam", "Term 2 Internal" -> "Term 2"
            else -> "Term 1"
        }

        executeDbAction("UPDATE_MARK_SINGLE") {
            repository.saveMark(
                Mark(
                    studentId = studentId,
                    subjectName = subjectName,
                    termName = termName,
                    examType = examType,
                    marksObtained = marksValue,
                    maxMarks = maxMarks
                )
            )
        }
    }

    // Attendance Functions
    fun saveAttendance(studentId: Int, date: String, status: String, termName: String) {
        executeDbAction("SAVE_ATTENDANCE") {
            repository.saveAttendanceRecord(
                AttendanceRecord(
                    studentId = studentId,
                    date = date,
                    status = status,
                    termName = termName
                )
            )
        }
    }

    fun deleteAttendance(record: AttendanceRecord) {
        executeDbAction("DELETE_ATTENDANCE") {
            repository.deleteAttendanceRecord(record)
        }
    }

    fun getAttendanceForStudent(studentId: Int, termName: String): Flow<List<AttendanceRecord>> {
        return repository.getAttendanceForStudentFlow(studentId, termName)
    }

    // Discipline Functions
    fun saveDiscipline(studentId: Int, date: String, description: String, grade: String, termName: String) {
        executeDbAction("SAVE_DISCIPLINE") {
            repository.saveDisciplineRecord(
                DisciplineRecord(
                    studentId = studentId,
                    date = date,
                    incidentDescription = description,
                    grade = grade,
                    termName = termName
                )
            )
        }
    }

    fun deleteDiscipline(record: DisciplineRecord) {
        executeDbAction("DELETE_DISCIPLINE") {
            repository.deleteDisciplineRecord(record)
        }
    }

    fun getDisciplineForStudent(studentId: Int, termName: String): Flow<List<DisciplineRecord>> {
        return repository.getDisciplineForStudentFlow(studentId, termName)
    }
}
