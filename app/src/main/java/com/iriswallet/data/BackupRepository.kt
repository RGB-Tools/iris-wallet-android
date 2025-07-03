package com.iriswallet.data

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.iriswallet.R
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppException
import com.iriswallet.utils.TAG
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import org.rgbtools.RgbLibException
import org.rgbtools.restoreKeys

object BackupRepository {

    private const val ZIP_MIME_TYPE = "application/zip"

    private lateinit var driveClient: Drive

    private fun initializeDriveClient(signInAccount: GoogleSignInAccount) {
        val credential =
            GoogleAccountCredential.usingOAuth2(
                AppContainer.appContext,
                listOf(DriveScopes.DRIVE_FILE),
            )
        credential.selectedAccount = signInAccount.account!!
        driveClient =
            Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName(AppContainer.appContext.getString(R.string.app_name_mainnet))
                .build()
    }

    private fun getBackupFile(mnemonic: String): File {
        val keys = restoreKeys(AppContainer.bitcoinNetwork.toRgbLibNetwork(), mnemonic)
        return File(
            AppContainer.appContext.filesDir,
            AppConstants.backupName.format(keys.masterFingerprint),
        )
    }

    fun doBackup(gAccount: GoogleSignInAccount): Boolean {
        Log.d(TAG, "Starting backup...")
        initializeDriveClient(gAccount)

        val mnemonic = SharedPreferencesManager.mnemonic

        val backupFile = getBackupFile(mnemonic)
        backupFile.delete()

        RgbRepository.backupDo(backupFile, mnemonic)
        Log.d(TAG, "Backup done")

        val oldBackups = driveClient.files().list().setQ("name='${backupFile.name}'").execute()
        val gFile = com.google.api.services.drive.model.File()
        gFile.name = backupFile.name
        val newBackupID =
            driveClient.files().create(gFile, FileContent(ZIP_MIME_TYPE, backupFile)).execute().id
        Log.d(TAG, "Backup uploaded. Backup file ID: $newBackupID")
        if (oldBackups != null) {
            for (file in oldBackups.files) {
                Log.d(TAG, "Deleting old backup file ${file.id} ...")
                driveClient.files().delete(file.id).execute()
            }
        }
        Log.d(TAG, "Backup operation completed")
        return true
    }

    fun restoreBackup(gAccount: GoogleSignInAccount, mnemonic: String): Boolean {
        Log.d(TAG, "Downloading most recent backup...")
        initializeDriveClient(gAccount)
        val backupFile = getBackupFile(mnemonic)
        val lastBackups =
            driveClient
                .files()
                .list()
                .setQ("name='${backupFile.name}'")
                .setOrderBy("createdTime")
                .execute()
        if (lastBackups.files.isNullOrEmpty())
            throw AppException(AppContainer.appContext.getString(R.string.err_no_backup_found))
        val lastBackup = lastBackups.files.last()
        val outputStream: OutputStream = FileOutputStream(backupFile)
        driveClient.files().get(lastBackup.id).executeMediaAndDownloadTo(outputStream)

        Log.d(TAG, "Restoring wallet from backup file...")
        try {
            RgbRepository.backupRestore(backupFile, mnemonic, AppContainer.rgbDir)
        } catch (e: RgbLibException.WrongPassword) {
            throw AppException(AppContainer.appContext.getString(R.string.invalid_mnemonic))
        }

        AppContainer.storedMnemonic = mnemonic
        Log.d(TAG, "Restoring preferences...")
        SharedPreferencesManager.mnemonic = mnemonic
        SharedPreferencesManager.backupConfigured = true
        Log.d(TAG, "Restore completed successfully!")
        return true
    }
}
