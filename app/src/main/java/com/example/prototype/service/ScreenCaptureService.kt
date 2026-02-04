package com.example.prototype.service

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.prototype.R
import com.example.prototype.data.IncidentRepository
import com.example.prototype.data.model.Incident
import com.example.prototype.domain.ContentAnalyzer
import com.googlecode.tesseract.android.TessBaseAPI
import java.util.concurrent.atomic.AtomicInteger
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

class ScreenCaptureService : Service() {

    // --- 1. CONSTANTS & PROPERTIES ---
    companion object {
        private const val TAG = "ScreenCaptureDebug"
        private const val SYNC_INTERVAL_MS = 28_800_000L // 8 Hours
    }

    private lateinit var projection: MediaProjection
    private lateinit var imageReader: ImageReader
    private val handler = Handler(Looper.getMainLooper())

    // Atomic Counters (Thread Safe)
    private val attemptCount = AtomicInteger(0)
    private val successCaptureCount = AtomicInteger(0)
    private val ocrFinishedCount = AtomicInteger(0)

    // --- 2. LIFECYCLE METHODS ---

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 1. Show Notification immediately (Required for Foreground Service)
        startForeground(1, createNotification())

        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")
        }

        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 2. Initialize Projection
        projection = mgr.getMediaProjection(resultCode, data) ?: return START_NOT_STICKY
        CaptureState.isRunning = true

        // 3. Broadcast State
        sendBroadcast(Intent("com.example.prototype.CAPTURE_STARTED").apply {
            setPackage(packageName)
        })

        // 4. Start Loops
        setupVirtualDisplay()
        handler.post(captureRunnable)
        handler.postDelayed(syncRunnable, SYNC_INTERVAL_MS)

        return START_STICKY
    }

    override fun onDestroy() {
        // Final sync on close (Using Repository, not FirebaseDirect)
        Thread { IncidentRepository.syncData(applicationContext) }.start()

        handler.removeCallbacks(syncRunnable)
        handler.removeCallbacks(captureRunnable)

        CaptureState.isRunning = false

        if (::projection.isInitialized) {
            projection.stop()
        }

        super.onDestroy()
    }

    // --- 3. PRIVATE CORE METHODS (Logic) ---

    private fun setupVirtualDisplay() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        projection.createVirtualDisplay(
            "capture",
            width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
    }

    private fun runOcr(bitmap: Bitmap, id: Int) {
        try {
            // A. Downscale to 50% to save CPU/Battery
            val width = bitmap.width / 2
            val height = bitmap.height / 2
            val scaledBitmap = bitmap.scale(width, height, false)

            // B. Initialize Tesseract
            // (Note: For production, we'd initialize this ONCE in onCreate to save CPU)
            val tessBaseAPI = TessBaseAPI()
            val tessDataPath = filesDir.absolutePath
            tessBaseAPI.init(tessDataPath, "eng")
            tessBaseAPI.setImage(scaledBitmap)

            val extractedText = tessBaseAPI.utF8Text
            tessBaseAPI.end()
            scaledBitmap.recycle() // Clean up memory

            ocrFinishedCount.incrementAndGet()

            // C. Domain Logic (The "Brain")
            if (extractedText.isNotEmpty()) {
                val analysis = ContentAnalyzer.analyze(extractedText)

                if (!analysis.isClean) {
                    for (violation in analysis.incidents) {
                        Log.e(TAG, "🚨 DETECTED: ${violation.word} (${violation.severity})")

                        val incident = Incident(
                            word = violation.word,
                            severity = violation.severity,
                            appName = "Facebook" // In future, use AccessibilityService to detect real app
                        )

                        // D. Data Layer (The Repository handles Saving & Syncing)
                        IncidentRepository.saveIncident(applicationContext, incident)
                    }
                }
            } else {
                Log.d(TAG, "Cycle #$id: OCR finished (empty).")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Cycle #$id: OCR FAILED", e)
        }
    }

    // --- 4. RUNNABLES (Loops) ---

    private val captureRunnable = object : Runnable {
        override fun run() {
            // Only capture if we think Facebook is open (Controlled by AccessibilityService)
            // Or remove this check if you want to capture EVERYTHING for testing
            if (ScreenState.isFacebookOpen) {

                val currentId = attemptCount.incrementAndGet()
                val image = imageReader.acquireLatestImage()

                if (image != null) {
                    successCaptureCount.incrementAndGet()
                    val bitmap = imageToBitmap(image)
                    image.close()

                    // Run OCR in background thread
                    Thread {
                        runOcr(bitmap, currentId)
                        bitmap.recycle()
                    }.start()
                }
            }
            // Repeat every 3 seconds
            handler.postDelayed(this, 3000)
        }
    }

    private val syncRunnable = object : Runnable {
        override fun run() {
            Log.d(TAG, "⏰ Scheduled Sync starting...")
            Thread {
                IncidentRepository.syncData(applicationContext)
            }.start()
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    // --- 5. HELPER METHODS ---

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop the padding out
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun createNotification(): Notification {
        val channelId = "capture_channel"
        val channel = NotificationChannel(channelId, "Screen Monitor", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        return Notification.Builder(this, channelId)
            .setContentTitle("SafeMonitor Active")
            .setContentText("Scanning for harmful content...")
            .setSmallIcon(R.drawable.ic_menu_camera) // Ensure this icon exists!
            .build()
    }

    // --- 6. INNER OBJECTS / STATE ---

    // Global State used by AccessibilityService to pause/resume capture
    object CaptureState {
        @Volatile var isRunning: Boolean = false
    }

    // Simple state holder for external services to toggle
    object ScreenState {
        @Volatile var isFacebookOpen: Boolean = true // Default to true for testing
    }
}