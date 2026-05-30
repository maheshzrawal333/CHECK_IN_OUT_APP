package com.maheshz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maheshz.ui.viewmodel.RegistrationViewModel
import com.maheshz.ui.viewmodel.RegistrationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    viewModel: RegistrationViewModel,
    onRegistered: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var orgCode by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Wrap the entire screen inside a full-sized Box to give it structure
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (state is RegistrationState.WaitingForApproval) {
            // Displayed ONLY when waiting for administrative approval
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Request Submitted!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Your request to join has been sent to the company administrator. Please wait for them to approve your access profile.",
                        textAlign = TextAlign.Center
                    )
                }
            }
        } // Inside your RegistrationScreen.kt - else block
        else {
            // 1. Establish a memory state for layout scroll tracking
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState) // FIX: Allows fields to scroll cleanly above the IME keyboard layer
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Employee Onboarding",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = orgCode,
                    onValueChange = { orgCode = it },
                    label = { Text("Organization Code") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.registerSimplified(orgCode, fullName, email) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state !is RegistrationState.Loading && orgCode.isNotBlank() && fullName.isNotBlank() && email.isNotBlank()
                ) {
                    if (state is RegistrationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Submit Request")
                    }
                }

                if (state is RegistrationState.Error) {
                    Text(
                        text = (state as RegistrationState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}