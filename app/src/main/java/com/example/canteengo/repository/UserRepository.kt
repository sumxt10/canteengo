package com.example.canteengo.repository

import com.example.canteengo.models.AdminProfile
import com.example.canteengo.models.StudentProfile
import com.example.canteengo.models.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {

    private fun authOrNull(): FirebaseAuth? {
        return try {
            FirebaseApp.getInstance()
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun dbOrNull(): FirebaseFirestore? {
        return try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getCurrentUserRole(): UserRole? {
        val auth = authOrNull() ?: return null
        val db = dbOrNull() ?: return null

        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection(USERS).document(uid).get().await()
        if (!doc.exists()) return null
        return UserRole.from(doc.getString(FIELD_ROLE))
    }

    suspend fun upsertCurrentUserRole(role: UserRole) {
        val auth = authOrNull() ?: error("Firebase is not initialized. Add google-services.json and apply google-services plugin.")
        val db = dbOrNull() ?: error("Firebase is not initialized. Add google-services.json and apply google-services plugin.")

        val user = auth.currentUser ?: error("No authenticated user")
        val data = hashMapOf(
            FIELD_UID to user.uid,
            FIELD_ROLE to role.name,
            FIELD_EMAIL to user.email,
            FIELD_IS_ACTIVE to true,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        )
        db.collection(USERS).document(user.uid).set(data).await()
    }

    suspend fun createStudentProfile(profile: StudentProfile) {
        val db = dbOrNull() ?: error("Firebase is not initialized")
        db.collection(USERS).document(profile.uid).set(
            hashMapOf(
                FIELD_UID to profile.uid,
                FIELD_ROLE to UserRole.STUDENT.name,
                FIELD_EMAIL to profile.email,
                FIELD_NAME to profile.name,
                FIELD_MOBILE to profile.mobile,
                FIELD_LIBRARY_CARD_NUMBER to profile.libraryCardNumber,
                FIELD_DEPARTMENT to profile.department,
                FIELD_DIVISION to profile.division,
                FIELD_IS_ACTIVE to true,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun createAdminProfile(profile: AdminProfile) {
        val db = dbOrNull() ?: error("Firebase is not initialized")
        db.collection(USERS).document(profile.uid).set(
            hashMapOf(
                FIELD_UID to profile.uid,
                FIELD_ROLE to UserRole.ADMIN.name,
                FIELD_EMAIL to profile.email,
                FIELD_NAME to profile.name,
                FIELD_MOBILE to profile.mobile,
                FIELD_IS_ACTIVE to true,
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                FIELD_CREATED_AT to FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun getCurrentStudentProfile(): StudentProfile? {
        val auth = authOrNull() ?: return null
        val db = dbOrNull() ?: return null
        val uid = auth.currentUser?.uid ?: return null

        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            if (!doc.exists()) return null
            StudentProfile(
                uid = doc.getString(FIELD_UID) ?: "",
                name = doc.getString(FIELD_NAME) ?: "",
                email = doc.getString(FIELD_EMAIL) ?: "",
                mobile = doc.getString(FIELD_MOBILE) ?: "",
                libraryCardNumber = doc.getString(FIELD_LIBRARY_CARD_NUMBER) ?: "",
                department = doc.getString(FIELD_DEPARTMENT) ?: "",
                division = doc.getString(FIELD_DIVISION) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentAdminProfile(): AdminProfile? {
        val auth = authOrNull() ?: return null
        val db = dbOrNull() ?: return null
        val uid = auth.currentUser?.uid ?: return null

        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            if (!doc.exists()) return null
            AdminProfile(
                uid = doc.getString(FIELD_UID) ?: "",
                name = doc.getString(FIELD_NAME) ?: "",
                email = doc.getString(FIELD_EMAIL) ?: "",
                mobile = doc.getString(FIELD_MOBILE) ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserByMobile(mobile: String): Map<String, Any>? {
        val db = dbOrNull() ?: return null
        return try {
            val snapshot = db.collection(USERS)
                .whereEqualTo(FIELD_MOBILE, mobile)
                .get()
                .await()
            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents[0].data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserMobile(mobile: String) {
        val auth = authOrNull() ?: error("Firebase is not initialized")
        val db = dbOrNull() ?: error("Firebase is not initialized")
        val user = auth.currentUser ?: error("No authenticated user")

        // Use set with merge to create document if it doesn't exist (for Google Sign-In users)
        val data = hashMapOf<String, Any?>(
            FIELD_UID to user.uid,
            FIELD_ROLE to UserRole.ADMIN.name,
            FIELD_EMAIL to user.email,
            FIELD_NAME to (user.displayName ?: ""),
            FIELD_MOBILE to mobile,
            FIELD_IS_ACTIVE to true,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        )

        db.collection(USERS).document(user.uid).set(data, SetOptions.merge()).await()
    }

    suspend fun updateStudentDetails(
        mobile: String,
        libraryCardNumber: String,
        department: String,
        division: String
    ) {
        val auth = authOrNull() ?: error("Firebase is not initialized")
        val db = dbOrNull() ?: error("Firebase is not initialized")
        val user = auth.currentUser ?: error("No authenticated user")

        // Use set with merge to create document if it doesn't exist (for Google Sign-In users)
        val data = hashMapOf<String, Any?>(
            FIELD_UID to user.uid,
            FIELD_ROLE to UserRole.STUDENT.name,
            FIELD_EMAIL to user.email,
            FIELD_NAME to (user.displayName ?: ""),
            FIELD_MOBILE to mobile,
            FIELD_LIBRARY_CARD_NUMBER to libraryCardNumber,
            FIELD_DEPARTMENT to department,
            FIELD_DIVISION to division,
            FIELD_IS_ACTIVE to true,
            FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            FIELD_CREATED_AT to FieldValue.serverTimestamp(),
        )

        db.collection(USERS).document(user.uid).set(data, SetOptions.merge()).await()
    }

    suspend fun hasUserMobile(): Boolean {
        val auth = authOrNull() ?: return false
        val db = dbOrNull() ?: return false
        val uid = auth.currentUser?.uid ?: return false

        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            val mobile = doc.getString(FIELD_MOBILE)
            !mobile.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Optimized method to get user role and mobile status in a single Firestore read.
     * Returns a data class with both values to avoid multiple network calls.
     */
    suspend fun getUserProfileStatus(): UserProfileStatus {
        val auth = authOrNull() ?: return UserProfileStatus(null, false)
        val db = dbOrNull() ?: return UserProfileStatus(null, false)
        val uid = auth.currentUser?.uid ?: return UserProfileStatus(null, false)

        return try {
            val doc = db.collection(USERS).document(uid).get().await()
            if (!doc.exists()) {
                UserProfileStatus(null, false)
            } else {
                val role = UserRole.from(doc.getString(FIELD_ROLE))
                val mobile = doc.getString(FIELD_MOBILE)
                val hasMobile = !mobile.isNullOrBlank()
                UserProfileStatus(role, hasMobile)
            }
        } catch (_: Exception) {
            UserProfileStatus(null, false)
        }
    }

    /**
     * Data class representing user profile status for optimized authentication flow.
     */
    data class UserProfileStatus(
        val role: UserRole?,
        val hasCompletedProfile: Boolean
    )

    companion object {
        private const val USERS = "users"
        private const val FIELD_UID = "uid"
        private const val FIELD_ROLE = "role"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_NAME = "name"
        private const val FIELD_MOBILE = "mobile"
        private const val FIELD_LIBRARY_CARD_NUMBER = "libraryCardNumber"
        private const val FIELD_DEPARTMENT = "department"
        private const val FIELD_DIVISION = "division"
        private const val FIELD_IS_ACTIVE = "isActive"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
    }
}
