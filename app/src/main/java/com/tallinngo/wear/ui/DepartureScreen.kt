package com.tallinngo.wear.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import com.tallinngo.wear.data.Departure
import com.tallinngo.wear.data.DepartureRepository
import com.tallinngo.wear.data.StopDepartures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data object Loading : UiState()
    data class Success(val stops: List<StopDepartures>) : UiState()
    data class Error(val message: String) : UiState()
}

class DepartureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DepartureRepository(application)
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            try {
                when (val result = repository.getNearbyDepartures()) {
                    is DepartureRepository.Result.Success -> {
                        _state.value = UiState.Success(result.stops)
                    }
                    is DepartureRepository.Result.Error -> {
                        _state.value = UiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

@Composable
fun DepartureScreen(
    hasLocationPermission: Boolean = false,
    viewModel: DepartureViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberScalingLazyListState()

    // Auto-refresh when permission is granted
    androidx.compose.runtime.LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.refresh()
        }
    }

    when (val s = state) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Finding stops...", fontSize = 12.sp, color = Color.Gray)
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.message, fontSize = 13.sp, color = Color(0xFFFF6B6B))
                Spacer(modifier = Modifier.height(8.dp))
                androidx.wear.compose.material3.TextButton(
                    onClick = { viewModel.refresh() }
                ) {
                    Text("Retry", fontSize = 12.sp)
                }
            }
        }

        is UiState.Success -> {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                s.stops.forEach { stopDepartures ->
                    // Stop name header
                    item {
                        Text(
                            text = stopDepartures.stop.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4FC3F7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (stopDepartures.departures.isEmpty()) {
                        item {
                            Text(
                                text = "No departures",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    } else {
                        stopDepartures.departures.forEach { departure ->
                            item {
                                DepartureRow(departure)
                            }
                        }
                    }

                    // Spacer between stops
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Refresh button at bottom
                item {
                    androidx.wear.compose.material3.TextButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("↻ Refresh", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DepartureRow(departure: Departure) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Route badge
        val badgeColor = when {
            departure.mode.contains("TRAM", true) -> Color(0xFFFF5722)
            departure.mode.contains("TROLLEY", true) -> Color(0xFF2196F3)
            departure.mode.contains("RAIL", true) || departure.mode.contains("TRAIN", true) -> Color(0xFF9C27B0)
            else -> Color(0xFF4CAF50) // Bus
        }

        Text(
            text = departure.routeShortName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .width(32.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Destination
        Text(
            text = departure.headsign,
            fontSize = 11.sp,
            color = Color.LightGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Time
        val timeText = if (departure.minutesUntil == 0) "Now" else "${departure.minutesUntil}m"
        val timeColor = when {
            departure.minutesUntil == 0 -> Color(0xFF4CAF50)
            departure.minutesUntil <= 2 -> Color(0xFFFFC107)
            else -> Color.White
        }
        Text(
            text = timeText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = timeColor
        )
    }
}
