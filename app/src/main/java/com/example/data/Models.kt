package com.example.data

const val BACKEND_URL = "https://result-maker-vawv.onrender.com"

data class JoinRequest(
    val requestId: String,
    val teacherName: String,
    val mobileNumber: String,
    val coordinatorId: String
)
