package com.example.ela_devicemanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonGreen,
                    background = HackerBlack,
                    surface = HackerDarkSurface,
                    onPrimary = HackerBlack,
                    onBackground = NeonGreen,
                    onSurface = NeonGreen
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        containerColor = HackerBlack,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        MainDashboard(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToNfc = {
                                startActivity(Intent(this, NfcConfigActivity::class.java))
                            },
                            onNavigateToBle = {
                                startActivity(Intent(this, BluetoothActivity::class.java))
                            },
                            onNavigateToFiles = {
                                // Módulo de archivos guardados (Próximamente)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainDashboard(
    modifier: Modifier = Modifier,
    onNavigateToNfc: () -> Unit,
    onNavigateToBle: () -> Unit,
    onNavigateToFiles: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HackerBlack)
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "ELA_SYS // TERMINAL",
            color = NeonGreen,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SYSTEM READY...",
            color = HackerGray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuHackerCard(
                title = "[ NFC_CONFIG ]",
                subtitle = "Proximidad / Tags",
                modifier = Modifier.weight(1f).height(150.dp),
                onClick = onNavigateToNfc
            )
            MenuHackerCard(
                title = "[ BLE_SCANNER ]",
                subtitle = "Sensores de aire",
                modifier = Modifier.weight(1f).height(150.dp),
                onClick = onNavigateToBle
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuHackerCard(
                title = "[ SAVED_FILES ]",
                subtitle = "Registros y Logs",
                modifier = Modifier.weight(1f).height(150.dp),
                onClick = onNavigateToFiles
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(onClick = { }) {
                Text(text = "// ABOUT_US", color = NeonGreen, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MenuHackerCard(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HackerDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = NeonGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = HackerGray,
                fontSize = 12.sp
            )
        }
    }
}