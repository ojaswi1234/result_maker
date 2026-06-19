package com.example.data

const val BACKEND_URL = "https://result-maker-vawv.onrender.com"

data class JoinRequest(
    val requestId: String,
    val teacherName: String,
    val teacherGoogleId: String,
    val mobileNumber: String,
    val coordinatorId: String
)

data class TeacherUser(
    val googleId: String,
    val name: String,
    val mobileNumber: String?,
    val role: String?,
    val coordinatorId: String?
)
