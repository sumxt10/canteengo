package com.example.canteengo.utils

import com.example.canteengo.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    suspend fun syncTokenForLoggedInUser(userRepository: UserRepository) {
        val hasFirebase = runCatching { FirebaseApp.getInstance() }.isSuccess
        if (!hasFirebase) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        if (token.isBlank()) return

        // Extra guard avoids accidentally syncing token for a signed-out state.
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return
        userRepository.upsertCurrentUserFcmToken(token)
    }
}

