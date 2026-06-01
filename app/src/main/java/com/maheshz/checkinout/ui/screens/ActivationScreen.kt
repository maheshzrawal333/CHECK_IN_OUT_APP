package com.maheshz.checkinout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maheshz.checkinout.ui.viewmodel.RegistrationState
import com.maheshz.checkinout.ui.viewmodel.RegistrationViewModel

@Composable
fun ActivationScreen(viewModel: RegistrationViewModel, onActivated: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var activationCode by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is RegistrationState.Success) {
            onActivated()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "Key",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Device Activation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Enter the Activation Code provided by your HR Administrator to link this device securely to your profile.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            OutlinedTextField(
                value = activationCode,
                onValueChange = { activationCode = it.uppercase() },
                label = { Text("Activation Code (e.g. EZZY-A1B2C3)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state is RegistrationState.Error) {
                Text(
                    text = (state as RegistrationState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { viewModel.activateWithCode(activationCode) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = activationCode.length > 5 && state !is RegistrationState.Loading
            ) {
                if (state is RegistrationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Activate Device")
                }
            }
        }
    }
}