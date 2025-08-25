package com.trackeco.trackeco.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun setupCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onCameraSetup: (Boolean) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Preview
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

                // Image capture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                // Image analysis for real-time processing (optional)
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // Select back camera as default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    )

                    onCameraSetup(true)
                    Log.d(TAG, "Camera setup successful")
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                    onCameraSetup(false)
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Camera initialization failed", exc)
                onCameraSetup(false)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun captureImage(onImageCaptured: (Uri?, String?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "Image capture is not initialized")
            onImageCaptured(null, "Camera not initialized")
            return
        }

        // Create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        
        // Create output file
        val photoFile = File(
            getOutputDirectory(),
            "$name.jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    onImageCaptured(null, "Photo capture failed: ${exception.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")
                    onImageCaptured(savedUri, null)
                }
            }
        )
    }

    fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "TrackEco").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            context.filesDir
        }
    }

    fun cleanup() {
        cameraExecutor.shutdown()
    }

    // Utility function to compress image if needed
    fun compressImageFile(imageFile: File, quality: Int = 80): File? {
        return try {
            val compressedFile = File(
                getOutputDirectory(),
                "compressed_${imageFile.name}"
            )
            
            // Implementation would use bitmap compression here
            // For now, return original file
            imageFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            null
        }
    }

    // Convert image to base64 for AI validation
    fun imageToBase64(imageFile: File): String? {
        return try {
            val bytes = imageFile.readBytes()
            android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to base64", e)
            null
        }
    }
}