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

    /**
     * Copy an already-encoded GIF [gifFile] into the public gallery (Pictures/GIFit).
     * Streams the file rather than buffering its bytes in memory.
     */
    suspend fun saveGif(
        context: Context,
        gifFile: File,
        fileName: String = "GIFit_${System.currentTimeMillis()}.gif"
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStoreQ(context, gifFile, fileName)
        } else {
            saveWithLegacy(context, gifFile, fileName)
        }
    }

    /**
     * Return a FileProvider URI for an already-written cache file so it can be shared.
     * The file must live under a path declared in res/xml/file_paths.xml.
     */
    fun fileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun saveWithMediaStoreQ(
        context: Context,
        gifFile: File,
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

        resolver.openOutputStream(uri)?.use { out ->
            gifFile.inputStream().use { it.copyTo(out) }
        }

        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveWithLegacy(
        context: Context,
        gifFile: File,
        fileName: String
    ): Uri? {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val gifitDir = File(picturesDir, "GIFit")
        if (!gifitDir.exists()) gifitDir.mkdirs()

        val file = File(gifitDir, fileName)
        gifFile.copyTo(file, overwrite = true)

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
