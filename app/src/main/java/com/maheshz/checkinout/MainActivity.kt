package com.maheshz.checkinout

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.maheshz.checkinout.di.AppContainer
import com.maheshz.checkinout.ui.screens.AIChatScreen
import com.maheshz.checkinout.ui.screens.AIChatViewModel
import com.maheshz.checkinout.ui.screens.ActivationScreen
import com.maheshz.checkinout.ui.screens.HistoryScreen
import com.maheshz.checkinout.ui.screens.HomeScreen
import com.maheshz.checkinout.ui.screens.ProfileScreen
import com.maheshz.checkinout.ui.theme.BrandPurple
import com.maheshz.checkinout.ui.theme.MyApplicationTheme
import com.maheshz.checkinout.ui.theme.NavIndicator
import com.maheshz.checkinout.ui.theme.SecondaryText
import com.maheshz.checkinout.ui.theme.WhiteSurface
import com.maheshz.checkinout.ui.viewmodel.CheckInViewModel
import com.maheshz.checkinout.ui.viewmodel.RegistrationViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CheckInOutApp).container

        setContent {
            MyApplicationTheme {
                val empCode by container.dataStoreManager.employeeCodeFlow.collectAsStateWithLifecycle(initialValue = null)

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val regVm: RegistrationViewModel = viewModel(
                        factory = RegistrationViewModel.provideFactory(
                            container.authRepository,
                            container.dataStoreManager
                        )
                    )

                    if (empCode != null) {
                        MainApp(container)
                    } else {
                        ActivationScreen(viewModel = regVm, onActivated = { })
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainApp(container: AppContainer) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            Surface(
                color = WhiteSurface,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBar(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    containerColor = WhiteSurface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = NavIndicator
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "history",
                        onClick = { navController.navigate("history") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = NavIndicator
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "AI Chat") },
                        label = { Text("AI Chat", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "chat",
                        onClick = { navController.navigate("chat") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = NavIndicator
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile", fontWeight = FontWeight.Bold) },
                        selected = currentRoute == "profile",
                        onClick = { navController.navigate("profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = SecondaryText,
                            unselectedTextColor = SecondaryText,
                            indicatorColor = NavIndicator
                        )
                    )
                }
            }
        }
    ) { p ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(p)) {
            composable("home") {
                val vm: CheckInViewModel = viewModel(
                    factory = CheckInViewModel.provideFactory(
                        container.fpPacketAdvertiser,
                        container.dataStoreManager,
                        container.attendanceRepository
                    )
                )
                HomeScreen(vm)
            }
            composable("history") { HistoryScreen(container.attendanceRepository) }
            composable("chat") {
                val vm: AIChatViewModel = viewModel(
                    factory = AIChatViewModel.provideFactory(container.apiService, container.dataStoreManager)
                )
                AIChatScreen(vm)
            }
            composable("profile") {
                val scope = rememberCoroutineScope()
                val name by container.dataStoreManager.fullNameFlow.collectAsState(initial = "")
                val orgCode by container.dataStoreManager.orgCodeFlow.collectAsState(initial = "")
                val empCode by container.dataStoreManager.employeeCodeFlow.collectAsState(initial = null)

                ProfileScreen(
                    onUnbindDevice = {
                        scope.launch {
                            try {
                                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
                                keyStore.load(null)
                                if (empCode != null && keyStore.containsAlias(empCode)) {
                                    keyStore.deleteEntry(empCode)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            container.dataStoreManager.clear()
                        }
                    },
                    name = name ?: "Employee",
                    orgCode = orgCode ?: "Company"
                )
            }
        }
    }
}