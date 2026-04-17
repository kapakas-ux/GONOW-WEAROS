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

        // Fetch departures for each platform separately
        val results = stops.take(8).mapNotNull { stop ->
            val departures = try {
                PeatusApi.fetchDepartures(stop.gtfsId)
            } catch (e: Exception) {
                emptyList()
            }
            if (departures.isEmpty()) return@mapNotNull null

            // Use most common headsign as direction
            val direction = departures
                .groupBy { it.headsign }
                .maxByOrNull { it.value.size }
                ?.key ?: ""

            StopDepartures(stop, direction, departures.take(4))
        }.take(6)

        return Result.Success(results)
    }
}
