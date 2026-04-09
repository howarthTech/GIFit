package com.gifit.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gifit.app.gif.AnimatedGifEncoder
import com.gifit.app.model.QuantizerType
import com.gifit.app.util.ImageResizer
import com.gifit.app.util.MediaStoreSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Foreground service for GIF encoding that survives app backgrounding.
 * Reports progress via static SharedFlows observable from any ViewModel.
 */
class GifEncodingService : Service() {

    companion object {
        private const val CHANNEL_ID = "gif_encoding"
        private const val NOTIFICATION_ID = 1001

        private val _progress = MutableStateFlow(0f)
        val progress: StateFlow<Float> = _progress.asStateFlow()

        private val _result = MutableSharedFlow<GifEncodingResult>(replay = 1)
        val result: SharedFlow<GifEncodingResult> = _result.asSharedFlow()

        private val _isEncoding = MutableStateFlow(false)
        val isEncoding: StateFlow<Boolean> = _isEncoding.asStateFlow()
    }

    sealed class GifEncodingResult {
        data class Success(val gifBytes: ByteArray) : GifEncodingResult()
        data class Error(val message: String) : GifEncodingResult()
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Preparing GIF...", 0)
        startForeground(NOTIFICATION_ID, notification)

        _isEncoding.value = true
        _progress.value = 0f

        serviceScope.launch {
            try {
                val frameUris = intent?.getStringArrayListExtra("frame_uris") ?: emptyList()
                val frameDelays = intent?.getIntArrayExtra("frame_delays") ?: intArrayOf()
                val frameOverlays = intent?.getStringArrayListExtra("frame_overlays") ?: emptyList()
                val maxWidth = intent?.getIntExtra("max_width", 480) ?: 480
                val rotations = intent?.getIntArrayExtra("rotations") ?: intArrayOf()
                val flipsH = intent?.getBooleanArrayExtra("flips_h") ?: booleanArrayOf()
                val flipsV = intent?.getBooleanArrayExtra("flips_v") ?: booleanArrayOf()
                val quantizerName = intent?.getStringExtra("quantizer_type") ?: "MEDIAN_CUT"
                val quantizerType = QuantizerType.valueOf(quantizerName)

                if (frameUris.size < 2) {
                    _result.emit(GifEncodingResult.Error("At least 2 frames required"))
                    stopSelf()
                    return@launch
                }

                // Load and transform bitmaps
                val bitmaps = withContext(Dispatchers.IO) {
                    frameUris.mapIndexed { i, uriString ->
                        val uri = android.net.Uri.parse(uriString)
                        val bitmap = ImageResizer.resizeBitmap(this@GifEncodingService, uri, maxWidth)
                        val rotation = rotations.getOrElse(i) { 0 }
                        val flipH = flipsH.getOrElse(i) { false }
                        val flipV = flipsV.getOrElse(i) { false }

                        if (rotation != 0 || flipH || flipV) {
                            val matrix = android.graphics.Matrix()
                            if (flipH) matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                            if (flipV) matrix.postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
                            if (rotation != 0) matrix.postRotate(rotation.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
                            val result = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            if (result !== bitmap) bitmap.recycle()
                            result
                        } else {
                            bitmap
                        }
                    }
                }

                // Encode GIF
                val outputStream = ByteArrayOutputStream()
                val encoder = AnimatedGifEncoder()
                encoder.encode(
                    frames = bitmaps,
                    perFrameDelays = frameDelays.toList(),
                    outputStream = outputStream,
                    perFrameOverlays = frameOverlays.map { it.ifBlank { null } },
                    quantizerType = quantizerType,
                    onProgress = { current, total ->
                        val p = current.toFloat() / total
                        _progress.value = p
                        updateNotification("Encoding frame $current of $total", (p * 100).toInt())
                    }
                )

                // Clean up bitmaps
                bitmaps.forEach { it.recycle() }

                _result.emit(GifEncodingResult.Success(outputStream.toByteArray()))
            } catch (e: Exception) {
                _result.emit(GifEncodingResult.Error("Encoding failed: ${e.message}"))
            } finally {
                _isEncoding.value = false
                _progress.value = 1f
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GIF Encoding",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while creating GIFs"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GIFit")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String, progress: Int) {
        val notification = buildNotification(text, progress)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
}
