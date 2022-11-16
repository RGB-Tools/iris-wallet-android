package com.iriswallet.ui

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.util.Linkify
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iriswallet.BuildConfig
import com.iriswallet.R
import com.iriswallet.databinding.FragmentAboutPageBinding
import com.iriswallet.utils.AppConstants
import com.iriswallet.utils.AppContainer
import com.iriswallet.utils.AppUtils
import java.util.regex.Pattern

class AboutPageFragment :
    MainBaseFragment<FragmentAboutPageBinding>(FragmentAboutPageBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appVersion = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        binding.aboutVersionTV.text = appVersion

        Linkify.addLinks(
            binding.aboutPrivacyPolicyTV,
            Pattern.compile(getString(R.string.privacy_policy_link)),
            null,
            null
        ) { _, _ -> AppConstants.privacyPolicyURL }

        Linkify.addLinks(
            binding.aboutTermsOfServiceTV,
            Pattern.compile(getString(R.string.terms_of_service_link)),
            null,
            null
        ) { _, _ -> AppContainer.termsAndConditionsURL }

        binding.aboutDownloadLogsBtn.setOnClickListener {
            val fileName =
                AppConstants.rgbDownloadLogsFileName.format(
                    System.currentTimeMillis(),
                    BuildConfig.VERSION_NAME
                )
            AppUtils.saveFileToDownloads(
                requireContext(),
                AppContainer.rgbLogsFile.toURI().toString(),
                fileName
            )
            val channel =
                NotificationChannel(
                        AppConstants.DOWNLOADS_NOTIFICATION_CHANNEL,
                        requireContext()
                            .getString(R.string.download_logs_notification_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    .apply {
                        description =
                            requireContext()
                                .getString(R.string.download_logs_notification_channel_description)
                    }
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
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
                NotificationCompat.Builder(
                        requireContext(),
                        AppConstants.DOWNLOADS_NOTIFICATION_CHANNEL
                    )
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(
                        requireContext().getString(R.string.download_logs_notification_title)
                    )
                    .setContentText(
                        requireContext().getString(R.string.download_logs_notification_text)
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

            with(NotificationManagerCompat.from(requireContext())) {
                notify(AppConstants.DOWNLOADS_NOTIFICATION_ID, builder.build())
            }

            Toast.makeText(activity, getString(R.string.downloaded_logs), Toast.LENGTH_LONG).show()
            binding.aboutDownloadLogsBtn.isEnabled = false
        }
    }
}
