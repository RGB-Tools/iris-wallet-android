package com.iriswallet.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSignInHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    fun createGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setServerClientId(AppContainer.backupServerClientID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(true)
            .setNonce(UUID.randomUUID().toString())
            .build()
    }

    suspend fun signIn(): GoogleIdTokenCredential? {
        val googleIdOption = createGoogleIdOption()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
        Log.d(TAG, "Requesting Google ID credential...")
        val result = credentialManager.getCredential(context, request)
        return handleSignInResult(result)
    }

    private fun handleSignInResult(result: GetCredentialResponse): GoogleIdTokenCredential? {
        val credential = result.credential
        Log.d(TAG, "Handling Google ID credential...")
        if (
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                Log.d(TAG, "Credential received, attempting to parse...")
                return GoogleIdTokenCredential.createFrom(credential.data)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Failed to parse GoogleIdTokenCredential", e)
                return null
            }
        }
        Log.w(
            TAG,
            "Resulting credential was not of type GoogleIdTokenCredential: ${credential.type}",
        )
        return null
    }

    data class TokenVerificationResult(
        val isSuccess: Boolean,
        val email: String?,
        val errorMessage: String? = null,
    )

    suspend fun verifyIdTokenAndGetEmail(idTokenString: String): TokenVerificationResult {
        return withContext(Dispatchers.IO) {
            val verifier =
                GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(listOf(AppContainer.backupServerClientID))
                    .build()
            Log.d(TAG, "Verifying ID token: ${idTokenString.take(30)}...")
            try {
                val verifiedIdToken: GoogleIdToken? = verifier.verify(idTokenString)
                if (verifiedIdToken != null) {
                    TokenVerificationResult(true, verifiedIdToken.payload.email)
                } else {
                    TokenVerificationResult(
                        false,
                        null,
                        "Token verifier returned null (token invalid, expired, or audience mismatch)",
                    )
                }
            } catch (e: Exception) {
                val errMessage =
                    when (e) {
                        is GeneralSecurityException -> "Security error: ${e.message}"
                        is IOException -> "Network error: ${e.message}"
                        is IllegalArgumentException -> "Invalid argument: ${e.message}"
                        else -> "Unexpected error: ${e.message}"
                    }
                TokenVerificationResult(false, null, errMessage)
            }
        }
    }
}
