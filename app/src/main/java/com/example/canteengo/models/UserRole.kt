package com.example.canteengo.models

enum class UserRole {
    STUDENT,
    ADMIN;

    companion object {
        fun from(value: String?): UserRole? {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
    }
}

