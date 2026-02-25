package com.example.canteengo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Network connectivity utility for handling offline states gracefully.
 */
object NetworkUtils {

    /**
     * Check if network is currently available.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Observe network connectivity changes as a Flow.
     */
    fun observeNetworkConnectivity(context: Context): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                  networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(isNetworkAvailable(context))

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /**
     * Convert network-related throwables to user-friendly messages.
     * Returns a clean message suitable for displaying to users.
     */
    fun getNetworkErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> NO_INTERNET_MESSAGE
            is SocketTimeoutException -> NO_INTERNET_MESSAGE
            is java.net.ConnectException -> NO_INTERNET_MESSAGE
            else -> {
                val message = throwable.message?.lowercase() ?: ""
                when {
                    message.contains("timeout") -> NO_INTERNET_MESSAGE
                    message.contains("unable to resolve host") -> NO_INTERNET_MESSAGE
                    message.contains("network") -> NO_INTERNET_MESSAGE
                    message.contains("connection") -> NO_INTERNET_MESSAGE
                    message.contains("socket") -> NO_INTERNET_MESSAGE
                    message.contains("failed to connect") -> NO_INTERNET_MESSAGE
                    else -> throwable.message ?: "An error occurred"
                }
            }
        }
    }

    /**
     * Check if a throwable is a network-related error.
     */
    fun isNetworkError(throwable: Throwable): Boolean {
        return when (throwable) {
            is UnknownHostException -> true
            is SocketTimeoutException -> true
            is java.net.ConnectException -> true
            else -> {
                val message = throwable.message?.lowercase() ?: ""
                message.contains("timeout") ||
                message.contains("unable to resolve host") ||
                message.contains("network") ||
                message.contains("connection") ||
                message.contains("socket") ||
                message.contains("failed to connect")
            }
        }
    }

    const val NO_INTERNET_MESSAGE = "No internet connection"
}

