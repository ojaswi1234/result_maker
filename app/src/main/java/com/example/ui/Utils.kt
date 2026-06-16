package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.ByteArrayOutputStream

val LocalLocale = staticCompositionLocalOf { "en" }

// URI Downscaling Base64 Encoder
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream?.close()

        val maxDim = 300
        val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
        val scaledBitmap = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val processedBitmap = processLogoBitmap(scaledBitmap)

        val outputStream = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val bytes = outputStream.toByteArray()
        
        if (processedBitmap != scaledBitmap) processedBitmap.recycle()
        if (scaledBitmap != bitmap) scaledBitmap.recycle()
        bitmap.recycle()

        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun processLogoBitmap(src: Bitmap): Bitmap {
    val width = src.width
    val height = src.height
    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val l = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()

        if (l > 250) {
            pixels[i] = 0x00FFFFFF
        }
    }
    dest.setPixels(pixels, 0, width, 0, 0, width, height)
    return dest
}
