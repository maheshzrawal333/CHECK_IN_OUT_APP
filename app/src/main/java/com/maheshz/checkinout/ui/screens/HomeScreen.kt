package com.maheshz.checkinout.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.maheshz.checkinout.ui.theme.*
import com.maheshz.checkinout.ui.viewmodel.CheckInViewModel
import com.maheshz.checkinout.ui.viewmodel.HomeState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun HomeScreen(viewModel: CheckInViewModel, orgName: String = "Company") {
    val state by viewModel.uiState.collectAsState()
    val name by viewModel.empName.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Shake-to-Scan Listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastUpdate: Long = 0
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        val SHAKE_THRESHOLD = 600

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (state !is HomeState.Idle) return

                val currentTime = System.currentTimeMillis()
                if ((currentTime - lastUpdate) > 100) {
                    val diffTime = currentTime - lastUpdate
                    lastUpdate = currentTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                    if (speed > SHAKE_THRESHOLD) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.startFlow()
                    }

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(sensorEventListener) }
    }

    // Biometric Trigger Logic
    LaunchedEffect(state) {
        when (state) {
            is HomeState.Found -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val sig = viewModel.getSignatureObject()
                if (sig != null) {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(context)
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Verify Identity")
                            .setSubtitle("Authenticate to transmit check-in signature")
                            .setNegativeButtonText("Cancel")
                            .build()

                        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                val signature = result.cryptoObject?.signature ?: return
                                viewModel.onFingerprintSuccess(signature)
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                viewModel.resetState()
                            }
                        })
                        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(sig))
                    }
                }
            }
            is HomeState.Success -> {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(3000)
                viewModel.resetState()
            }
            is HomeState.Failed, is HomeState.Timeout, is HomeState.SecurityLockout -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(LightPurple, CircleShape).clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.firstOrNull()?.uppercase() ?: "U", color = DarkPurple, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(orgName.uppercase(), style = MaterialTheme.typography.labelSmall, color = BrandPurple)
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Scanner Area
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Static decorative rings
                Box(modifier = Modifier.size(256.dp).border(2.dp, PaleTeal, CircleShape))
                Box(modifier = Modifier.size(224.dp).border(4.dp, TealAccent.copy(alpha=0.4f), CircleShape))

                // Central Fingerprint Button (Static)
                FingerprintButton(state = state, onClick = { if (state is HomeState.Idle) viewModel.startFlow() })
            }

            // Status Text Area
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (titleText, statusText, statusColor) = when (state) {
                    is HomeState.Idle -> Triple("SHAKE OR TAP\nTO CHECK IN", "SCANNER READY", SecondaryText)
                    is HomeState.Scanning -> Triple("SEARCHING\nFOR SCANNER", "LOOKING FOR DEVICE", Color(0xFFFFA000))
                    is HomeState.Found -> Triple("VERIFY\nIDENTITY", "BIOMETRIC PROMPT", BrandPurple)
                    is HomeState.Broadcasting -> Triple("BROADCASTING\nSIGNAL", "SENDING TO SCANNER", DarkPurple)
                    is HomeState.Success -> Triple("SUCCESS\nVERIFIED", "VERIFICATION COMPLETE", TealAccent)
                    is HomeState.Failed -> Triple("FAILED\nERROR", "AUTHORIZATION FAILED", MaterialTheme.colorScheme.error)
                    is HomeState.Timeout -> Triple("TIMEOUT\nNO DEVICE", "COULDN'T CONNECT", MaterialTheme.colorScheme.error)
                    is HomeState.SecurityLockout -> Triple("ACCOUNT\nLOCKED", "SECURITY BREACH", MaterialTheme.colorScheme.error)
                }

                Text(titleText, style = MaterialTheme.typography.displayLarge, color = PrimaryText, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                }
            }

            // Recent Activity
            HorizontalDivider(color = LightGrayBorder)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = BrandPurple)
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text("Last Event — ${sdf.format(Date())}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TODAY", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                    val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(sdfDate.format(Date()), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = SecondaryText)
                }
            }
        }

        // Dialogs
        if (state is HomeState.SecurityLockout) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = { TextButton(onClick = { viewModel.resetState() }) { Text("Acknowledge") } },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Security Breach Detected") },
                text = { Text((state as HomeState.SecurityLockout).reason) }
            )
        } else if (state is HomeState.Failed || state is HomeState.Timeout) {
            AlertDialog(
                onDismissRequest = { viewModel.resetState() },
                confirmButton = { TextButton(onClick = { viewModel.resetState() }) { Text("Dismiss") } },
                title = { Text("Connection Failed") },
                text = {
                    val msg = if (state is HomeState.Failed) (state as HomeState.Failed).failureCode else "Could not connect to the scanner."
                    Text("Error: $msg")
                }
            )
        }
    }
}

@Composable
fun FingerprintButton(state: HomeState, onClick: () -> Unit) {
    val iconColor = if (state is HomeState.Success) Color.White else TealAccent
    val bgColor = if (state is HomeState.Success) TealAccent else Color.White

    Box(
        modifier = Modifier
            .size(192.dp)
            .background(bgColor, CircleShape)
            .border(8.dp, TealAccent, CircleShape)
            .clickable(enabled = state is HomeState.Idle) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (state is HomeState.Success) Icons.Default.Check else Icons.Default.Fingerprint,
            contentDescription = "Action Icon",
            modifier = Modifier.size(96.dp),
            tint = iconColor
        )
    }
}