package com.maheshz.checkinout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maheshz.checkinout.ui.theme.BrandPurple
import com.maheshz.checkinout.ui.theme.LightPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onUnbindDevice: () -> Unit,
    name: String,
    orgCode: String
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Corporate Profile", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(LightPurple, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "U",
                    color = BrandPurple,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "Active Employee Status",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Business, contentDescription = "Organization", tint = BrandPurple)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Organization Code", fontWeight = FontWeight.Bold)
                            Text(orgCode.ifEmpty { "Unknown" }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = "Security", tint = BrandPurple)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Device Identity", fontWeight = FontWeight.Bold)
                            Text("Hardware Biometrics Bound", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showUnbindDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PhonelinkErase, contentDescription = "Unbind")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Unbind Corporate Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showUnbindDialog) {
            AlertDialog(
                onDismissRequest = { showUnbindDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error) },
                title = { Text("Remove Device?", textAlign = TextAlign.Center) },
                text = {
                    Text(
                        "This will destroy the cryptographic keys binding this phone to $orgCode. " +
                                "You will not be able to check in/out until your IT Administrator issues you a new Activation Code. \n\n" +
                                "Are you sure you want to proceed?",
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnbindDialog = false
                            onUnbindDevice()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Yes, Unbind Device")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnbindDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}