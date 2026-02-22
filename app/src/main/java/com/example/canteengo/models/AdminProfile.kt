package com.example.canteengo.models

data class AdminProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val role: String = UserRole.ADMIN.name,
)

