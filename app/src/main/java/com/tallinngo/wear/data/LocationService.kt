package com.tallinngo.wear.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationService {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)

            // Try fresh location first
            val location = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                // Fall back to last known
                val last = client.lastLocation.await()
                last?.let { Pair(it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            null
        }
    }
}
