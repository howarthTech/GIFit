package com.gifit.app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object MediaStoreSaver {

    suspend fun saveGif(
        context: Context,
        gifBytes: ByteArray,
        fileName: String = "GIFit_${System.currentTimeMillis()}.gif"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStoreQ(context, gifBytes, fileName)
        } else {
            saveWithLegacy(context, gifBytes, fileName)
        }
    }

    /**
     * Write GIF bytes to a temp cache file and return a FileProvider URI for sharing.
     * Does not require WRITE_EXTERNAL_STORAGE.
     */
    fun shareTempGif(context: Context, gifBytes: ByteArray): Uri {
        val cacheDir = File(context.cacheDir, "shared_gifs")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val file = File(cacheDir, "GIFit_${System.currentTimeMillis()}.gif")
        file.writeBytes(gifBytes)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun saveWithMediaStoreQ(
        context: Context,
        gifBytes: ByteArray,
        fileName: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GIFit")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValues
        ) ?: return null

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(gifBytes)
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacy(
        context: Context,
        gifBytes: ByteArray,
        fileName: String
    ): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val gifitDir = File(picturesDir, "GIFit")
        if (!gifitDir.exists()) gifitDir.mkdirs()

        val file = File(gifitDir, fileName)
        file.writeBytes(gifBytes)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
            @Suppress("DEPRECATION")
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }

        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
}
