package com.maheshz.checkinout.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maheshz.checkinout.ui.theme.BgColor
import com.maheshz.checkinout.ui.theme.BrandPurple
import com.maheshz.checkinout.ui.theme.DarkPurple
import com.maheshz.checkinout.ui.theme.LightPurple
import com.maheshz.checkinout.ui.theme.PrimaryText
import com.maheshz.checkinout.ui.theme.SecondaryText
import com.maheshz.checkinout.ui.theme.WhiteSurface
import com.maheshz.checkinout.ui.viewmodel.RegistrationViewModel

@Composable
fun WaitingForApprovalScreen(viewModel: RegistrationViewModel) { // 🌟 FIXED: Changed signature to accept RegistrationViewModel
    val context = LocalContext.current
    val statusMsg by viewModel.statusMessage.collectAsState()

    // Listen for status message state changes to trigger UI feedback toasts
    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
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
                    .background(LightPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Pending",
                    tint = DarkPurple,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Approval Pending",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your request to join the organization has been sent successfully. An administrator must approve your account profile before you can access the clock-in scanner system.",
                fontSize = 15.sp,
                color = SecondaryText,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Refresh button pulls approval status live from backend
            Button(
                onClick = { viewModel.checkApprovalStatus() }, // 🌟 FIXED: Calls the check function on the viewmodel directly
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("Refresh Status", color = WhiteSurface)
            }
        }
    }
}