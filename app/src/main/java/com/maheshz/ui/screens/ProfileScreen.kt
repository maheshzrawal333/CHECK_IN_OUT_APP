package com.maheshz.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    name: String
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Profile: $name", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Text("Logout")
        }
    }
}
