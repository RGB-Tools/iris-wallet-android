package com.iriswallet.ui

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.IBinder
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.Identity
import com.iriswallet.R
import com.iriswallet.data.BackupRepository
import com.iriswallet.data.RgbRepository
import com.iriswallet.data.SharedPreferencesManager
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import com.iriswallet.utils.GoogleDriveAuthHelper
import com.iriswallet.utils.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BackupService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var authorizationClient: AuthorizationClient

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
        if (notification != null) {
            startForeground(
                AppConstants.BACKUP_LOGS_NOTIFICATION_ID,
                notification,
                FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(AppConstants.BACKUP_LOGS_NOTIFICATION_ID, notification)
        }
        authorizationClient = Identity.getAuthorizationClient(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        doBackup()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        RgbRepository.closeWallet()
        serviceJob.cancel()
        super.onDestroy()
        Log.d(TAG, "BackupService destroyed and job cancelled.")
    }

    private fun doBackup() {
        serviceScope.launch {
            val authRequest = GoogleDriveAuthHelper.createDriveAuthorizationRequest()
            Log.d(TAG, "Requesting Drive authorization in service...")
            val authResult = authorizationClient.authorize(authRequest).await()
            if (authResult.hasResolution()) {
                Log.e(TAG, "Drive authorization requires user resolution, service cannot proceed")
                SharedPreferencesManager.backupGoogleAccount = null
                stopSelf()
                return@launch
            }
            val driveAccessToken = authResult.accessToken
            if (driveAccessToken == null) {
                Log.e(TAG, "Drive access token is null, service cannot proceed")
                SharedPreferencesManager.backupGoogleAccount = null
                stopSelf()
                return@launch
            }
            Log.d(TAG, "Initializing Drive client in service...")
            val driveClient = GoogleDriveAuthHelper.initializeDriveClient(driveAccessToken)
            try {
                Log.d(TAG, "Doing backup in service...")
                BackupRepository.doBackup(driveClient)
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
