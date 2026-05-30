package com.maheshz.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maheshz.data.repository.AttendanceRepository
import com.maheshz.model.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

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
            records.forEach {
                append("${it.type},${Date(it.timestamp)},${it.verifiedBy}\n")
            }
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
                title = { Text("Attendance") },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(if (isListView) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = shareCsv) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isListView) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(records) { r ->
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { selectedRecord = r }) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(r.type, style = MaterialTheme.typography.titleMedium)
                                Text(Date(r.timestamp).toString(), style = MaterialTheme.typography.bodyMedium)
                                Text("Verified by: ${r.verifiedBy}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                CalendarView(records = records) {
                    selectedRecord = it
                }
            }
        }

        if (selectedRecord != null) {
            ModalBottomSheet(onDismissRequest = { selectedRecord = null }) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text("Check Detail", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type: ${selectedRecord?.type}")
                    Text("Time: ${selectedRecord?.timestamp?.let { Date(it) }}")
                    Text("Verified by: SYSTEM")
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarView(records: List<AttendanceRecord>, onRecordSelect: (AttendanceRecord) -> Unit) {
    // Very basic placeholder representation of a calendar
    val fmt = SimpleDateFormat("dd", Locale.getDefault())
    val grouped = records.groupBy { fmt.format(Date(it.timestamp)) }
    
    // We just show a grid-like layout for days 1-30
    Column(modifier = Modifier.padding(16.dp)) {
        for (row in 0 until 5) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 7) {
                    val day = row * 7 + col + 1
                    if (day <= 30) {
                        val strDay = day.toString().padStart(2, '0')
                        val dayRecords = grouped[strDay]
                        
                        // Fake logic: if standard weekend, grey. Assume 6,7 are weekend
                        val isWeekend = col == 5 || col == 6
                        val bgColor = when {
                            isWeekend -> Color.LightGray
                            dayRecords != null && dayRecords.size >= 2 -> Color.Green.copy(alpha=0.6f)
                            dayRecords != null && dayRecords.size == 1 -> Color(0xFFFFA000).copy(alpha=0.6f)
                            else -> Color.Red.copy(alpha=0.3f)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(bgColor, CircleShape)
                                .clickable {
                                    dayRecords?.firstOrNull()?.let { onRecordSelect(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strDay, textAlign = TextAlign.Center)
                        }
                    } else {
                        Box(modifier = Modifier.size(40.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
