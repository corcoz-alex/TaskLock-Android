package com.corcozalex.tasklock

import android.Manifest
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
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
import com.corcozalex.tasklock.service.AlarmService
import com.corcozalex.tasklock.ui.screens.AlarmActiveScreen
import com.corcozalex.tasklock.ui.screens.DashboardScreen
import com.corcozalex.tasklock.ui.screens.LoginScreen
import com.corcozalex.tasklock.ui.screens.RegisterScreen
import com.corcozalex.tasklock.ui.theme.TaskLockTheme
import com.corcozalex.tasklock.viewmodel.AuthState
import com.corcozalex.tasklock.viewmodel.AuthViewModel
import com.corcozalex.tasklock.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val isAlarmTriggeredFlow = MutableStateFlow(false)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // handle denied permissions later if needed
    }

    // --- CATCH COLD-START INTENTS ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.initialize(applicationContext)
        enableEdgeToEdge()
        requestPermissionsIfNeeded()

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


                    NavHost(navController = navController, startDestination = "splash") {


                        // ROUTE: The Traffic Cop (Splash)
                        composable("splash") {
                            LaunchedEffect(Unit) {
                                val refreshToken = NetworkClient.tokenManager.getRefreshToken()
                                if (!refreshToken.isNullOrBlank()) {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                            // loading spinner
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                            }
                        }

                        // ROUTE: Login
                        composable("login") {
                            LaunchedEffect(currentAuthState) {
                                if (currentAuthState is AuthState.Success) {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                            LoginScreen(
                                authState = currentAuthState,
                                onLoginClick = { email, password ->
                                    authViewModel.login(
                                        email,
                                        password
                                    )
                                },
                                onNavigateToRegister = {
                                    navController.navigate("register")
                                    authViewModel.logout()
                                }
                            )
                        }

                        // ROUTE: Dashboard
                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel()
                            val dashboardState by dashboardViewModel.uiState.collectAsState()

                            DashboardScreen(
                                uiState = dashboardState,
                                onLogoutClick = {
                                    authViewModel.logout()
                                    navController.navigate("login") {
                                        popUpTo("dashboard") {
                                            inclusive = true
                                        }
                                    }
                                },
                                onCreateTaskClick = { title, description, scheduledAtMillis, repeatMode, repeatDayOfWeek, requiredObject ->
                                    dashboardViewModel.createTask(
                                        context,
                                        title,
                                        description,
                                        scheduledAtMillis,
                                        repeatMode,
                                        repeatDayOfWeek,
                                        requiredObject
                                    )
                                },
                                onUpdateTaskClick = { task, title, description, scheduledAtMillis, repeatMode, repeatDayOfWeek, requiredObject ->
                                    dashboardViewModel.updateTask(
                                        context,
                                        task,
                                        title,
                                        description,
                                        scheduledAtMillis,
                                        repeatMode,
                                        repeatDayOfWeek,
                                        requiredObject
                                    )
                                },
                                onToggleTaskClick = { task ->
                                    dashboardViewModel.toggleTaskCompletion(
                                        context,
                                        task
                                    )
                                },
                                onDeleteTaskClick = { taskId -> dashboardViewModel.deleteTask(context, taskId) },
                                onSyncTaskAlarms = { tasks ->
                                    dashboardViewModel.syncTaskAlarms(context, tasks)
                                },
                                onTestAlarmClick = { dashboardViewModel.scheduleTestAlarm(context) }
                            )
                        }

                        // ROUTE: Register
                        composable("register") {
                            RegisterScreen(
                                authState = currentAuthState,
                                onRegisterClick = { email, password, confirmPassword->
                                    authViewModel.register(
                                        email,
                                        password,
                                        confirmPassword
                                    )
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                    authViewModel.logout()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}