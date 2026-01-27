package com.example.prototype

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.widget.Button
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE_CAPTURE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        copyTessData(this)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        findViewById<Button>(R.id.startBtn).setOnClickListener {
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_CODE_CAPTURE
            )
        }
    }

    private fun copyTessData(context: Context) {
        val tessdataDir = File(context.filesDir, "tessdata")
        if (!tessdataDir.exists()) tessdataDir.mkdirs()

        val file = File(tessdataDir, "eng.traineddata")
        if (!file.exists()) {
            context.assets.open("tessdata/eng.traineddata").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("OCR", "Copied eng.traineddata to ${file.absolutePath}")
        } else {
            Log.d("OCR", "Tessdata already exists at ${file.absolutePath}")
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CAPTURE &&
            resultCode == Activity.RESULT_OK &&
            data != null
        ) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(intent)
        }
    }
}