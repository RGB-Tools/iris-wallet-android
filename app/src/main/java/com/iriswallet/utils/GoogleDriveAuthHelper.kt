package com.iriswallet.utils

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.iriswallet.R
import kotlinx.coroutines.launch

interface GoogleDriveAuthListener {
    fun onGoogleSignInSuccess(email: String)

    fun onDriveAccessTokenReceived(accessToken: String)

    fun onGoogleSignInError(errorExtraInfo: String?)

    fun onDriveAuthorizationError(errorExtraInfo: String?)
}

class GoogleDriveAuthHelper(
    private val fragment: Fragment,
    private val authorizeLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val listener: GoogleDriveAuthListener,
) {
    private val googleSignInHelper: GoogleSignInHelper =
        GoogleSignInHelper(fragment.requireContext())
    private var authorizationClient: AuthorizationClient =
        Identity.getAuthorizationClient(fragment.requireActivity())
    private var pendingEmailForDriveAuth: String? = null

    fun initiateSignInAndRequestDriveAccess() {
        fragment.lifecycleScope.launch {
            try {
                Log.d(TAG, "Initiating Google Sign-In...")
                val googleIdTokenCredential = googleSignInHelper.signIn()
                if (googleIdTokenCredential == null) {
                    listener.onGoogleSignInError(
                        fragment.getString(R.string.err_no_credential_returned)
                    )
                    return@launch
                }
                Log.d(TAG, "Google Sign-In successful. ID: ${googleIdTokenCredential.id}")

                val verificationResult =
                    googleSignInHelper.verifyIdTokenAndGetEmail(googleIdTokenCredential.idToken)
                if (verificationResult.isSuccess) {
                    val account =
                        if (verificationResult.email.isNullOrBlank()) {
                            googleIdTokenCredential.id
                        } else {
                            verificationResult.email!!
                        }
                    Log.d(TAG, "ID Token verified successfully. Account: $account")
                    pendingEmailForDriveAuth = account
                    listener.onGoogleSignInSuccess(account)
                    requestDriveAccessTokenInternal()
                } else {
                    Log.w(TAG, "ID Token verification failed: ${verificationResult.errorMessage}")
                    listener.onGoogleSignInError(
                        fragment.getString(
                            R.string.err_failed_to_verify_google_account,
                            verificationResult.errorMessage,
                        )
                    )
                }
            } catch (e: Exception) {
                handleSignInException(e)
            }
        }
    }

    private fun handleSignInException(e: Exception) {
        Log.w(TAG, "Google Sign-In failed", e)
        val errorExtraInfo =
            when (e) {
                is GetCredentialCancellationException -> fragment.getString(R.string.user_cancelled)
                is NoCredentialException -> fragment.getString(R.string.no_google_accounts)
                is ApiException -> fragment.getString(R.string.api_error, e.statusCode.toString())
                else -> fragment.getString(R.string.unknown_error, e.message)
            }
        listener.onGoogleSignInError(errorExtraInfo)
    }

    fun handleAuthorizationResult(activityResultCode: Int) {
        if (activityResultCode == Activity.RESULT_OK) {
            Log.d(TAG, "User granted Drive access via launcher, retrying token request")
            requestDriveAccessTokenInternal()
        } else {
            Log.e(TAG, "User denied Drive access via launcher")
            listener.onDriveAuthorizationError(
                fragment.getString(R.string.user_denied_drive_access)
            )
        }
    }

    private fun requestDriveAccessTokenInternal() {
        val authRequest = createDriveAuthorizationRequest()
        authorizationClient
            .authorize(authRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent!!).build()
                    authorizeLauncher.launch(intentSenderRequest)
                } else {
                    val accessToken = authorizationResult.accessToken
                    if (accessToken != null) {
                        Log.d(TAG, "Got Drive access token")
                        listener.onDriveAccessTokenReceived(accessToken)
                    } else {
                        Log.e(TAG, "Drive access token is null")
                        listener.onDriveAuthorizationError("Access token was null")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to authorize Drive", e)
                listener.onDriveAuthorizationError(e.message)
            }
    }

    companion object {
        fun createDriveAuthorizationRequest(): AuthorizationRequest {
            return AuthorizationRequest.Builder()
                .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_FILE)))
                .build()
        }

        fun initializeDriveClient(
            driveAccessToken: String,
            httpTransport: HttpTransport = NetHttpTransport(),
            jsonFactory: JsonFactory = GsonFactory.getDefaultInstance(),
        ): Drive {
            val requestInitializer = HttpRequestInitializer { httpRequest ->
                httpRequest.headers.authorization = "Bearer $driveAccessToken"
            }
            return Drive.Builder(httpTransport, jsonFactory, requestInitializer)
                .setApplicationName(AppContainer.appContext.getString(R.string.app_name_mainnet))
                .build()
        }
    }
}
