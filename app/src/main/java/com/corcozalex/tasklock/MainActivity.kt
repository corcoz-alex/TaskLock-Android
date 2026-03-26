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
import com.corcozalex.tasklock.network.dataStore
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
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // --- CATCH BACKGROUND INTENTS ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Forces the Activity to update its current intent
        handleAlarmIntent(intent)
        isAlarmTriggeredFlow.value = intent.getBooleanExtra("IS_ALARM_TRIGGERED", false)
    }

    // --- CATCH COLD-START INTENTS ---
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkClient.initialize(applicationContext)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Check if the app was launched by the AlarmService
        handleAlarmIntent(intent)

        val initialAlarmTriggered = intent.getBooleanExtra("IS_ALARM_TRIGGERED", false)
        isAlarmTriggeredFlow.value = initialAlarmTriggered
        val initialRoute = if (initialAlarmTriggered) "alarm_active" else "splash"

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

                    val activity = context as? MainActivity
                    val isAlarmTriggered by isAlarmTriggeredFlow.collectAsState()
                    // Force the UI to hijack the screen if the alarm is triggered,
                    // completely ignoring Compose's saved state.
                    LaunchedEffect(isAlarmTriggered) {
                        if (isAlarmTriggered) {
                            navController.navigate("alarm_active") {
                                // Wipe the backstack so the user cannot swipe back to escape the alarm!
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = initialRoute) {

                        // ROUTE: Alarm Screen
                        composable("alarm_active") {
                            AlarmActiveScreen (
                                onEmergencyStop = {
                                    // 1. Kill the audio
                                    val stopIntent = Intent(context, AlarmService::class.java)
                                    context.stopService(stopIntent)

                                    // 2. Clear the screen flags so the phone can sleep again
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){
                                        setShowWhenLocked(false)
                                        setTurnScreenOn(false)
                                    }
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                                    // 3. Reset the master switch and intent so it doesn't loop
                                    activity?.intent?.removeExtra("IS_ALARM_TRIGGERED")
                                    isAlarmTriggeredFlow.value = false

                                    // 4. Navigate back to safety
                                    navController.navigate("dashboard") {
                                        popUpTo("alarm_active") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ROUTE: The Traffic Cop (Splash)
                        composable("splash") {
                            val storedToken by tokenManager.getToken.collectAsState(initial = "CHECKING_VAULT")
                            LaunchedEffect(storedToken) {
                                if (storedToken != "CHECKING_VAULT") {
                                    if (storedToken.isNullOrBlank()) {
                                        navController.navigate("login") { popUpTo("splash") { inclusive = true } }
                                    } else {
                                        navController.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
                            }
                        }

                        // ROUTE: Login
                        composable("login") {
                            LaunchedEffect(currentAuthState) {
                                if (currentAuthState is AuthState.Success) {
                                    navController.navigate("dashboard") { popUpTo("login") { inclusive = true } }
                                }
                            }
                            LoginScreen(
                                authState = currentAuthState,
                                onLoginClick = { email, password -> authViewModel.login(email, password) },
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
                                    navController.navigate("login") { popUpTo("dashboard") { inclusive = true } }
                                },
                                onCreateTaskClick = { title, description -> dashboardViewModel.createTask(title, description) },
                                onToggleTaskClick = { task -> dashboardViewModel.toggleTaskCompletion(task) },
                                onDeleteTaskClick = { taskId -> dashboardViewModel.deleteTask(taskId) },
                                onTestAlarmClick = { dashboardViewModel.scheduleTestAlarm(context) }
                            )
                        }

                        // ROUTE: Register
                        composable("register"){
                            RegisterScreen(
                                authState = currentAuthState,
                                onRegisterClick = { email, password -> authViewModel.register(email, password) },
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

    // --- THE WAKE-UP ENGINE ---
    private fun handleAlarmIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("IS_ALARM_TRIGGERED", false) == true) {

            // 1. Tell Compose to change the UI
            isAlarmTriggeredFlow.value = true

            // 2. Physically hijack the screen panel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)

                // Aggressively ask the OS to dismiss the lock screen
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.requestDismissKeyguard(this, null)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            // 3. Keep the screen awake so the user can use the camera
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val isGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!isGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}