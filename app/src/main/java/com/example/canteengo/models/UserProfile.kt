package com.example.canteengo.models

import com.google.firebase.Timestamp

/** Firestore: users/{uid} */
data class UserProfile(
    val uid: String = "",
    val role: String = "",
    val email: String? = null,
    val name: String? = null,
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

