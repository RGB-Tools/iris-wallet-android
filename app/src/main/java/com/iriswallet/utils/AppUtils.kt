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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.CharacterSetECI
import com.iriswallet.R
import com.iriswallet.data.SharedPreferencesManager
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lelloman.identicon.drawable.ClassicIdenticonDrawable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class AppUtils {
    companion object {
        private fun calculateAvailableWidth(
            windowManager: WindowManager,
            scale: Double = 1.0,
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

        fun copyFile(src: File, dst: File) {
            val inStream = FileInputStream(src)
            val outStream = FileOutputStream(dst)
            val inChannel = inStream.channel
            val outChannel = outStream.channel
            inChannel.transferTo(0, inChannel.size(), outChannel)
            inStream.close()
            outStream.close()
        }

        fun deleteAppData() {
            Log.i(TAG, "Deleting app data...")
            deleteRgbData()
            AppContainer.dbPath.delete()
            SharedPreferencesManager.clearAll()
        }

        fun deleteRgbData() {
            Log.i(TAG, "Deleting rgb data...")
            AppContainer.rgbDir.deleteRecursively()
            AppContainer.rgbDir.mkdir()
        }

        fun getAssetIdIdenticon(
            assetID: String,
            width: Int,
            height: Int,
        ): ClassicIdenticonDrawable {
            val errCorrection = 200
            val correctedWidth = width + errCorrection
            val correctedHeight = height + errCorrection
            return ClassicIdenticonDrawable(correctedWidth, correctedHeight, assetID.hashCode())
        }

        fun getQRCodeBitmap(
            data: String,
            windowManager: WindowManager,
            scale: Double = 1.0,
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
                    MediaStore.Images.Thumbnails.MINI_KIND,
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
                        fileName,
                    )
                URL(url).openStream().use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            }
        }

        fun createNotification(
            context: Context,
            channelId: String,
            notificationId: Int,
            channelNameId: Int,
            channelDescriptionId: Int,
            contentTitleId: Int,
            contentTextId: Int,
            intent: Intent,
            requestPermissionLauncher: ActivityResultLauncher<String>? = null,
        ): Notification? {
            val channel =
                NotificationChannel(
                        channelId,
                        context.getString(channelNameId),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    )
                    .apply { description = context.getString(channelDescriptionId) }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            val builder =
                NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(context.getString(contentTitleId))
                    .setContentText(context.getString(contentTextId))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (requestPermissionLauncher != null) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return null
                        } else {
                            Log.d(TAG, "Cannot request notification permissions")
                        }
                    }
                }
                val notification = builder.build()
                notify(notificationId, notification)
                return notification
            }
        }

        fun askForNotificationsPermission(
            context: Context,
            requestPermissionLauncher: ActivityResultLauncher<String>,
        ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "Requesting notification permissions...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return true
                }
            }
            return false
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

        fun getErrMsg(context: Context, baseMsgID: Int, extraMsg: String? = null): String {
            var errMsg = context.getString(baseMsgID)
            if (!extraMsg.isNullOrBlank())
                errMsg =
                    context.getString(
                        R.string.app_exception_msg,
                        errMsg,
                        extraMsg.replaceFirstChar(Char::lowercase),
                    )
            return errMsg
        }

        fun toastErrorFromFragment(fragment: Fragment, baseMsgID: Int, extraMsg: String? = null) {
            val errMsg = getErrMsg(fragment.requireContext(), baseMsgID, extraMsg)
            toastFromFragment(fragment, errMsg)
        }

        fun toastFromFragment(fragment: Fragment, msg: String) {
            Log.d(fragment.TAG, msg)
            Toast.makeText(fragment.activity, msg, Toast.LENGTH_LONG).show()
        }
    }
}

fun String.getSha256(): String {
    val digest = MessageDigest.getInstance("SHA-256").apply { reset() }
    val byteData: ByteArray = digest.digest(this.toByteArray())
    return StringBuffer()
        .apply {
            byteData.forEach { append(((it.toInt() and 0xff) + 0x100).toString(16).substring(1)) }
        }
        .toString()
}

class IntegerInputFilter(
    private val maxDigits: Int,
    private val minValue: Int? = null,
    private val maxValue: Int? = null,
) : InputFilter {

    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int,
    ): CharSequence? {
        if (source != "" && !source.matches(Regex("[0-9]*"))) return ""
        val newText =
            dest.substring(0, dstart) + source.subSequence(start, end) + dest.substring(dend)
        if (newText.length > maxDigits) return ""
        if (newText.isEmpty() || newText == "0") return newText
        return try {
            val value = newText.toInt()
            if ((minValue != null && value < minValue) || (maxValue != null && value > maxValue)) {
                ""
            } else {
                null
            }
        } catch (_: NumberFormatException) {
            ""
        }
    }
}

class LazyMutable<T>(val initializer: () -> T) : ReadWriteProperty<Any?, T> {
    private object UNINITIALIZED

    private var prop: Any? = UNINITIALIZED

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (prop == UNINITIALIZED) {
            synchronized(this) {
                return if (prop == UNINITIALIZED) initializer().also { prop = it } else prop as T
            }
        } else prop as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) { prop = value }
    }
}
