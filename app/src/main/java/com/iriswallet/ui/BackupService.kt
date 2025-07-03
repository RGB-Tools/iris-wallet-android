package com.iriswallet.ui

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.iriswallet.R
import com.iriswallet.data.BackupRepository
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val notification =
            AppUtils.createNotification(
                AppContainer.appContext,
                AppConstants.BACKUP_LOGS_NOTIFICATION_CHANNEL,
                AppConstants.BACKUP_LOGS_NOTIFICATION_ID,
                R.string.backup_notification_channel_name,
                R.string.backup_notification_channel_description,
                R.string.backup_notification_title,
                R.string.backup_notification_text,
                intent,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && notification != null) {
            startForeground(
                AppConstants.BACKUP_LOGS_NOTIFICATION_ID,
                notification,
                FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(AppConstants.BACKUP_LOGS_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        doBackup()
        return START_NOT_STICKY
    }

    private fun doBackup() {
        CoroutineScope(Dispatchers.IO).launch {
            val gAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
            Log.d(TAG, "Doing backup with last logged-in account $gAccount")
            try {
                BackupRepository.doBackup(gAccount!!)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed: $e")
                val intent = Intent(this@BackupService, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                AppUtils.createNotification(
                    AppContainer.appContext,
                    AppConstants.BACKUP_LOGS_NOTIFICATION_CHANNEL,
                    AppConstants.BACKUP_LOGS_NOTIFICATION_ID,
                    R.string.backup_notification_channel_name,
                    R.string.backup_notification_channel_description,
                    R.string.backup_notification_error_title,
                    R.string.backup_notification_error_text,
                    intent,
                )
            }
        }
    }
}
