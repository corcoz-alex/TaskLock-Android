package com.corcozalex.tasklock.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.corcozalex.tasklock.ui.components.CameraPreview

@Composable
fun AlarmActiveScreen(
    onEmergencyStop: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var captureStatusMessage by remember { mutableStateOf<String?>(null) }
    var captureRequestCount by remember { mutableStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Ask for camera access the first time the alarm screen opens.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "WAKE UP!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp, top = 32.dp)
            )
            Text(
                text = "Time to go to the gym.\nTake a picture of your Toothbrush.",
                fontSize = 18.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f) // Takes up the remaining middle space
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(16.dp)) // Gives the camera smooth, rounded corners
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    captureRequest = captureRequestCount,
                    onCaptureStarted = {
                        isCapturing = true
                        cameraErrorMessage = null
                        captureStatusMessage = null
                    },
                    onPhotoCaptured = {
                        isCapturing = false
                        captureStatusMessage = "Photo captured successfully."
                    },
                    onCameraError = { message ->
                        isCapturing = false
                        cameraErrorMessage = message
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is required to show the wake-up camera.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }

        if (cameraErrorMessage != null) {
            Text(
                text = cameraErrorMessage!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        if (captureStatusMessage != null) {
            Text(
                text = captureStatusMessage!!,
                color = Color(0xFF7CFF8B),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
        }

        Button(
            onClick = { captureRequestCount += 1 },
            enabled = hasCameraPermission && !isCapturing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 12.dp)
        ) {
            Text(if (isCapturing) "SNAPPING..." else "SNAP PICTURE")
        }

        // Emergency Stop Button
        OutlinedButton(
            onClick = onEmergencyStop,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Text("EMERGENCY STOP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}