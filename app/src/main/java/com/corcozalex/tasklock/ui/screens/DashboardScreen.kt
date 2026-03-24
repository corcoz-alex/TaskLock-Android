package com.corcozalex.tasklock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.corcozalex.tasklock.viewmodel.DashboardState

@Composable
fun DashboardScreen(
    uiState: DashboardState,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        when (uiState) {
            is DashboardState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Decrypting secure profile...")
            }

            is DashboardState.Success -> {
                Text(
                    text = "Welcome to TaskLock",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Logged in as: ${uiState.userProfile.email}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Database ID: ${uiState.userProfile.id} | Active: ${uiState.userProfile.is_active}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            is DashboardState.Error -> {
                Text(
                    text = "Security Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogoutClick) {
            Text("Logout")
        }
    }
}