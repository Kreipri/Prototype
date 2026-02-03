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
import java.io.File
import java.io.FileOutputStream
import com.googlecode.tesseract.android.TessBaseAPI

class ScreenCaptureService : Service() {

    private lateinit var projection: MediaProjection
    private lateinit var imageReader: ImageReader
    private val handler = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, notification())

        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager

        projection = mgr.getMediaProjection(
            intent.getIntExtra("resultCode", Activity.RESULT_CANCELED),
            intent.getParcelableExtra("data")!!
        ) ?: return START_NOT_STICKY
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("data", Intent::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")!!
        }

        projection = mgr.getMediaProjection(resultCode, resultData) ?: return START_NOT_STICKY

        CaptureState.isRunning = true

        // --- ADD THIS BLOCK ---
        // Notify OverlayService that we are live!
        sendBroadcast(Intent("com.example.prototype.CAPTURE_STARTED").apply {
            setPackage(packageName) // Security: keep broadcast within app
        })
        // ----------------------

        startCapture(intent)

        return START_STICKY
    }

    override fun onDestroy() {
        CaptureState.isRunning = false
        super.onDestroy()
    }

    private fun startCapture(intent: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val dataIntent: Intent = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("data", Intent::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("data")!!
        }

        projection = mgr.getMediaProjection(
            intent.getIntExtra("resultCode", Activity.RESULT_CANCELED),
            dataIntent
        ) ?: return

        // Use device screen size dynamically instead of hardcoded
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
                Log.d("ScreenCaptureService", "Screenshot capturing")
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val bitmap = imageToBitmap(image)
                    image.close()
                    // Run OCR off the main thread
                    Thread {
                        runOcr(bitmap)
                        bitmap.recycle()
                    }.start()

                    Log.d("ScreenCaptureService", "Screenshot processed")
                }
            }
            handler.postDelayed(this, 3000)
        }
    }

    // Convert Image to Bitmap
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

    // Stub for OCR processing
    private fun runOcr(bitmap: Bitmap) {
        try {
            val tessBaseAPI = TessBaseAPI()
            val tessDataPath = filesDir.absolutePath  // must contain tessdata/

            Log.d("OCR", "Files dir: ${filesDir.absolutePath}")
            Log.d("OCR", "Tessdata exists: ${File(filesDir, "tessdata").exists()}")

            tessBaseAPI.init(tessDataPath, "eng")      // now it will find tessdata/eng.traineddata
            tessBaseAPI.setImage(bitmap)
            val extractedText = tessBaseAPI.utF8Text
            tessBaseAPI.end()

            Log.d("ScreenCaptureService", "OCR Text: $extractedText")
        } catch (e: Exception) {
            Log.e("ScreenCaptureService", "OCR error", e)
        }
    }


    private fun notification(): Notification {
        val channelId = "capture"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
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


