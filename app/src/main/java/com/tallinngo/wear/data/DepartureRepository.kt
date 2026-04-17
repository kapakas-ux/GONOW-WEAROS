package com.tallinngo.wear.data

import android.content.Context

class DepartureRepository(private val context: Context) {

    sealed class Result {
        data class Success(val stops: List<StopDepartures>) : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun getNearbyDepartures(): Result {
        val location = LocationService.getCurrentLocation(context)
            ?: return Result.Error("Cannot get location.\nSet location in emulator\nor enable GPS.")

        val (lat, lon) = location
        val stops = try {
            PeatusApi.fetchNearbyStops(lat, lon)
        } catch (e: Exception) {
            return Result.Error("Network error:\n${e.message}")
        }

        if (stops.isEmpty()) {
            return Result.Error("No stops within 500m.\nAre you in Estonia?")
        }

        val results = stops.take(3).map { stop ->
            val departures = try {
                PeatusApi.fetchDepartures(stop.gtfsId)
            } catch (e: Exception) {
                emptyList()
            }
            StopDepartures(stop, departures)
        }
        return Result.Success(results)
    }
}
