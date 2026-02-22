package com.example.canteengo.models

data class StudentProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val libraryCardNumber: String = "",
    val department: String = "",
    val division: String = "",
    val role: String = UserRole.STUDENT.name,
)

