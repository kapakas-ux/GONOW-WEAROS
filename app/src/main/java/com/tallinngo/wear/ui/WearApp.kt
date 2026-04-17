package com.tallinngo.wear.ui

import androidx.compose.runtime.Composable
import com.tallinngo.wear.ui.theme.GoNowWearTheme

@Composable
fun WearApp(hasLocationPermission: Boolean = false) {
    GoNowWearTheme {
        DepartureScreen(hasLocationPermission = hasLocationPermission)
    }
}
