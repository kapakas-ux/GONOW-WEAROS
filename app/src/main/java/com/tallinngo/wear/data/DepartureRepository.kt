package com.tallinngo.wear.data

import android.content.Context

class DepartureRepository(private val context: Context) {

    suspend fun getNearbyDepartures(): List<StopDepartures> {
        val location = LocationService.getCurrentLocation(context)
            ?: return emptyList()

        val (lat, lon) = location
        val stops = PeatusApi.fetchNearbyStops(lat, lon)

        return stops.take(3).map { stop ->
            val departures = try {
                PeatusApi.fetchDepartures(stop.gtfsId)
            } catch (e: Exception) {
                emptyList()
            }
            StopDepartures(stop, departures)
        }
    }
}
