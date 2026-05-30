package com.maheshz.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.maheshz.ui.theme.BgColor
import com.maheshz.ui.theme.BrandPurple
import com.maheshz.ui.theme.DarkPurple
import com.maheshz.ui.theme.LightPurple
import com.maheshz.ui.theme.PaleTeal
import com.maheshz.ui.theme.PrimaryText
import com.maheshz.ui.theme.SecondaryText
import com.maheshz.ui.theme.TealAccent
import com.maheshz.ui.viewmodel.CheckInViewModel
import com.maheshz.ui.viewmodel.HomeState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: CheckInViewModel) {
    val state by viewModel.uiState.collectAsState()
    val name by viewModel.empName.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is HomeState.Found) {
            val sig = viewModel.getSignatureObject()
            if (sig != null) {
                val activity = context as? FragmentActivity
                if (activity != null) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Verify your identity")
                        .setSubtitle("Touch fingerprint sensor to check in/out")
                        .setNegativeButtonText("Cancel")
                        .build()

                    val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            val signature = result.cryptoObject?.signature ?: return
                            val timestamp = System.currentTimeMillis() / 1000
                            val dataToSign = "${timestamp}".toByteArray()
                            signature.update(dataToSign)
                            viewModel.onFingerprintSuccess(signature.sign())
                        }
                    })

                    biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(sig))
                }
            }
        } else if (state is HomeState.Success || state is HomeState.Failed || state is HomeState.Timeout) {
            delay(3000)
            viewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgColor)) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LightPurple, CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "U",
                        color = DarkPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TECH SOLUTIONS INC.",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandPurple
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            
            // Scanner Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Outer Ring 1
                Box(modifier = Modifier.size(256.dp).border(2.dp, PaleTeal, CircleShape))
                // Outer Ring 2
                Box(modifier = Modifier.size(224.dp).border(4.dp, TealAccent.copy(alpha=0.4f), CircleShape))
                
                // Scanner Button
                FingerprintButton(state = state, onClick = {
                    if (state is HomeState.Idle) viewModel.startFlow()
                })
            }
            
            // Text Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (titleText, statusText, statusColor) = when (state) {
                    is HomeState.Idle -> Triple("TAP TO\nCHECK IN/OUT", "SCANNER READY", SecondaryText)
                    is HomeState.Scanning -> Triple("SEARCHING\nFOR SCANNER", "LOOKING FOR DEVICE", Color(0xFFFFA000))
                    is HomeState.Found -> Triple("VERIFY\nIDENTITY", "BIOMETRIC PROMPT", BrandPurple)
                    is HomeState.Broadcasting -> Triple("BROADCASTING\nSIGNAL", "SENDING TO SCANNER", DarkPurple)
                    is HomeState.Success -> Triple("SUCCESS\nVERIFIED", "VERIFICATION COMPLETE", TealAccent)
                    is HomeState.Failed -> Triple("FAILED\nERROR", "AUTHORIZATION FAILED", MaterialTheme.colorScheme.error)
                    is HomeState.Timeout -> Triple("TIMEOUT\nNO DEVICE", "COULDN'T CONNECT", MaterialTheme.colorScheme.error)
                }

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.displayLarge,
                    color = PrimaryText,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                }
            }
        }
        
        // Full screen status overlays for Success/Fail
        if (state is HomeState.Success) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha=0.9f)), contentAlignment = Alignment.Center) {
                Text("CHECK ${(state as HomeState.Success).eventType} ✓", color = Color.White, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
            }
        } else if (state is HomeState.Failed) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha=0.9f)), contentAlignment = Alignment.Center) {
                Text("FAILED\n${(state as HomeState.Failed).failureCode}", color = Color.White, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun FingerprintButton(state: HomeState, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state is HomeState.Scanning || state is HomeState.Broadcasting) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .size(192.dp)
            .scale(scale)
            .background(Color.White, CircleShape)
            .border(8.dp, TealAccent, CircleShape)
            .clickable(enabled = state is HomeState.Idle) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint, 
            contentDescription = "Fingerprint", 
            modifier = Modifier.size(96.dp),
            tint = TealAccent
        )
    }
}
