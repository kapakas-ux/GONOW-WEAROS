package com.tallinngo.wear.data

data class Stop(
    val gtfsId: String,
    val name: String,
    val lat: Double,
    val lon: Double
)

data class Departure(
    val routeShortName: String,
    val headsign: String,
    val minutesUntil: Int,
    val mode: String,
    val isRealtime: Boolean
)

data class StopDepartures(
    val stop: Stop,
    val departures: List<Departure>
)
