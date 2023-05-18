package com.iriswallet.utils

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

interface GoogleSignInServiceListener {
    fun loggedIn(gAccount: GoogleSignInAccount)

    fun handleLoginError(errorExtraInfo: String? = null)
}

class GoogleSignInService(private val fragment: Fragment) {

    private var googleSignInServiceListener: GoogleSignInServiceListener =
        fragment as GoogleSignInServiceListener
    private var signInLauncher: ActivityResultLauncher<Intent> =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
            var errMsg: String? = null
            if (result.resultCode == Activity.RESULT_OK) {
                val getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                if (getAccountTask.isSuccessful) {
                    googleSignInServiceListener.loggedIn(getAccountTask.result)
                    return@registerForActivityResult
                } else {
                    errMsg = getAccountTask.exception?.message
                }
            }
            googleSignInServiceListener.handleLoginError(errMsg)
        }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(AppContainer.backupServerClientID)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        GoogleSignIn.getClient(fragment.requireActivity(), signInOptions)
    }

    fun signInGoogle() {
        val gAccount =
            GoogleSignIn.getLastSignedInAccount(fragment.requireActivity().applicationContext)
        if (gAccount == null) signInLauncher.launch(googleSignInClient.signInIntent)
        else googleSignInServiceListener.loggedIn(gAccount)
    }
}
