package com.example.ela_devicemanager

import androidx.compose.ui.graphics.Color

val HackerBlack = Color(0xFF000000)
val HackerDarkSurface = Color(0xFF0A0F0A)
val NeonGreen = Color(0xFF00FF66)
val DimNeonGreen = Color(0xFF003311)
val HackerGray = Color(0xFF888888)

data class DeviceItem(val name: String, val address: String, val rssi: Int, val rawData: String)