package com.example.canteengo.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private fun authOrNull(): FirebaseAuth? {
        return try {
            // FirebaseAuth.getInstance() will crash if FirebaseApp isn't initialized.
            FirebaseApp.getInstance()
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    fun currentUser() = authOrNull()?.currentUser

    suspend fun signIn(email: String, password: String) {
        val auth = authOrNull() ?: error("Firebase is not initialized. Add google-services.json and apply google-services plugin.")
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String) {
        val auth = authOrNull() ?: error("Firebase is not initialized. Add google-services.json and apply google-services plugin.")
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    fun signOut() {
        authOrNull()?.signOut()
    }
}
