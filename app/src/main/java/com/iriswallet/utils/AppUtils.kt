package com.iriswallet.utils

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import com.iriswallet.data.SharedPreferencesManager

class AppUtils {
    companion object {
        fun deleteAppData() {
            AppContainer.bdkDir.deleteRecursively()
            AppContainer.rgbDir.deleteRecursively()
            AppContainer.dbPath.delete()
            SharedPreferencesManager.clearAll()
        }

        fun getQRCodeBitmap(
            data: String,
            windowManager: WindowManager,
            scale: Double = 1.0
        ): Bitmap? {
            if (scale > 1.0 || scale < 0.1)
                throw AppException("QR code scale cannot be outside the range 0.1-1.0")
            val width: Int =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
            return BarcodeEncoder().encodeBitmap(data, BarcodeFormat.QR_CODE, width, width)
        }

        fun getRgbDir(parentDir: File): File {
            return File(parentDir, AppConstants.rgbDirName)
        }

        fun uLongAbsDiff(first: ULong, second: ULong): ULong {
            return if (first > second) first - second else second - first
        }
    }
}
