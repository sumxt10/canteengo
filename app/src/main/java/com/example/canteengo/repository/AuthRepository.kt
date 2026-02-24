package com.example.canteengo.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

    /**
     * Links an email/password credential to the current user's account.
     * This is used after Google Sign-In to enable email/password login.
     *
     * @param email The email address to link (should match the Google account email)
     * @param password The password to set for email/password authentication
     * @throws FirebaseAuthWeakPasswordException if the password is too weak
     * @throws FirebaseAuthUserCollisionException if the credential is already in use
     * @throws Exception for other Firebase Auth errors
     */
    suspend fun linkEmailPasswordCredential(email: String, password: String): LinkCredentialResult {
        val auth = authOrNull() ?: return LinkCredentialResult.Error("Firebase is not initialized")
        val user = auth.currentUser ?: return LinkCredentialResult.Error("No authenticated user")

        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.linkWithCredential(credential).await()
            LinkCredentialResult.Success
        } catch (e: FirebaseAuthWeakPasswordException) {
            LinkCredentialResult.WeakPassword(e.reason ?: "Password is too weak")
        } catch (e: FirebaseAuthUserCollisionException) {
            // This email/password combination might already be linked or used by another account
            LinkCredentialResult.CredentialAlreadyInUse("This email is already associated with another account or login method")
        } catch (e: Exception) {
            LinkCredentialResult.Error(e.message ?: "Failed to link credentials")
        }
    }

    /**
     * Checks if the current user has email/password provider linked.
     */
    fun hasEmailPasswordProvider(): Boolean {
        val user = authOrNull()?.currentUser ?: return false
        return user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }
    }

    fun signOut() {
        authOrNull()?.signOut()
        // Clear cached data on logout
        com.example.canteengo.utils.CacheManager.clearAll()
    }
}

/**
 * Sealed class representing the result of linking credentials.
 */
sealed class LinkCredentialResult {
    object Success : LinkCredentialResult()
    data class WeakPassword(val message: String) : LinkCredentialResult()
    data class CredentialAlreadyInUse(val message: String) : LinkCredentialResult()
    data class Error(val message: String) : LinkCredentialResult()
}
