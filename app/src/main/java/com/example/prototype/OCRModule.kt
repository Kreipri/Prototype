package com.example.prototype

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI

class OCRModule(private val context: Context) {

    fun extractText(bitmap: Bitmap): String {
        val preprocessed = preprocess(bitmap)
        return runOcr(preprocessed)
    }

    private fun preprocess(bitmap: Bitmap): Bitmap {
        // Placeholder: grayscale / thresholding can go here
        return bitmap
    }

    private fun runOcr(bitmap: Bitmap): String {
        val api = TessBaseAPI()
        api.init(context.filesDir.absolutePath, "eng")
        api.setImage(bitmap)
        val text = api.utF8Text ?: ""
        api.end()
        return text
    }
}