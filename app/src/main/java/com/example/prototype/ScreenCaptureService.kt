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
import java.util.concurrent.atomic.AtomicLong

class ScreenCaptureService : Service() {

    private lateinit var projection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var ocrModule: OCRModule
    private lateinit var textPreprocessingModule: TextPreprocessingModule

    private val handler = Handler(Looper.getMainLooper())
    private val screenshotCounter = AtomicLong(0)

    override fun onCreate() {
        super.onCreate()
        ocrModule = OCRModule(applicationContext)
        textPreprocessingModule = TextPreprocessingModule()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, notification())
        startCapture(intent)
        return START_STICKY
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
                    val screenshotId = screenshotCounter.incrementAndGet()
                    Log.d("ScreenCaptureService", "Screenshot acquired #$screenshotId")

                    Thread {
                        val text = ocrModule.extractText(bitmap)
                        val preprocessedText = textPreprocessingModule.preprocessText(text)
                        Log.d("ScreenCaptureService", "OCR text from screenshot #$screenshotId: $text")
                        Log.d("ScreenCaptureService", "Preprocessed text from screenshot #$screenshotId: $preprocessedText")
                        bitmap.recycle()
                    }.start()
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
//    private fun runOcr(bitmap: Bitmap) {
//        try {
//            val tessBaseAPI = TessBaseAPI()
//            val tessDataPath = filesDir.absolutePath  // must contain tessdata/
//
//            Log.d("OCR", "Files dir: ${filesDir.absolutePath}")
//            Log.d("OCR", "Tessdata exists: ${File(filesDir, "tessdata").exists()}")
//
//            Log.d("OCR","Sending to OCR.")
//            tessBaseAPI.init(tessDataPath, "eng")      // now it will find tessdata/eng.traineddata
//            tessBaseAPI.setImage(bitmap)
//            val extractedText = tessBaseAPI.utF8Text
//            tessBaseAPI.end()
//
//            Log.d("ScreenCaptureService", "OCR Text: $extractedText")
//        } catch (e: Exception) {
//            Log.e("ScreenCaptureService", "OCR error", e)
//        }
//    }


    private fun notification(): Notification {
        val channelId = "screen_capture"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, channelId)
            .setContentTitle("Screen Capture Service")
            .setContentText("Monitoring screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = Notification.Builder(this, "screen_capture")
            .setContentTitle("Screen Capture Service")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }

    override fun onBind(intent: Intent?) = null
}
