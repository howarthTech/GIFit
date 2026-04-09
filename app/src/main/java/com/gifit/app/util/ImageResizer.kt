package com.gifit.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ImageResizer {

    fun resizeBitmap(context: Context, uri: Uri, maxWidth: Int = 480): Bitmap {
        // First pass: decode bounds only
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate inSampleSize (power of 2)
        var sampleSize = 1
        while (originalWidth / (sampleSize * 2) >= maxWidth) {
            sampleSize *= 2
        }

        // Second pass: decode with sample size
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Cannot open image: $uri")

        // Fine scale to exact max width
        val scaled = if (sampled.width > maxWidth) {
            val ratio = maxWidth.toFloat() / sampled.width
            val targetHeight = (sampled.height * ratio).toInt()
            val result = Bitmap.createScaledBitmap(sampled, maxWidth, targetHeight, true)
            if (result !== sampled) sampled.recycle()
            result
        } else {
            sampled
        }

        // Handle EXIF rotation
        val rotation = getExifRotation(context, uri)
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            if (rotated !== scaled) scaled.recycle()
            rotated
        } else {
            scaled
        }
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
