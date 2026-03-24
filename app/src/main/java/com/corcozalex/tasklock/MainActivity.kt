package com.corcozalex.tasklock

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.corcozalex.tasklock.network.NetworkClient
import com.corcozalex.tasklock.network.TokenManager
import com.corcozalex.tasklock.network.dataStore
import com.corcozalex.tasklock.ui.screens.DashboardScreen
import com.corcozalex.tasklock.ui.screens.LoginScreen
import com.corcozalex.tasklock.ui.screens.RegisterScreen
import com.corcozalex.tasklock.ui.theme.TaskLockTheme
import com.corcozalex.tasklock.viewmodel.AuthState
import com.corcozalex.tasklock.viewmodel.AuthViewModel
import com.corcozalex.tasklock.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            TaskLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val currentAuthState by authViewModel.authState.collectAsState()

                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val tokenManager = remember { TokenManager(context.dataStore) }

                    NavHost(navController = navController, startDestination = "splash") {

                        // ROUTE A: The Traffic Cop (Splash)
                        composable("splash") {
                            // Read the vault
                            val storedToken by tokenManager.getToken.collectAsState(initial = "CHECKING_VAULT")

                            LaunchedEffect(storedToken) {
                                if (storedToken != "CHECKING_VAULT") {
                                    if (storedToken.isNullOrBlank()) {
                                        // Vault is empty -> Go to Login
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        // Token found! -> Go to Dashboard
                                        navController.navigate("dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                            }
                        }

                        // ROUTE B: Login
                        composable("login") {
                            LaunchedEffect(currentAuthState) {
                                if (currentAuthState is AuthState.Success) {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }

                            LoginScreen(
                                authState = currentAuthState,
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                    authViewModel.logout()
                                }
                            )
                        }

                        // ROUTE C: Dashboard
                        composable("dashboard") {
                            // 1. Create the new brain for the dashboard
                            val dashboardViewModel: com.corcozalex.tasklock.viewmodel.DashboardViewModel = viewModel()

                            // 2. Listen to the brain's state
                            val dashboardState by dashboardViewModel.uiState.collectAsState()

                            // 3. Pass the state into the UI
                            DashboardScreen(
                                uiState = dashboardState,
                                onLogoutClick = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("register"){
                            RegisterScreen(
                                authState = currentAuthState,
                                onRegisterClick = { email, password ->
                                    authViewModel.register(email, password)
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack() // Go back to login
                                    authViewModel.logout()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}