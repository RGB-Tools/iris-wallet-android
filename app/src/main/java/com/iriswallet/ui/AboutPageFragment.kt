package com.iriswallet.ui

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.iriswallet.BuildConfig
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAboutPageBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import java.util.regex.Pattern

class AboutPageFragment :
    MainBaseFragment<FragmentAboutPageBinding>(FragmentAboutPageBinding::inflate) {

    private var requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) showDownloadedNotification()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appVersion =
            getString(R.string.app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        binding.aboutVersionTV.text = appVersion

        Linkify.addLinks(
            binding.aboutPrivacyPolicyTV,
            Pattern.compile(getString(R.string.privacy_policy_link)),
            null,
            null,
        ) { _, _ ->
            AppConstants.PRIVACY_POLICY_URL
        }

        Linkify.addLinks(
            binding.aboutTermsOfServiceTV,
            Pattern.compile(getString(R.string.terms_of_service_link)),
            null,
            null,
        ) { _, _ ->
            AppContainer.termsAndConditionsURL
        }

        binding.aboutDownloadLogsBtn.setOnClickListener {
            val zipFileName =
                AppConstants.LOGS_ZIP_FILE_NAME.format(
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                    System.currentTimeMillis(),
                )
            val zipFile =
                AppUtils.zipFiles(
                    AppContainer.appContext,
                    listOf(
                        Pair(AppContainer.rgbLogsFile, AppConstants.RGB_DOWNLOAD_LOGS_FILE_NAME),
                        Pair(AppContainer.appLogsFile, AppContainer.appLogsFile.name),
                    ),
                    zipFileName,
                )
            AppUtils.saveFileToDownloads(
                requireContext(),
                zipFile.toURI().toString(),
                zipFileName,
                "application/zip",
            )
            showDownloadedNotification()
            Toast.makeText(activity, getString(R.string.downloaded_logs), Toast.LENGTH_LONG).show()
            zipFile.delete()
            binding.aboutDownloadLogsBtn.isEnabled = false
        }
    }

    private fun showDownloadedNotification() {
        val intent = Intent()
        intent.action = DownloadManager.ACTION_VIEW_DOWNLOADS
        AppUtils.createNotification(
            requireContext(),
            AppConstants.DOWNLOAD_LOGS_NOTIFICATION_CHANNEL,
            AppConstants.DOWNLOAD_LOGS_NOTIFICATION_ID,
            R.string.download_logs_notification_channel_name,
            R.string.download_logs_notification_channel_description,
            R.string.download_logs_notification_title,
            R.string.download_logs_notification_text,
            intent,
            requestPermissionLauncher = requestPermissionLauncher,
        )
    }
}
