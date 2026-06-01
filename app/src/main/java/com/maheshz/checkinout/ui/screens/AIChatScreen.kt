package com.maheshz.checkinout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.maheshz.checkinout.data.remote.ApiService
import com.maheshz.checkinout.data.remote.ChatRequest
import com.maheshz.checkinout.util.DataStoreManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AI(val text: String) : ChatMessage()
    object Typing : ChatMessage()
}

class AIChatViewModel(
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private var sessionId: String = ""

    init {
        viewModelScope.launch {
            val savedSession = dataStoreManager.sessionIdFlow.firstOrNull()
            if (savedSession.isNullOrEmpty()) {
                sessionId = UUID.randomUUID().toString()
                dataStoreManager.saveSessionId(sessionId)
            } else {
                sessionId = savedSession
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            sessionId = UUID.randomUUID().toString()
            dataStoreManager.saveSessionId(sessionId)
            _messages.value = emptyList()
        }
    }

    fun sendMessage(text: String) {
        val currentMsgs = _messages.value.toMutableList()
        currentMsgs.add(ChatMessage.User(text))
        currentMsgs.add(ChatMessage.Typing)
        _messages.value = currentMsgs

        viewModelScope.launch {
            try {
                val response = apiService.chat(ChatRequest(text, sessionId))
                if (response.isSuccessful) {
                    // Quick simulation of streaming or just loading full string
                    val resString = response.body()?.string() ?: "No response"
                    val filteredMsgs = _messages.value.filter { it !is ChatMessage.Typing }.toMutableList()
                    filteredMsgs.add(ChatMessage.AI(resString))
                    _messages.value = filteredMsgs
                } else {
                    val filteredMsgs = _messages.value.filter { it !is ChatMessage.Typing }.toMutableList()
                    filteredMsgs.add(ChatMessage.AI("Sorry, I couldn't reach the server."))
                    _messages.value = filteredMsgs
                }
            } catch (e: Exception) {
                val filteredMsgs = _messages.value.filter { it !is ChatMessage.Typing }.toMutableList()
                filteredMsgs.add(ChatMessage.AI("Error: ${e.message}"))
                _messages.value = filteredMsgs
            }
        }
    }

    companion object {
        fun provideFactory(apiService: ApiService, dsm: DataStoreManager): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AIChatViewModel(apiService, dsm) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(viewModel: AIChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var currentText by remember { mutableStateOf("") }
    
    val quickChips = listOf("How many days this month?", "What time yesterday?", "My hours this week", "Last 5 check-ins")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
                actions = {
                    TextButton(onClick = { viewModel.clearChat() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { p ->
        Column(modifier = Modifier.padding(p).fillMaxSize()) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                items(messages) { msg ->
                    when (msg) {
                        is ChatMessage.User -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
                                Text(msg.text, modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)).padding(12.dp), color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        is ChatMessage.AI -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
                                Text(msg.text, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp))
                            }
                        }
                        is ChatMessage.Typing -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
                                Text("...", modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(12.dp))
                            }
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickChips.forEach { chip ->
                        SuggestionChip(onClick = { viewModel.sendMessage(chip) }, label = { Text(chip) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask something...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { 
                        if(currentText.isNotBlank()) {
                            viewModel.sendMessage(currentText)
                            currentText = ""
                        }
                    }, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
