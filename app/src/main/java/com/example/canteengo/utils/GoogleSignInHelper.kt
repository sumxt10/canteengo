package com.example.canteengo.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Helper for Google Sign-In using Credential Manager API.
 *
 * Usage:
 *   val helper = GoogleSignInHelper(context, WEB_CLIENT_ID)
 *   val result = helper.signIn()
 *   // result.isSuccess → Firebase user is authenticated
 */
class GoogleSignInHelper(
    private val context: Context,
    private val webClientId: String
) {
    private val credentialManager = CredentialManager.create(context)
    private val auth = FirebaseAuth.getInstance()

    suspend fun signIn(): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            Result.failure(e)
        }
    }

    private suspend fun handleSignIn(result: GetCredentialResponse): Result<String> {
        val credential = result.credential

        return when {
            credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Firebase Auth with Google credential
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()

                val user = authResult.user
                if (user != null) {
                    Result.success(user.uid)
                } else {
                    Result.failure(Exception("Firebase authentication returned null user"))
                }
            }
            else -> {
                Result.failure(Exception("Unexpected credential type: ${credential.type}"))
            }
        }
    }

    companion object {
        private const val TAG = "GoogleSignInHelper"
    }
}

