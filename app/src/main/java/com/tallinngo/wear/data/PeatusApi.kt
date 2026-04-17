package com.tallinngo.wear.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object PeatusApi {

    private const val URL = "https://api.peatus.ee/routing/v1/routers/estonia/index/graphql"
    private val JSON_MEDIA = "application/json".toMediaType()
    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNearbyStops(lat: Double, lon: Double): List<Stop> = withContext(Dispatchers.IO) {
        val query = """
            {
              stopsByRadius(lat: $lat, lon: $lon, radius: 500, first: 20) {
                edges {
                  node {
                    stop { gtfsId name lat lon }
                    distance
                  }
                }
              }
            }
        """.trimIndent()

        val body = gson.toJson(mapOf("query" to query)).toRequestBody(JSON_MEDIA)
        val request = Request.Builder().url(URL).post(body).build()
        val response = client.newCall(request).execute()
        val json = gson.fromJson(response.body?.string(), JsonObject::class.java)

        val edges = json
            ?.getAsJsonObject("data")
            ?.getAsJsonObject("stopsByRadius")
            ?.getAsJsonArray("edges")
            ?: return@withContext emptyList()

        val seen = mutableSetOf<String>()
        edges.mapNotNull { edge ->
            val node = edge.asJsonObject.getAsJsonObject("node")
            val stopObj = node.getAsJsonObject("stop")
            val gtfsId = stopObj.get("gtfsId").asString
            val name = stopObj.get("name").asString

            // Deduplicate by name (multiple platforms for same stop)
            if (!seen.add(name)) return@mapNotNull null

            Stop(
                gtfsId = gtfsId,
                name = name,
                lat = stopObj.get("lat").asDouble,
                lon = stopObj.get("lon").asDouble
            )
        }.take(6)
    }

    suspend fun fetchDepartures(stopGtfsId: String): List<Departure> = withContext(Dispatchers.IO) {
        val nowSeconds = System.currentTimeMillis() / 1000
        val query = """
            {
              stop(id: "estonia:$stopGtfsId") {
                stoptimesWithoutPatterns(numberOfDepartures: 6, startTime: ${nowSeconds - 60}) {
                  scheduledDeparture
                  realtimeDeparture
                  realtime
                  realtimeState
                  headsign
                  serviceDay
                  trip {
                    route {
                      shortName
                      mode
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val body = gson.toJson(mapOf("query" to query)).toRequestBody(JSON_MEDIA)
        val request = Request.Builder().url(URL).post(body).build()
        val response = client.newCall(request).execute()
        val json = gson.fromJson(response.body?.string(), JsonObject::class.java)

        val stoptimes = json
            ?.getAsJsonObject("data")
            ?.getAsJsonObject("stop")
            ?.getAsJsonArray("stoptimesWithoutPatterns")
            ?: return@withContext emptyList()

        stoptimes.mapNotNull { st ->
            val obj = st.asJsonObject
            val state = obj.get("realtimeState")?.asString ?: ""
            if (state == "DEPARTED" || state == "CANCELED") return@mapNotNull null

            val serviceDay = obj.get("serviceDay").asLong
            val isRealtime = obj.get("realtime")?.asBoolean ?: false
            val departure = if (isRealtime) {
                obj.get("realtimeDeparture").asLong
            } else {
                obj.get("scheduledDeparture").asLong
            }
            val departureEpoch = serviceDay + departure
            val diffSeconds = departureEpoch - nowSeconds

            if (diffSeconds < -60) return@mapNotNull null

            val minutes = maxOf(0, (diffSeconds / 60).toInt())
            val trip = obj.getAsJsonObject("trip")
            val route = trip?.getAsJsonObject("route")

            Departure(
                routeShortName = route?.get("shortName")?.asString ?: "?",
                headsign = obj.get("headsign")?.asString ?: "",
                minutesUntil = minutes,
                mode = route?.get("mode")?.asString ?: "BUS",
                isRealtime = isRealtime
            )
        }.take(5)
    }
}
