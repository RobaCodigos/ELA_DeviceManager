package com.example.ela_devicemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class BluetoothActivity : ComponentActivity() {

    private val bleScanner by lazy { BleScanner(this) }
    private val deviceList = mutableStateListOf<DeviceItem>()
    private var isScanningState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            startBleScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonGreen, background = HackerBlack, surface = HackerDarkSurface,
                    onPrimary = HackerBlack, onBackground = NeonGreen, onSurface = NeonGreen
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = HackerBlack) {
                    BluetoothScannerScreen(
                        devices = deviceList,
                        isScanning = isScanningState.value,
                        onScanClick = { checkPermissionsAndScan() },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndScan() {
        // Recopilamos todos los permisos tácticos necesarios
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Si es un móvil moderno (como el Poco X6 Pro), añadimos los permisos BLE nativos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
            // Lanza el popup del sistema pidiendo autorización
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startBleScan() {
        deviceList.clear()
        isScanningState.value = true

        bleScanner.startScanning(this, object : BleScanner.BleScanListener {
            override fun onDeviceFound(deviceName: String, deviceAddress: String, rssi: Int, manufacturerDataStr: String) {
                runOnUiThread {
                    if (deviceList.none { it.address == deviceAddress }) {
                        deviceList.add(DeviceItem(deviceName, deviceAddress, rssi, manufacturerDataStr))
                    }
                }
            }

            override fun onScanFinished() {
                runOnUiThread { isScanningState.value = false }
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScannerScreen(
    devices: List<DeviceItem>,
    isScanning: Boolean,
    onScanClick: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDevices = devices.filter {
        it.name.contains(searchQuery, true) || it.address.contains(searchQuery, true) || it.rawData.contains(searchQuery, true)
    }

    Column(modifier = Modifier.fillMaxSize().background(HackerBlack).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(text = "< VOLVER", color = NeonGreen, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "BLE_SCANNER_MODULE", color = NeonGreen, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onScanClick,
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, NeonGreen, RoundedCornerShape(8.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen)
        ) {
            Text(text = if (isScanning) ">> ESCANEANDO FRECUENCIAS..." else "INICIAR ESCANEO BLE", color = NeonGreen, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filtrar trama / MAC...", color = HackerGray) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonGreen, trackColor = HackerDarkSurface)
            Spacer(modifier = Modifier.height(12.dp))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = HackerDarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "> ${device.name}", color = NeonGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "MAC: ${device.address}", color = HackerGray, fontSize = 13.sp)
                        Text(text = "RSSI: ${device.rssi} dBm", color = HackerGray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "RAW: ${device.rawData}", color = NeonGreen.copy(alpha = 0.8f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}