package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.net.Uri
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Authenticated(val email: String, val name: String, val photoUrl: String?, val googleId: String) : AuthState
}

data class MonthlyAttendanceSummary(
    val studentId: Int,
    val attendedClasses: Int,
    val totalClasses: Int
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SchoolRepository(database)

    private var socket: Socket? = null

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

    private val _approvedTeachers = MutableStateFlow<List<com.example.data.TeacherUser>>(emptyList())
    val approvedTeachers: StateFlow<List<com.example.data.TeacherUser>> = _approvedTeachers.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _isWaitingForApproval = MutableStateFlow(false)
    val isWaitingForApproval: StateFlow<Boolean> = _isWaitingForApproval.asStateFlow()

    private val _coordinatorName = MutableStateFlow<String?>(null)
    val coordinatorName: StateFlow<String?> = _coordinatorName.asStateFlow()

    private val _canToggleRole = MutableStateFlow(false)
    val canToggleRole: StateFlow<Boolean> = _canToggleRole.asStateFlow()

    fun togglePrivilegedRole() {
        if (_canToggleRole.value) {
            val current = _currentUserRole.value
            val newRole = if (current == "Admin") "Teacher" else "Admin"
            updateRole(newRole)
        }
    }

    private fun getCurrentUserGoogleId(): String? {
        val auth = _authState.value
        return if (auth is AuthState.Authenticated) {
            auth.googleId
        } else {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        }
    }

    private fun getScopedKey(baseKey: String): String {
        val googleId = getCurrentUserGoogleId()
        return if (googleId != null) "${baseKey}_$googleId" else baseKey
    }

    fun fetchCoordinatorName(coordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "$BACKEND_URL/coordinator-info/$coordId"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val name = json.optString("name")
                _coordinatorName.value = name
            } catch (e: Exception) {
                println("Error fetching coordinator name: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun checkRequestStatusFromServer(googleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "$BACKEND_URL/request-status/$googleId"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val status = json.optString("status")
                
                val prefs = getApplication<Application>()
                    .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val roleKey = "user_role_$googleId"
                val coordKey = "coordinator_id_$googleId"
                val waitingKey = "is_waiting_for_approval_$googleId"
                
                if (status == "approved") {
                    val coordId = json.optString("coordinatorId")
                    _isWaitingForApproval.value = false
                    _currentUserRole.value = "Teacher"
                    _coordinatorId.value = coordId
                    
                    prefs.edit()
                        .putString(roleKey, "Teacher")
                        .putString(coordKey, coordId)
                        .putBoolean(waitingKey, false)
                        .apply()
                        
                    fetchCoordinatorName(coordId)
                    syncUserWithBackend()
                } else if (status == "none") {
                    _isWaitingForApproval.value = false
                    _currentUserRole.value = null
                    _coordinatorId.value = null
                    
                    prefs.edit()
                        .remove(roleKey)
                        .remove(coordKey)
                        .putBoolean(waitingKey, false)
                        .apply()
                }
            } catch (e: Exception) {
                println("Error checking request status: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateRole(role: String?) {
        _currentUserRole.value = role
        val key = getScopedKey("user_role")
        getApplication<Application>()
            .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putString(key, role).apply()
        syncUserWithBackend()
    }

    fun updateCoordinatorId(id: String?) {
        _coordinatorId.value = id
        id?.let {
            socket?.emit("join_room", it)
            println("Socket.io: Emitted join_room for $it")
            
            val key = getScopedKey("coordinator_id")
            getApplication<Application>()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString(key, it).apply()
            
            fetchPendingRequestsFromServer(it)
            fetchApprovedTeachersFromServer(it)
            fetchNotificationsFromServer(it)
        }
        syncUserWithBackend()
    }

    fun setWaitingForApproval(waiting: Boolean) {
        _isWaitingForApproval.value = waiting
        val key = getScopedKey("is_waiting_for_approval")
        getApplication<Application>()
            .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(key, waiting).apply()
    }

    fun syncUserWithBackend() {
        val auth = authState.value
        if (auth is AuthState.Authenticated) {
            val googleId = auth.googleId
            val name = auth.name
            val role = currentUserRole.value
            val coordId = coordinatorId.value
            
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val url = URL("$BACKEND_URL/users/sync")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json; utf-8")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.doOutput = true
                    
                    val jsonParam = JSONObject().apply {
                        put("googleId", googleId)
                        put("name", name)
                        if (role != null) put("role", role)
                        if (coordId != null) put("coordinatorId", coordId)
                    }
                    
                    connection.outputStream.use { os ->
                        val input = jsonParam.toString().toByteArray(charset("utf-8"))
                        os.write(input, 0, input.size)
                    }
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        println("User synced successfully with backend")
                        try {
                            val responseBody = connection.inputStream.bufferedReader().readText()
                            val resJson = JSONObject(responseBody)
                            _canToggleRole.value = resJson.optBoolean("canToggleRole", false)
                        } catch (e: Exception) { e.printStackTrace() }
                    } else {
                        println("Failed to sync user with backend: $responseCode")
                    }
                } catch (e: Exception) {
                    println("Error syncing user with backend: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    // Socket.io Implementation
    fun requestAccess(coordId: String, mobile: String) {
        val auth = authState.value
        val name = if (auth is AuthState.Authenticated) auth.name else "Teacher"
        val googleId = if (auth is AuthState.Authenticated) auth.googleId else ""
        
        // CRITICAL FIX: Teacher must join their own ID room to hear the response
        if (googleId.isNotEmpty()) {
            socket?.emit("join_room", googleId)
        }
        
        val data = JSONObject().apply {
            put("teacherName", name)
            put("mobileNumber", mobile)
            put("coordinatorId", coordId)
            put("teacherGoogleId", googleId)
        }
        socket?.emit("request_join", data)
    }

    fun approveRequest(requestId: String, teacherGoogleId: String) {
        val data = JSONObject().apply {
            put("requestId", requestId)
            put("teacherGoogleId", teacherGoogleId)
        }
        socket?.emit("approve_request", data)
        _pendingRequests.value = _pendingRequests.value.filter { it.requestId != requestId }
        
        coordinatorId.value?.let { id ->
            viewModelScope.launch(Dispatchers.IO) {
                kotlinx.coroutines.delay(500)
                fetchApprovedTeachersFromServer(id)
            }
        }
    }

    fun denyRequest(requestId: String, teacherGoogleId: String) {
        val data = JSONObject().apply {
            put("requestId", requestId)
            put("teacherGoogleId", teacherGoogleId)
        }
        socket?.emit("deny_request", data)
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
                    photoUrl = null,
                    googleId = currentUser.uid
                )
                // Sync user session with the backend on launch
                syncUserWithBackend()
            }
        }

        // Restore coordinatorId, role, and waiting status from SharedPreferences
        val googleId = currentUser?.uid
        val roleKey = if (googleId != null) "user_role_$googleId" else "user_role"
        val coordKey = if (googleId != null) "coordinator_id_$googleId" else "coordinator_id"
        val waitingKey = if (googleId != null) "is_waiting_for_approval_$googleId" else "is_waiting_for_approval"

        val savedRole = sharedPrefs.getString(roleKey, null)
        val savedCoordId = sharedPrefs.getString(coordKey, null)
        val savedWaiting = sharedPrefs.getBoolean(waitingKey, false)
        
        _isWaitingForApproval.value = savedWaiting
        if (savedCoordId != null && savedRole != null) {
            _coordinatorId.value = savedCoordId
            _currentUserRole.value = savedRole
            if (savedRole == "Teacher") {
                fetchCoordinatorName(savedCoordId)
            }
            fetchNotificationsFromServer(savedCoordId)
        } else {
            _coordinatorId.value = null
            _currentUserRole.value = null
        }

        if (_isWaitingForApproval.value && _coordinatorId.value == null && googleId != null) {
            checkRequestStatusFromServer(googleId)
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Check if settings need to be initialized or fetched
            repository.getSchoolSettingDirect()
        }

        // Initialize Socket.io
        try {
            socket = IO.socket(BACKEND_URL)
            socket?.on(Socket.EVENT_CONNECT) {
                println("Socket.io: Connected to backend")
                // If coordinatorId is already known (e.g. from local storage/prev session), join room
                coordinatorId.value?.let { id ->
                    socket?.emit("join_room", id)
                    // BUG FIX: Fetch missed pending requests and approved teachers on reconnect
                    fetchPendingRequestsFromServer(id)
                    fetchApprovedTeachersFromServer(id)
                    fetchNotificationsFromServer(id)
                }
            }
            
            socket?.on("new_join_request") { args ->
                val data = args[0] as JSONObject
                val request = JoinRequest(
                    requestId = data.optString("_id", UUID.randomUUID().toString()),
                    teacherName = data.optString("teacherName"),
                    teacherGoogleId = data.optString("teacherGoogleId"),
                    mobileNumber = data.optString("mobileNumber"),
                    coordinatorId = data.optString("coordinatorId")
                )
                if (_pendingRequests.value.none { it.requestId == request.requestId }) {
                    _pendingRequests.value = _pendingRequests.value + request
                }
            }

            socket?.on("join_request_resolved") { args ->
                val data = args[0] as JSONObject
                val status = data.optString("status")
                val prefs = getApplication<Application>()
                    .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

                val currentGoogleId = getCurrentUserGoogleId()
                val rKey = if (currentGoogleId != null) "user_role_$currentGoogleId" else "user_role"
                val cKey = if (currentGoogleId != null) "coordinator_id_$currentGoogleId" else "coordinator_id"
                val wKey = if (currentGoogleId != null) "is_waiting_for_approval_$currentGoogleId" else "is_waiting_for_approval"

                if (status == "approved") {
                    val coordId = data.optString("coordinatorId")
                    _isWaitingForApproval.value = false
                    _currentUserRole.value = "Teacher"
                    _coordinatorId.value = coordId

                    prefs.edit()
                        .putString(rKey, "Teacher")
                        .putString(cKey, coordId)
                        .putBoolean(wKey, false)
                        .apply()

                    fetchCoordinatorName(coordId)
                    // Sync state immediately upon approval
                    syncUserWithBackend()
                    fetchNotificationsFromServer(coordId)
                } else if (status == "denied") {
                    _isWaitingForApproval.value = false
                    _currentUserRole.value = null
                    _coordinatorId.value = null

                    prefs.edit()
                        .remove(rKey)
                        .remove(cKey)
                        .putBoolean(wKey, false)
                        .apply()
                }
            }

            socket?.on("new_notification") { args ->
                val data = args[0] as JSONObject
                val item = NotificationItem(
                    id = data.optString("_id"),
                    coordinatorId = data.optString("coordinatorId"),
                    senderName = data.optString("senderName"),
                    message = data.optString("message"),
                    attachmentUrl = if (data.isNull("attachmentUrl")) null else data.optString("attachmentUrl"),
                    attachmentName = if (data.isNull("attachmentName")) null else data.optString("attachmentName"),
                    attachmentMimeType = if (data.isNull("attachmentMimeType")) null else data.optString("attachmentMimeType"),
                    createdAt = data.optString("createdAt")
                )
                if (_notifications.value.none { it.id == item.id }) {
                    _notifications.value = listOf(item) + _notifications.value
                }
            }

            socket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Fetches pending join requests from the server via REST.
     * Fix for Bug 1: Recovery when socket events are missed during backend downtime.
     */
    private fun fetchPendingRequestsFromServer(coordinatorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "$BACKEND_URL/pending-requests/$coordinatorId"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val fetchedRequests = mutableListOf<com.example.data.JoinRequest>()
                
                for (i in 0 until jsonArray.length()) {
                    val data = jsonArray.getJSONObject(i)
                    fetchedRequests.add(com.example.data.JoinRequest(
                        requestId = data.optString("_id"),
                        teacherName = data.optString("teacherName"),
                        teacherGoogleId = data.optString("teacherGoogleId"),
                        mobileNumber = data.optString("mobileNumber"),
                        coordinatorId = data.optString("coordinatorId")
                    ))
                }
                
                // Update state with fresh list from server
                _pendingRequests.value = fetchedRequests
            } catch (e: Exception) {
                println("Error fetching pending requests: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Fetches approved teachers from the server via REST.
     */
    fun fetchApprovedTeachersFromServer(coordinatorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "$BACKEND_URL/teachers/$coordinatorId"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val fetchedTeachers = mutableListOf<com.example.data.TeacherUser>()
                
                for (i in 0 until jsonArray.length()) {
                    val data = jsonArray.getJSONObject(i)
                    fetchedTeachers.add(com.example.data.TeacherUser(
                        googleId = data.optString("googleId"),
                        name = data.optString("name"),
                        mobileNumber = if (data.isNull("mobileNumber")) null else data.optString("mobileNumber"),
                        role = if (data.isNull("role")) null else data.optString("role"),
                        coordinatorId = if (data.isNull("coordinatorId")) null else data.optString("coordinatorId")
                    ))
                }
                
                _approvedTeachers.value = fetchedTeachers
            } catch (e: Exception) {
                println("Error fetching approved teachers: ${e.message}")
                e.printStackTrace()
            }
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
                            photoUrl = null,
                            googleId = user.uid
                        )
                        syncUserWithBackend()
                        onLoginSuccess()
                    } else {
                        onError("Google Login failed: No user email returned")
                    }
                } else {
                    onError("Google Login failed: ${task.exception?.message}")
                }
            }
    }



    fun logout(onComplete: () -> Unit) {
        val googleId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        _authState.value = AuthState.Unauthenticated
        
        // BUG FIX: Clear coordinatorId, role, and waiting status from SharedPreferences on logout
        val prefs = getApplication<Application>()
            .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            
        val editor = prefs.edit()
        
        // Clean up legacy unscoped keys
        editor.remove("coordinator_id")
              .remove("user_role")
              .remove("is_waiting_for_approval")
              
        if (googleId != null) {
            editor.remove("user_role_$googleId")
                  .remove("coordinator_id_$googleId")
                  .remove("is_waiting_for_approval_$googleId")
        }
        editor.apply()
            
        _coordinatorId.value = null
        _currentUserRole.value = null
        _isWaitingForApproval.value = false
        _coordinatorName.value = null
        _canToggleRole.value = false
        onComplete()
    }

    override fun onCleared() {
        super.onCleared()
        socket?.disconnect()
        socket?.off()
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



    fun updateTermWeights(t1: Int, t2: Int) {
        _term1Weight.value = t1
        _term2Weight.value = t2
        getApplication<Application>().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putInt("term1_weight", t1)
            .putInt("term2_weight", t2)
            .apply()
    }

    fun fetchNotificationsFromServer(coordId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "$BACKEND_URL/notifications/$coordId"
                val response = URL(url).readText()
                val jsonArray = JSONArray(response)
                val fetched = mutableListOf<NotificationItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    fetched.add(
                        NotificationItem(
                            id = obj.optString("_id"),
                            coordinatorId = obj.optString("coordinatorId"),
                            senderName = obj.optString("senderName"),
                            message = obj.optString("message"),
                            attachmentUrl = if (obj.isNull("attachmentUrl")) null else obj.optString("attachmentUrl"),
                            attachmentName = if (obj.isNull("attachmentName")) null else obj.optString("attachmentName"),
                            attachmentMimeType = if (obj.isNull("attachmentMimeType")) null else obj.optString("attachmentMimeType"),
                            createdAt = obj.optString("createdAt")
                        )
                    )
                }
                _notifications.value = fetched
            } catch (e: Exception) {
                println("Error fetching notifications: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun sendNotification(
        message: String,
        fileUri: Uri?,
        fileName: String?,
        mimeType: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val coordId = coordinatorId.value ?: return onFailure("No coordinator ID found")
        val sender = authState.value.let { if (it is AuthState.Authenticated) it.name else "Coordinator" }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val builder = okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("coordinatorId", coordId)
                    .addFormDataPart("senderName", sender)
                    .addFormDataPart("message", message)
                
                if (fileUri != null && fileName != null && mimeType != null) {
                    val contentResolver = getApplication<Application>().contentResolver
                    val inputStream = contentResolver.openInputStream(fileUri)
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        inputStream.close()
                        val mediaType = mimeType.toMediaTypeOrNull()
                        val fileBody = bytes.toRequestBody(mediaType)
                        builder.addFormDataPart("file", fileName, fileBody)
                    }
                }
                
                val requestBody = builder.build()
                val request = okhttp3.Request.Builder()
                    .url("$BACKEND_URL/notifications/send")
                    .post(requestBody)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        val errorMsg = response.body?.string() ?: "Unknown error"
                        viewModelScope.launch(Dispatchers.Main) {
                            onFailure(errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    onFailure(e.message ?: "Failed to send request")
                }
            }
        }
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
            "Term 1 Internal", "Term 2 Internal" -> {
                if (config != null) {
                    var total = 0.0
                    if (config.hasMultipleAssessment) total += config.multipleAssessmentMarks
                    if (config.hasNotebookSubmission) total += config.notebookSubmissionMarks
                    if (config.hasSubjectEnrichment) total += config.subjectEnrichmentMarks
                    if (config.hasPaWeightage) total += config.paWeightageMarks
                    if (total == 0.0) 20.0 else total
                } else 20.0
            }
            "PT 3" -> config?.t2PaMaxMarks1 ?: 20.0
            "PT 4" -> config?.t2PaMaxMarks2 ?: 20.0
            "FA 2" -> {
                val secSub = allSectionSubjects.value.find { it.className == className && it.sectionName == sectionName && it.subjectName == subjectName }
                secSub?.maxMarks ?: 80.0
            }
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
