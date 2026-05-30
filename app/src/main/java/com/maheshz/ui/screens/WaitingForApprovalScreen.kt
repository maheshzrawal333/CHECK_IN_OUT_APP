package com.maheshz.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WaitingForApprovalScreen(onCheckStatus: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.maheshz.ui.theme.BgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pending Icon Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(com.maheshz.ui.theme.LightPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Pending",
                    tint = com.maheshz.ui.theme.DarkPurple,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Approval Pending",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = com.maheshz.ui.theme.PrimaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your request to join the organization has been sent successfully. An administrator must approve your account profile before you can access the clock-in scanner system.",
                fontSize = 15.sp,
                color = com.maheshz.ui.theme.SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Helpful button to test entry again once approved by admin
            Button(
                onClick = onCheckStatus,
                colors = ButtonDefaults.buttonColors(containerColor = com.maheshz.ui.theme.BrandPurple)
            ) {
                Text("Refresh Status", color = com.maheshz.ui.theme.WhiteSurface)
            }
        }
    }
}