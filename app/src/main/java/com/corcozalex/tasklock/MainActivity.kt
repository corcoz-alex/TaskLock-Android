package com.corcozalex.tasklock

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.corcozalex.tasklock.ui.screens.LoginScreen
import com.corcozalex.tasklock.ui.theme.TaskLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //tells app to draw under the system status bar
        enableEdgeToEdge()
        setContent {
            TaskLockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginClick = { enteredEmail, enteredPassword ->
                            Log.d(
                                "AuthTest",
                                "Login clicked! Email  $enteredEmail, Password: $enteredPassword"
                            )
                        },
                        onNavigateToRegister = {
                            Log.d("AuthTest", "Navigate to Register clicked!")
                        }
                    )
                }
            }
        }
    }
}