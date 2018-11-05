package com.akhutornoy.tastekeystore.ui

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.utils.ui.showSnack
import com.github.ajalt.timberkt.Timber
import com.google.android.material.snackbar.Snackbar

private const val INTENT_SYSTEM_AUTHENTICATION: Int = 12
class SystemPasswordAsker(private val rootView: View) {
    fun askPassword(activity: AppCompatActivity) {
        val km = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (km.isKeyguardSecure) {
            val authIntent = km.createConfirmDeviceCredentialIntent(
                activity.getString(R.string.ask_password_title_auth),
                activity.getString(R.string.ask_password_msg_auth)
            )
            activity.startActivityForResult(authIntent, INTENT_SYSTEM_AUTHENTICATION)
        } else {
            Snackbar.make(
                rootView,
                R.string.set_system_password,
                Snackbar.LENGTH_LONG
            ).setAction(
                R.string.set
            ) { onSetSystemPasswordClicked(activity) }
                .show()
        }
    }

    private fun onSetSystemPasswordClicked(activity: AppCompatActivity) {
        activity.startActivityForResult(Intent(Settings.ACTION_SECURITY_SETTINGS), 0)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return if (requestCode == INTENT_SYSTEM_AUTHENTICATION) {
            @StringRes val msgResId = if (resultCode == Activity.RESULT_OK) {
                Timber.d { "onActivityResult(): RESULT_OK" }
                R.string.password_success_msg
            } else {
                Timber.d { "onActivityResult(): error=$resultCode" }
                R.string.password_fail_msg
            }
            showSnack(rootView, msgResId)
            true
        } else {
            false
        }
    }
}