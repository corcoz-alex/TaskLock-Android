package com.corcozalex.tasklock.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    captureRequest: Int = 0,
    onCaptureStarted: () -> Unit = {},
    onPhotoCaptured: (String) -> Unit = {},
    onCameraError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Remember the camera provider so it doesn't get recreated on recomposition
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView fallback avoids black preview on some lock-screen/overlay paths.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val executor = ContextCompat.getMainExecutor(context)
        val cameraBindTask = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCaptureUseCase = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCaptureUseCase
                )
                imageCapture = imageCaptureUseCase
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera binding failed", e)
                onCameraError("Camera failed to start. Please retry.")
            }
        }

        cameraProviderFuture.addListener(cameraBindTask, executor)

        onDispose {
            if (cameraProviderFuture.isDone) {
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera unbind failed", e)
                }
            }
            imageCapture = null
        }
    }

    LaunchedEffect(captureRequest) {
        if (captureRequest <= 0) return@LaunchedEffect

        val activeImageCapture = imageCapture
        if (activeImageCapture == null) {
            onCameraError("Camera is not ready yet. Try again in a second.")
            return@LaunchedEffect
        }

        val outputDirectory = File(context.cacheDir, "alarm_captures").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val outputFile = File(outputDirectory, "alarm_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        onCaptureStarted()
        activeImageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(outputFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraPreview", "Photo capture failed", exception)
                    onCameraError("Photo capture failed. Please try again.")
                }
            }
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { previewView }
    )
}