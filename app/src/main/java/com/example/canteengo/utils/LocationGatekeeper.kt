package com.example.canteengo.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationGatekeeper {
    private const val CANTEEN_LAT = 19.0457
    private const val CANTEEN_LON = 72.8892
    private const val ALLOWED_RADIUS_METERS = 100.0

    sealed class Result {
        data class Allowed(val distanceMeters: Double) : Result()
        data class TooFar(val distanceMeters: Double) : Result()
        object PermissionMissing : Result()
        object LocationUnavailable : Result()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun verifyStudentWithinOrderRadius(context: Context): Result {
        if (!hasLocationPermission(context)) return Result.PermissionMissing

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Result.LocationUnavailable

        @Suppress("MissingPermission")
        val currentLocation: Location? = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }

        val location = currentLocation ?: return Result.LocationUnavailable
        val distance = distanceToCanteenMeters(location.getLatitude(), location.getLongitude())

        return if (distance <= ALLOWED_RADIUS_METERS) {
            Result.Allowed(distance)
        } else {
            Result.TooFar(distance)
        }
    }

    private fun distanceToCanteenMeters(lat: Double, lon: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat, lon, CANTEEN_LAT, CANTEEN_LON, results)
        return results[0].toDouble()
    }
}

