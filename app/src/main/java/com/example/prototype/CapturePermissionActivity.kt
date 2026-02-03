package com.example.prototype

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle


class CapturePermissionActivity : Activity() {

    private lateinit var projectionManager: MediaProjectionManager
    private val REQUEST_CODE_CAPTURE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager

        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_CODE_CAPTURE
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CAPTURE &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(intent)
        }

        finish() // IMPORTANT: activity disappears immediately
    }


}