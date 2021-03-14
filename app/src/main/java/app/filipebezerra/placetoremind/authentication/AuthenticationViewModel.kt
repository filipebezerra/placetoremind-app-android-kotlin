package app.filipebezerra.placetoremind.authentication

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import app.filipebezerra.placetoremind.BuildConfig
import app.filipebezerra.placetoremind.R
import app.filipebezerra.placetoremind.base.BaseViewModel
import app.filipebezerra.placetoremind.base.NavigationCommand
import app.filipebezerra.placetoremind.utils.SIGN_IN_REQUEST_CODE
import com.firebase.ui.auth.AuthUI
import timber.log.Timber


class AuthenticationViewModel(app: Application) : BaseViewModel(app) {
    val userAuthenticated: LiveData<Boolean> = FirebaseUserLiveData().map { it != null }

    fun onLoginButtonClicked() {
        navigationCommand.value = NavigationCommand.ForResult(
            buildSignInIntent(),
            SIGN_IN_REQUEST_CODE
        )
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            SIGN_IN_REQUEST_CODE -> handleSignInResponse(resultCode, data)
        }
    }

    private fun handleSignInResponse(resultCode: Int, data: Intent?) {
        resultCode.takeIf { it == Activity.RESULT_CANCELED }?.let {
            navigationCommand.value = NavigationCommand.Back
            return
        }

        Timber.w("Unknown result after trying to sign in")
    }

    private fun buildSignInIntent() = AuthUI.getInstance().createSignInIntentBuilder()
        .setTheme(R.style.AppTheme)
        .setLogo(R.mipmap.ic_launcher_round)
        .setTosAndPrivacyPolicyUrls(
            "https://firebase.google.com/terms/analytics",
            "https://firebase.google.com/policies/analytics"
        )
        .setLockOrientation(true)
        .setAvailableProviders(buildAvaiableProviders())
        .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
        .build()

    private fun buildAvaiableProviders(): List<AuthUI.IdpConfig> = listOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build()
    )
}