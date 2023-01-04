package com.iriswallet.utils

import android.Manifest
import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BulletSpan
import android.util.Log
import android.util.Size
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.CharacterSetECI
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lelloman.identicon.drawable.ClassicIdenticonDrawable
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class AppUtils {
    companion object {
        private fun calculateAvailableWidth(
            windowManager: WindowManager,
            scale: Double = 1.0
        ): Int {
            if (scale > 1.0 || scale < 0.1)
                throw IllegalArgumentException("QR code scale cannot be outside the range 0.1-1.0")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val insets =
                    windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                        WindowInsets.Type.systemBars()
                    )
                ((windowMetrics.bounds.width() - insets.left - insets.right) * scale).toInt()
            } else {
                val size = Point()
                windowManager.defaultDisplay.getSize(size)
                (size.x * scale).toInt()
            }
        }

        fun deleteAppData() {
            Log.i(TAG, "Deleting app data...")
            AppContainer.bdkDir.deleteRecursively()
            AppContainer.rgbDir.deleteRecursively()
            AppContainer.dbPath.delete()
            SharedPreferencesManager.clearAll()
        }

        fun getAssetIdIdenticon(
            assetID: String,
            width: Int,
            height: Int
        ): ClassicIdenticonDrawable {
            val errCorrection = 200
            val correctedWidth = width + errCorrection
            val correctedHeight = height + errCorrection
            return ClassicIdenticonDrawable(correctedWidth, correctedHeight, assetID.hashCode())
        }

        fun getQRCodeBitmap(
            data: String,
            windowManager: WindowManager,
            scale: Double = 1.0
        ): Bitmap? {
            val width = calculateAvailableWidth(windowManager, scale)
            val hints: MutableMap<EncodeHintType, Any> = EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = CharacterSetECI.UTF8
            hints[EncodeHintType.MARGIN] = 2
            return BarcodeEncoder().encodeBitmap(data, BarcodeFormat.QR_CODE, width, width, hints)
        }

        fun getImageThumbnail(filePath: String, width: Int, height: Int): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ThumbnailUtils.createImageThumbnail(File(filePath), Size(width, height), null)
            } else {
                ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(filePath), width, height)
            }
        }

        fun getVideoThumbnail(filePath: String, width: Int, height: Int): Bitmap? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ThumbnailUtils.createVideoThumbnail(File(filePath), Size(width, height), null)
            else
                ThumbnailUtils.createVideoThumbnail(
                    filePath,
                    MediaStore.Images.Thumbnails.MINI_KIND
                )
        }

        fun getRgbDir(parentDir: File): File {
            return File(parentDir, AppConstants.rgbDirName)
        }

        fun saveFileToDownloads(context: Context, url: String, fileName: String, mimeType: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    URL(url).openStream().use { input ->
                        resolver.openOutputStream(uri).use { output ->
                            input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                        }
                    }
                }
            } else {
                val target =
                    File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ),
                        fileName
                    )
                URL(url).openStream().use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        }

        fun showDownloadedNotification(
            context: Context,
            channelId: String,
            notificationId: Int,
            channelNameId: Int,
            channelDescriptionId: Int,
            contentTitleId: Int,
            contentTextId: Int,
            requestPermissionLauncher: ActivityResultLauncher<String>
        ) {
            val channel =
                NotificationChannel(
                        channelId,
                        context.getString(channelNameId),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    .apply { description = context.getString(channelDescriptionId) }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            val intent = Intent()
            intent.action = DownloadManager.ACTION_VIEW_DOWNLOADS

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            val builder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(context.getString(contentTitleId))
                    .setContentText(context.getString(contentTextId))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return
                    }
                }
                notify(notificationId, builder.build())
            }
        }

        fun List<String>.toBulletedList(color: Int = 0): CharSequence {
            return SpannableString(this.joinToString("\n")).apply {
                this@toBulletedList.foldIndexed(0) { index, acc, span ->
                    val end =
                        acc + span.length + if (index != this@toBulletedList.size - 1) 1 else 0
                    this.setSpan(BulletSpan(16, color), acc, end, 0)
                    end
                }
            }
        }

        fun uLongAbsDiff(first: ULong, second: ULong): ULong {
            return if (first > second) first - second else second - first
        }
    }
}

class AsciiInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence {
        for (i in start until end) {
            if (source[i].code < 32 || source[i].code > 127) {
                return ""
            }
        }
        return source
    }
}

class DecimalsInputFilter(maxIntegerPlaces: Int, maxDecimalPlaces: Int, minValue: Double? = null) :
    InputFilter {
    private val maxIntegerPlaces: Int
    private val maxDecimalPlaces: Int
    private val minValue: Double?

    init {
        this.maxIntegerPlaces = maxIntegerPlaces
        this.maxDecimalPlaces = maxDecimalPlaces
        this.minValue = minValue
    }

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence {
        if (source != "" && !source.matches(Regex("[0-9.]*"))) return ""

        val formattedSource = source.subSequence(start, end).toString()
        val destPrefix = dest.subSequence(0, dstart).toString()
        val destSuffix = dest.subSequence(dend, dest.length).toString()
        val result = destPrefix + formattedSource + destSuffix

        if (result.count { it == '.' } > 1) return ""
        if (result.startsWith(".")) return minValue.toString().split(".").first().toString()
        if (minValue != null) {
            if (result == "" || result.toDouble() < minValue) return minValue.toString()
        }

        val integerPlaces = result.indexOf(".")
        if (integerPlaces == -1 && result.length > maxIntegerPlaces && source != ".") return ""
        val decimalPlaces = if (integerPlaces == -1) 0 else result.length - integerPlaces - 1
        if (integerPlaces > maxIntegerPlaces) return ""
        if (decimalPlaces > maxDecimalPlaces) return ""

        return source
    }
}
