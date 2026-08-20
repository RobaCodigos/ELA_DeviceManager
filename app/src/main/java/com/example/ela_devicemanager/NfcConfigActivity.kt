package com.example.ela_devicemanager

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class NfcConfigActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    var isTagScannedState = mutableStateOf(false)
    var isPendingWriteState = mutableStateOf(false)

    private var originalJsonData: JSONObject? = null
    private var originalTnf: Short = 0
    private var originalType: ByteArray = ByteArray(0)

    var tagNameState = mutableStateOf("")
    var tagVersionState = mutableStateOf("")

    var advertisingNameState = mutableStateOf("")
    var tagEnableState = mutableStateOf(false)
    var tagPowerState = mutableStateOf("0")
    var tagFormatState = mutableStateOf("T EN")
    var bleEmitPeriodState = mutableStateOf("5")
    var mfrDataModeState = mutableStateOf(false)
    var mfrIdState = mutableStateOf("000000000000")
    var batteryPresenceState = mutableStateOf(false)

    var dataLoggerEnableState = mutableStateOf(false)
    var dataLoggerPeriodState = mutableStateOf("86400")

    private var activeTag: Tag? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonGreen, background = HackerBlack, surface = HackerDarkSurface,
                    onPrimary = HackerBlack, onBackground = NeonGreen, onSurface = NeonGreen
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = HackerBlack) {
                    if (!isTagScannedState.value) {
                        NfcWaitingScreen(onBack = { finish() })
                    } else {
                        NfcElaScreen(
                            tagName = tagNameState.value,
                            tagVersion = tagVersionState.value,
                            advName = advertisingNameState.value,
                            onAdvNameChange = { advertisingNameState.value = it },
                            tagEnabled = tagEnableState.value,
                            onTagEnabledChange = { tagEnableState.value = it },
                            tagPower = tagPowerState.value,
                            onTagPowerChange = { tagPowerState.value = it },
                            tagFormat = tagFormatState.value,
                            onTagFormatChange = { tagFormatState.value = it },
                            emitPeriod = bleEmitPeriodState.value,
                            onEmitPeriodChange = { bleEmitPeriodState.value = it },
                            mfrMode = mfrDataModeState.value,
                            onMfrModeChange = { mfrDataModeState.value = it },
                            mfrId = mfrIdState.value,
                            onMfrIdChange = { mfrIdState.value = it },
                            batteryPresence = batteryPresenceState.value,
                            onBatteryPresenceChange = { batteryPresenceState.value = it },
                            loggerEnabled = dataLoggerEnableState.value,
                            onLoggerEnabledChange = { dataLoggerEnableState.value = it },
                            loggerPeriod = dataLoggerPeriodState.value,
                            onLoggerPeriodChange = { dataLoggerPeriodState.value = it },
                            isPendingWrite = isPendingWriteState.value,
                            onCancelWrite = { isPendingWriteState.value = false },
                            onInitiateWrite = { isPendingWriteState.value = true },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            tag?.let {
                if (isPendingWriteState.value) {
                    writeDataToTag(it)
                    isPendingWriteState.value = false
                } else {
                    readTagData(it)
                }
            }
        }
    }

    private fun readTagData(tag: Tag) {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val message = ndef.ndefMessage
                if (message != null && message.records.isNotEmpty()) {
                    val record = message.records[0]

                    originalTnf = record.tnf
                    originalType = record.type

                    val payload = record.payload
                    var text = String(payload, StandardCharsets.UTF_8)

                    if (!text.startsWith("{") && text.contains("{")) {
                        text = text.substring(text.indexOf("{"))
                    }

                    try {
                        val json = JSONObject(text)
                        originalJsonData = json

                        tagVersionState.value = json.optString("title", "Desconocida")

                        val props = json.optJSONObject("properties")
                        if (props != null) {
                            val nameObj = props.optJSONObject("Name")
                            val nameValue = nameObj?.optString("value", "Desconocido") ?: "Desconocido"

                            tagNameState.value = nameValue
                            advertisingNameState.value = nameValue
                            tagEnableState.value = props.optJSONObject("EN")?.optInt("value", 0) == 1
                            tagPowerState.value = props.optJSONObject("Power")?.optString("value", "0") ?: "0"
                            tagFormatState.value = props.optJSONObject("Format")?.optString("value", "T EN") ?: "T EN"
                            bleEmitPeriodState.value = props.optJSONObject("AdvRec")?.optString("value", "5") ?: "5"
                            mfrDataModeState.value = props.optJSONObject("MfrData")?.optInt("value", 0) == 1
                            mfrIdState.value = props.optJSONObject("MfrID")?.optString("value", "000000000000") ?: "000000000000"
                            batteryPresenceState.value = props.optJSONObject("BattVoltSR")?.optInt("value", 0) == 1
                            dataLoggerEnableState.value = props.optJSONObject("LogEN")?.optInt("value", 0) == 1
                            dataLoggerPeriodState.value = props.optJSONObject("LogRec")?.optString("value", "86400") ?: "86400"
                        }

                        isTagScannedState.value = true
                        Toast.makeText(this, "¡Sensor mapeado!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        tagNameState.value = "FORMATO NO RECONOCIDO"
                        advertisingNameState.value = text.take(20)
                        isTagScannedState.value = true
                        Toast.makeText(this, "Tag leído (No estándar)", Toast.LENGTH_SHORT).show()
                    }
                }
                ndef.close()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error de lectura NFC", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeDataToTag(tag: Tag) {
        if (originalJsonData == null) {
            Toast.makeText(this, "Faltan los datos originales. Vuelve a leer el Tag.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Toast.makeText(this, "El tag NFC está protegido", Toast.LENGTH_SHORT).show()
                    ndef.close()
                    return
                }

                val props = originalJsonData!!.optJSONObject("properties")
                if (props != null) {
                    props.optJSONObject("Name")?.put("value", advertisingNameState.value)
                    props.optJSONObject("EN")?.put("value", if (tagEnableState.value) 1 else 0)
                    props.optJSONObject("Power")?.put("value", tagPowerState.value.toIntOrNull() ?: 0)
                    props.optJSONObject("Format")?.put("value", tagFormatState.value)
                    props.optJSONObject("AdvRec")?.put("value", bleEmitPeriodState.value.toDoubleOrNull() ?: 5.0)
                    props.optJSONObject("MfrData")?.put("value", if (mfrDataModeState.value) 1 else 0)
                    props.optJSONObject("MfrID")?.put("value", mfrIdState.value)
                    props.optJSONObject("BattVoltSR")?.put("value", if (batteryPresenceState.value) 1 else 0)
                    props.optJSONObject("LogEN")?.put("value", if (dataLoggerEnableState.value) 1 else 0)
                    props.optJSONObject("LogRec")?.put("value", dataLoggerPeriodState.value.toIntOrNull() ?: 86400)
                }

                val configPayload = originalJsonData!!.toString()
                val payloadBytes = configPayload.toByteArray(StandardCharsets.UTF_8)
                val finalData: ByteArray

                if (originalTnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                    originalType.contentEquals(android.nfc.NdefRecord.RTD_TEXT)) {
                    val langBytes = "en".toByteArray(StandardCharsets.US_ASCII)
                    finalData = ByteArray(1 + langBytes.size + payloadBytes.size)
                    finalData[0] = langBytes.size.toByte()
                    System.arraycopy(langBytes, 0, finalData, 1, langBytes.size)
                    System.arraycopy(payloadBytes, 0, finalData, 1 + langBytes.size, payloadBytes.size)
                } else {
                    finalData = payloadBytes
                }

                val record = android.nfc.NdefRecord(originalTnf, originalType, ByteArray(0), finalData)
                val message = android.nfc.NdefMessage(arrayOf(record))

                ndef.writeNdefMessage(message)
                ndef.close()
                Toast.makeText(this, "¡Configuración inyectada con éxito!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "El tag no soporta formato NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al escribir: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun NfcWaitingScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(HackerBlack).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(text = "< VOLVER", color = NeonGreen, fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.weight(1f))
        CircularProgressIndicator(modifier = Modifier.size(100.dp), color = NeonGreen, strokeWidth = 6.dp, trackColor = HackerDarkSurface)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = ">> LISTENING FOR NDEF...", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "ACERCA EL SENSOR BLUE PUCK AL DISPOSITIVO", color = HackerGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1.5f))
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Necesario para usar ExposedDropdownMenuBox
@Composable
fun NfcElaScreen(
    tagName: String, tagVersion: String,
    advName: String, onAdvNameChange: (String) -> Unit,
    tagEnabled: Boolean, onTagEnabledChange: (Boolean) -> Unit,
    tagPower: String, onTagPowerChange: (String) -> Unit,
    tagFormat: String, onTagFormatChange: (String) -> Unit,
    emitPeriod: String, onEmitPeriodChange: (String) -> Unit,
    mfrMode: Boolean, onMfrModeChange: (Boolean) -> Unit,
    mfrId: String, onMfrIdChange: (String) -> Unit,
    batteryPresence: Boolean, onBatteryPresenceChange: (Boolean) -> Unit,
    loggerEnabled: Boolean, onLoggerEnabledChange: (Boolean) -> Unit,
    loggerPeriod: String, onLoggerPeriodChange: (String) -> Unit,
    isPendingWrite: Boolean, onCancelWrite: () -> Unit, onInitiateWrite: () -> Unit,
    onBack: () -> Unit
) {
    var bluetoothExpanded by remember { mutableStateOf(true) }
    var sensorExpanded by remember { mutableStateOf(true) }
    var showActionsDialog by remember { mutableStateOf(false) }

    // Listas de valores permitidos por el hardware
    val powerOptions = listOf("-40", "-20", "-16", "-12", "-8", "-4", "0", "3", "4")
    var powerExpanded by remember { mutableStateOf(false) }

    val formatOptions = listOf("T EN", "Id")
    var formatExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().background(HackerBlack).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(text = "< VOLVER", color = NeonGreen, fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { showActionsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen),
                    modifier = Modifier.border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                ) {
                    Text(text = "More actions", color = NeonGreen, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "NFC Configuration", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().border(1.dp, NeonGreen, RoundedCornerShape(8.dp)), colors = CardDefaults.cardColors(containerColor = HackerDarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Resume", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Tag name / Id: $tagName", color = HackerGray, fontSize = 14.sp)
                    Text(text = "Technology: Bluetooth", color = HackerGray, fontSize = 14.sp)
                    Text(text = "Tag version: $tagVersion", color = HackerGray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Configuration", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth().border(1.dp, NeonGreen, RoundedCornerShape(8.dp)), colors = CardDefaults.cardColors(containerColor = HackerDarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { bluetoothExpanded = !bluetoothExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Bluetooth", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = if (bluetoothExpanded) "▲" else "▼", color = NeonGreen)
                    }

                    if (bluetoothExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = advName, onValueChange = onAdvNameChange,
                            label = { Text("Advertising Name", color = HackerGray) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, cursorColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "TAG Enable State", color = HackerGray)
                            Switch(checked = tagEnabled, onCheckedChange = onTagEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // DESPLEGABLE PARA TAG POWER
                        ExposedDropdownMenuBox(
                            expanded = powerExpanded,
                            onExpandedChange = { powerExpanded = !powerExpanded }
                        ) {
                            OutlinedTextField(
                                value = tagPower,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("TAG Power (dBm)", color = HackerGray) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = powerExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                            )
                            ExposedDropdownMenu(
                                expanded = powerExpanded,
                                onDismissRequest = { powerExpanded = false },
                                modifier = Modifier.background(HackerDarkSurface).border(1.dp, NeonGreen)
                            ) {
                                powerOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option, color = NeonGreen) },
                                        onClick = {
                                            onTagPowerChange(option)
                                            powerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // DESPLEGABLE PARA TAG FORMAT
                        ExposedDropdownMenuBox(
                            expanded = formatExpanded,
                            onExpandedChange = { formatExpanded = !formatExpanded }
                        ) {
                            OutlinedTextField(
                                value = tagFormat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("TAG Format", color = HackerGray) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                            )
                            ExposedDropdownMenu(
                                expanded = formatExpanded,
                                onDismissRequest = { formatExpanded = false },
                                modifier = Modifier.background(HackerDarkSurface).border(1.dp, NeonGreen)
                            ) {
                                formatOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option, color = NeonGreen) },
                                        onClick = {
                                            onTagFormatChange(option)
                                            formatExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emitPeriod, onValueChange = onEmitPeriodChange,
                            label = { Text("BLE Emit Period (s)", color = HackerGray) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, cursorColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Manufacturer Data Mode", color = HackerGray)
                            Switch(checked = mfrMode, onCheckedChange = onMfrModeChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = mfrId, onValueChange = onMfrIdChange,
                            label = { Text("Manufacturer ID", color = HackerGray) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, cursorColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Battery voltage presence", color = HackerGray)
                            Switch(checked = batteryPresence, onCheckedChange = onBatteryPresenceChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().border(1.dp, NeonGreen, RoundedCornerShape(8.dp)), colors = CardDefaults.cardColors(containerColor = HackerDarkSurface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sensorExpanded = !sensorExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sensor", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = if (sensorExpanded) "▲" else "▼", color = NeonGreen)
                    }

                    if (sensorExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Data Logger Enable", color = HackerGray)
                            Switch(checked = loggerEnabled, onCheckedChange = onLoggerEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = loggerPeriod, onValueChange = onLoggerPeriodChange,
                            label = { Text("Data Logger period", color = HackerGray) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = HackerGray, focusedLabelColor = NeonGreen, cursorColor = NeonGreen, focusedTextColor = NeonGreen, unfocusedTextColor = NeonGreen)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onInitiateWrite,
                modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, NeonGreen, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen)
            ) {
                Text(text = "WRITE THE TAG (ESCRIBIR NFC)", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        }

        if (isPendingWrite) {
            AlertDialog(
                onDismissRequest = onCancelWrite,
                containerColor = HackerDarkSurface,
                title = { Text(">> PREPARADO PARA ESCRIBIR", color = NeonGreen, fontWeight = FontWeight.Bold) },
                text = { Text("Acerca el sensor a la parte trasera del dispositivo para aplicar los cambios de configuración. Mantenlo estable.", color = HackerGray) },
                confirmButton = {
                    TextButton(onClick = onCancelWrite) {
                        Text("Cancelar", color = Color.Red)
                    }
                }
            )
        }

        if (showActionsDialog) {
            AlertDialog(
                onDismissRequest = { showActionsDialog = false },
                containerColor = HackerDarkSurface,
                title = { Text(text = "Actions", color = NeonGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showActionsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen), modifier = Modifier.fillMaxWidth()) {
                            Text("Raw data", color = NeonGreen)
                        }
                        Button(onClick = { showActionsDialog = false; onInitiateWrite() }, colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen), modifier = Modifier.fillMaxWidth()) {
                            Text("Write the tag", color = NeonGreen)
                        }
                        Button(onClick = { showActionsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen), modifier = Modifier.fillMaxWidth()) {
                            Text("Security", color = NeonGreen)
                        }
                        Button(onClick = { showActionsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = DimNeonGreen), modifier = Modifier.fillMaxWidth()) {
                            Text("Data logger", color = NeonGreen)
                        }
                        Button(onClick = { showActionsDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
                            Text("Clear tag", color = Color.Red)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showActionsDialog = false }) { Text("Cerrar", color = NeonGreen) }
                }
            )
        }
    }
}