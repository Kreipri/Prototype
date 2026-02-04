package com.example.prototype.ui.child

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.example.prototype.service.ScreenCaptureService

/**
 * A transparent activity responsible for launching the system-level
 * Screen Capture permission request.
 */
class CapturePermissionActivity : Activity() {

    companion object {
        private const val REQUEST_CODE_CAPTURE = 1001
    }

    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // Launch the system dialog
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_CODE_CAPTURE
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Permission granted: Pass data to the Foreground Service
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(intent)
        }

        // Always finish so the activity doesn't stay in the backstack
        finish()
    }
}