package com.example.prototype

import android.app.*
import android.content.Context
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
import java.util.concurrent.atomic.AtomicInteger // IMPORT THIS
import java.io.File
import com.googlecode.tesseract.android.TessBaseAPI
import android.widget.Toast

class ScreenCaptureService : Service() {

    private lateinit var projection: MediaProjection
    private lateinit var imageReader: ImageReader
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "ScreenCaptureDebug"

    // --- ATOMIC COUNTERS (Thread Safe) ---
    // specific types that handle concurrency automatically
    private val attemptCount = AtomicInteger(0)
    private val successCaptureCount = AtomicInteger(0)
    private val ocrFinishedCount = AtomicInteger(0)
    // -------------------------------------

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, notification())

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

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

        projection = mgr.getMediaProjection(resultCode, data) ?: return START_NOT_STICKY
        CaptureState.isRunning = true

        sendBroadcast(Intent("com.example.prototype.CAPTURE_STARTED").apply {
            setPackage(packageName)
        })

        startCapture(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        CaptureState.isRunning = false
        super.onDestroy()
    }

    private fun startCapture(intent: Intent) {
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        projection.createVirtualDisplay(
            "capture",
            width, height, resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        handler.post(captureRunnable)
    }

    private val captureRunnable = object : Runnable {
        override fun run() {
            if (ScreenState.isFacebookOpen) {
                // ATOMIC INCREMENT
                val currentId = attemptCount.incrementAndGet()

                Log.d(TAG, "--- CYCLE #$currentId STARTED ---")

                val image = imageReader.acquireLatestImage()

                if (image != null) {
                    successCaptureCount.incrementAndGet()

                    val bitmap = imageToBitmap(image)
                    image.close()

                    // Start background thread for OCR
                    Thread {
                        runOcr(bitmap, currentId)
                        bitmap.recycle()
                    }.start()

                } else {
                    Log.w(TAG, "Cycle #$currentId: SKIPPED (No new image)")
                }
            }
            handler.postDelayed(this, 3000)
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun runOcr(bitmap: Bitmap, id: Int) {
        try {
            val tessBaseAPI = TessBaseAPI()
            val tessDataPath = filesDir.absolutePath

            tessBaseAPI.init(tessDataPath, "eng")
            tessBaseAPI.setImage(bitmap)
            val extractedText = tessBaseAPI.utF8Text
            tessBaseAPI.end()

            val finishedTotal = ocrFinishedCount.incrementAndGet()

            if (extractedText.isNotEmpty()) {
                // Run Analysis
                val analysis = TextAnalyzer.analyze(extractedText)

                if (!analysis.isClean) {
                    // Loop through all found words and save them
                    for (incident in analysis.incidents) {

                        Log.e(TAG, "🚨 DETECTED: ${incident.word} (${incident.severity})")

                        // SAVE TO STORAGE
                        IncidentLogger.logIncident(
                            context = applicationContext,
                            word = incident.word,
                            severity = incident.severity,
                            appName = "Facebook"
                        )
                    }

                    // Visual Feedback
                    val msg = analysis.incidents.joinToString { "${it.word} (${it.severity})" }
                    handler.post {
                        Toast.makeText(applicationContext, "⚠️ Logged: $msg", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.d(TAG, "Cycle #$id: OCR finished (empty).")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Cycle #$id: OCR FAILED", e)
        }
    }


    private fun notification(): Notification {
        val channelId = "capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Monitoring Active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onBind(intent: Intent?) = null

    object CaptureState {
        @Volatile var isRunning: Boolean = false
    }
}