package com.example.ela_devicemanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ela_devicemanager.ui.theme.Ela_DeviceManagerTheme

class MainActivity : ComponentActivity() {

    private val bleScanner by lazy { BleScanner(this) }
    private val deviceList = mutableStateListOf<DeviceItem>()
    private var isScanningState = mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startBleScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Ela_DeviceManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        devices = deviceList,
                        isScanning = isScanningState.value,
                        onScanClick = { checkPermissionsAndScan() }
                    )
                }
            }
        }
    }

    private fun checkPermissionsAndScan() {
        val context = this
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
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
                runOnUiThread {
                    isScanningState.value = false
                }
            }
        })
    }
}

data class DeviceItem(val name: String, val address: String, val rssi: Int, val rawData: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    devices: List<DeviceItem>,
    isScanning: Boolean,
    onScanClick: () -> Unit
) {
    // Estado para almacenar el texto que escribe el usuario en el buscador
    var searchQuery by remember { mutableStateOf("") }

    // Filtramos la lista en tiempo real según lo que el usuario escriba
    val filteredDevices = devices.filter { device ->
        device.name.contains(searchQuery, ignoreCase = true) ||
                device.address.contains(searchQuery, ignoreCase = true) ||
                device.rawData.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ELA Device Manager",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Botón de Escaneo
        Button(
            onClick = onScanClick,
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(text = if (isScanning) "Buscando Blue PUCK..." else "Escanear Sensores BLE")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de Búsqueda por Texto
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filtrar por nombre, MAC o trama...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Lista Filtrada
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = device.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "MAC: ${device.address}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Señal: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Trama: ${device.rawData}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}