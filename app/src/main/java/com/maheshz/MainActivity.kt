package com.maheshz

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.maheshz.ui.screens.HistoryScreen
import com.maheshz.ui.screens.HomeScreen
import com.maheshz.ui.screens.ProfileScreen
import com.maheshz.ui.screens.RegistrationScreen
import com.maheshz.ui.theme.MyApplicationTheme
import com.maheshz.ui.viewmodel.CheckInViewModel
import com.maheshz.ui.viewmodel.RegistrationViewModel
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import com.maheshz.ui.screens.WaitingForApprovalScreen

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CheckInOutApp).container

        setContent {
            MyApplicationTheme {
                val token by container.dataStoreManager.accessTokenFlow.collectAsStateWithLifecycle(initialValue = null)
                val isPending by container.dataStoreManager.isPendingFlow.collectAsStateWithLifecycle(initialValue = false)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        token != null -> MainApp(container)
                        isPending -> WaitingForApprovalScreen(onCheckStatus = {})
                        else -> {
                            val regVm: RegistrationViewModel = viewModel(
                                factory = RegistrationViewModel.provideFactory(
                                    container.authRepository,
                                    container.dataStoreManager
                                )
                            )
                            RegistrationScreen(viewModel = regVm, onRegistered = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(container: com.maheshz.di.AppContainer) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            Surface(
                color = com.maheshz.ui.theme.WhiteSurface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Last Event row (visible only on Home, but keeping global for consistency or can be fixed. Let's make it global as a footer)
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    if (currentRoute == "home") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = com.maheshz.ui.theme.BrandPurple)
                                val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                Text("Last CHECK IN — ${sdf.format(java.util.Date())}", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TODAY", style = MaterialTheme.typography.labelSmall, color = com.maheshz.ui.theme.SecondaryText)
                                val sdfDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                                Text(sdfDate.format(java.util.Date()), fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontSize = 14.sp, color = com.maheshz.ui.theme.SecondaryText)
                            }
                        }
                        HorizontalDivider(color = com.maheshz.ui.theme.LightGrayBorder)
                    }

                    NavigationBar(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        containerColor = com.maheshz.ui.theme.WhiteSurface,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.maheshz.ui.theme.BrandPurple,
                                selectedTextColor = com.maheshz.ui.theme.BrandPurple,
                                unselectedIconColor = com.maheshz.ui.theme.SecondaryText,
                                unselectedTextColor = com.maheshz.ui.theme.SecondaryText,
                                indicatorColor = com.maheshz.ui.theme.NavIndicator
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                            label = { Text("History", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            selected = currentRoute == "history",
                            onClick = { navController.navigate("history") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.maheshz.ui.theme.BrandPurple,
                                selectedTextColor = com.maheshz.ui.theme.BrandPurple,
                                unselectedIconColor = com.maheshz.ui.theme.SecondaryText,
                                unselectedTextColor = com.maheshz.ui.theme.SecondaryText,
                                indicatorColor = com.maheshz.ui.theme.NavIndicator
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "AI Chat") },
                            label = { Text("AI Chat", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            selected = currentRoute == "chat",
                            onClick = { navController.navigate("chat") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.maheshz.ui.theme.BrandPurple,
                                selectedTextColor = com.maheshz.ui.theme.BrandPurple,
                                unselectedIconColor = com.maheshz.ui.theme.SecondaryText,
                                unselectedTextColor = com.maheshz.ui.theme.SecondaryText,
                                indicatorColor = com.maheshz.ui.theme.NavIndicator
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                            label = { Text("Profile", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                            selected = currentRoute == "profile",
                            onClick = { navController.navigate("profile") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.maheshz.ui.theme.BrandPurple,
                                selectedTextColor = com.maheshz.ui.theme.BrandPurple,
                                unselectedIconColor = com.maheshz.ui.theme.SecondaryText,
                                unselectedTextColor = com.maheshz.ui.theme.SecondaryText,
                                indicatorColor = com.maheshz.ui.theme.NavIndicator
                            )
                        )
                    }
                }
            }
        }
    ) { p ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(p)) {
            composable("home") {
                val vm: CheckInViewModel = viewModel(
                    factory = CheckInViewModel.provideFactory(
                        container.iotBeaconScanner,
                        container.fpPacketAdvertiser,
                        container.resultReceiver,
                        container.dataStoreManager,
                        container.attendanceRepository
                    )
                )
                HomeScreen(vm)
            }
            composable("history") {
                HistoryScreen(container.attendanceRepository)
            }
            composable("chat") {
                val vm: com.maheshz.ui.screens.AIChatViewModel = viewModel(
                    factory = com.maheshz.ui.screens.AIChatViewModel.provideFactory(
                        container.apiService,
                        container.dataStoreManager
                    )
                )
                com.maheshz.ui.screens.AIChatScreen(vm)
            }
            composable("profile") {

                val scope = rememberCoroutineScope()
                val name by container.dataStoreManager.fullNameFlow.collectAsState(initial = "")
                ProfileScreen(
                    onLogout = {
                        scope.launch { container.dataStoreManager.clear() }
                    },
                    name = name ?: ""
                )
            }
        }
    }
}
