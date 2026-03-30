package com.corcozalex.tasklock.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

class ObjectAnalyzer(
    context: Context,
    private val targetObject: String,
    private val onLabelsDetected: (List<String>) -> Unit
) : ImageAnalysis.Analyzer {

    private val localModel = LocalModel.Builder()
        .setAssetFilePath("tasklock_model.tflite")
        .build()

    private val labelNames: List<String> = context.assets.open("labels.txt")
        .bufferedReader()
        .readLines()
        .map { line ->
            line.substringAfter(" ").trim().lowercase()
        }

    private val customOptions = CustomImageLabelerOptions.Builder(localModel)
        .setMaxResultCount(3)
        .build()

    private val labeler = ImageLabeling.getClient(customOptions)

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val fullBitmap = imageProxy.toBitmap()
            val fullWidth = fullBitmap.width
            val fullHeight = fullBitmap.height

            val cropWidth: Int
            val cropHeight: Int

            if (targetObject.lowercase() == "toothbrush") {
                cropWidth = (fullWidth * 0.35).toInt()
                cropHeight = (fullHeight * 0.80).toInt()
            } else {
                val shortestSide = minOf(fullWidth, fullHeight)
                val size = (shortestSide * 0.70).toInt()
                cropWidth = size
                cropHeight = size
            }

            val startX = (fullWidth - cropWidth) / 2
            val startY = (fullHeight - cropHeight) / 2

            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap, startX, startY, cropWidth, cropHeight
            )

            val image = InputImage.fromBitmap(croppedBitmap, 0)

            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val highConfidenceLabels = labels
                        .filter { it.confidence > 0.80f }
                        .map { label ->
                            val word = labelNames.getOrElse(label.index) { "unknown" }
                            Log.d("ML_DEBUG", "Guessed: $word at ${label.confidence * 100}%")
                            word
                        }
                    onLabelsDetected(highConfidenceLabels)
                }
                .addOnFailureListener { e -> Log.e("ML_ERROR", "Labeling failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } catch (e: Exception) {
            Log.e("ML_ERROR", "Cropping failed!", e)
            imageProxy.close()
        }
    }
}