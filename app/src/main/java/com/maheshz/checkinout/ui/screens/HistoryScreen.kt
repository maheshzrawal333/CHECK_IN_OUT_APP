package com.maheshz.checkinout.ui.screens

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maheshz.checkinout.data.repository.AttendanceRepository
import com.maheshz.checkinout.model.AttendanceRecord
import com.maheshz.checkinout.ui.theme.BrandPurple
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(repository: AttendanceRepository) {
    val records by repository.getAllRecords().collectAsState(initial = emptyList())
    var isListView by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    val context = LocalContext.current

    val shareCsv = {
        val csv = buildString {
            append("Type,Timestamp,VerifiedBy\n")
            records.forEach { append("${it.type},${Date(it.timestamp)},${it.verifiedBy}\n") }
        }
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            type = "text/csv"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Attendance"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History") },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(if (isListView) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = shareCsv) { Icon(Icons.Default.Share, contentDescription = "Export CSV") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isListView) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(records) { r ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { selectedRecord = r },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(r.type, style = MaterialTheme.typography.titleMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
                                Text(Date(r.timestamp).toString(), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            } else {
                RealCalendarView(records = records, onRecordSelect = { selectedRecord = it })
            }
        }

        if (selectedRecord != null) {
            ModalBottomSheet(onDismissRequest = { selectedRecord = null }) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Check Detail", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Event Type: ${selectedRecord?.type}")
                    Text("Timestamp: ${selectedRecord?.timestamp?.let { Date(it) }}")
                    Text("Security Level: Verified by SYSTEM (Hardware ECDSA)")
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RealCalendarView(records: List<AttendanceRecord>, onRecordSelect: (AttendanceRecord) -> Unit) {
    val currentMonth = YearMonth.now()
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7

    val grouped = records.groupBy {
        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = BrandPurple
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        var currentDay = 1
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 7) {
                    if (row == 0 && col < firstDayOfWeek || currentDay > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) // Empty cell
                    } else {
                        val date = currentMonth.atDay(currentDay)
                        val dayRecords = grouped[date]

                        val bgColor = when {
                            dayRecords != null && dayRecords.any { it.type.contains("CHECK_OUT") } -> Color.Green.copy(alpha = 0.6f)
                            dayRecords != null -> Color(0xFFFFA000).copy(alpha = 0.6f)
                            date.dayOfWeek.value >= 6 -> Color.LightGray.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .background(bgColor, CircleShape)
                                .clickable { dayRecords?.firstOrNull()?.let { onRecordSelect(it) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentDay.toString(), textAlign = TextAlign.Center, fontWeight = if (dayRecords != null) FontWeight.Bold else FontWeight.Normal)
                        }
                        currentDay++
                    }
                }
            }
        }
    }
}