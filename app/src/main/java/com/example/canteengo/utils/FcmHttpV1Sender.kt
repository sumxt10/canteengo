package com.example.canteengo.utils

import android.content.Context
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FcmHttpV1Sender {

    private const val PROJECT_ID = "canteengo-f1c4d"
    private const val FCM_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private const val SERVICE_ACCOUNT_ASSET_PATH = "service-account.json"

    @Volatile
    private var credentials: GoogleCredentials? = null

    private fun getCredentials(context: Context): GoogleCredentials {
        val existing = credentials
        if (existing != null) return existing

        synchronized(this) {
            val secondCheck = credentials
            if (secondCheck != null) return secondCheck

            val loaded = context.assets.open(SERVICE_ACCOUNT_ASSET_PATH).use { stream ->
                GoogleCredentials.fromStream(stream).createScoped(listOf(SCOPE))
            }
            credentials = loaded
            return loaded
        }
    }

    private fun getAccessToken(context: Context): String {
        val creds = getCredentials(context)
        creds.refreshIfExpired()
        return creds.accessToken.tokenValue
    }

    suspend fun sendOrderStatus(
        context: Context,
        studentFcmToken: String,
        recipientUid: String,
        orderId: String,
        status: String,
        adminName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", studentFcmToken)
                    put("android", JSONObject().apply {
                        put("priority", "high")
                    })
                    put("data", JSONObject().apply {
                        put("type", "order_status")
                        put("recipientUid", recipientUid)
                        put("orderId", orderId)
                        put("status", status)
                        put("adminName", adminName)
                    })
                })
            }

            val accessToken = getAccessToken(context)
            val connection = (URL(FCM_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
            }

            connection.outputStream.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            if (responseCode in 200..299) {
                responseText
            } else {
                error("FCM send failed: HTTP $responseCode, body=$responseText")
            }
        }
    }
}

