package com.akhutornoy.tastekeystore.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.Injections
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.data.Repository
import com.akhutornoy.tastekeystore.security.DataSignatureVerifier
import com.akhutornoy.tastekeystore.ui.extensions.hideKeyboard
import com.github.ajalt.timberkt.Timber
import kotlinx.android.synthetic.main.activity_user_password_verification_activity.*
import kotlinx.android.synthetic.main.content_user_password_verification_acivity.*
import java.lang.Exception
import java.security.KeyStoreException

class UserPasswordVerificationActivity : AppCompatActivity() {

    private val repository: Repository by lazy { Injections.provideRepository(this) }
    private val passwordVerifier: DataSignatureVerifier by lazy { Injections.providePasswordVerifier(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_password_verification_activity)
        setupActionBar()

        create_keys_button.setOnClickListener { onCreateKeysClicked() }
        sign_password_button.setOnClickListener(this::onSignPasswordClicked)
        verify_password_button.setOnClickListener(this::onVerifyClicked)
        check_keys_button.setOnClickListener { onCheckKeysClicked() }
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun onCreateKeysClicked() {
        passwordVerifier.createKeys()
        showMessage(R.string.keys_generated)
    }

    private fun onSignPasswordClicked(v: View) {
        v.hideKeyboard()

        val password = password_edit_text.text.toString()
        try {
            val signatureStr = passwordVerifier.signData(password)
            repository.signature = signatureStr
            showMessage(R.string.password_signed)
        } catch (e: KeyStoreException) {
            Timber.e(e) { "onSignPasswordClicked(): " }
            showMessage(getString(R.string.cant_get_private_key))
        } catch (e: Exception) {
            Timber.e(e) { "onSignPasswordClicked(): " }
        }
    }

    private fun onVerifyClicked(v: View) {
        v.hideKeyboard()

        val verifyPasswString = verify_password_edit_text.text.toString()
        val signatureStr = repository.signature
        val result = passwordVerifier.verifyData(verifyPasswString, signatureStr)

        val text = "Passwords are same = $result"
        showMessage(text)
    }

    private fun onCheckKeysClicked() {
        @StringRes val message =
            if (passwordVerifier.isKeysCreated()) R.string.keys_exist
            else R.string.keys_not_exist
        showMessage(message)
    }

    private fun showMessage(@StringRes stringRes: Int) = info_output_chip.setText(stringRes)

    private fun showMessage(message: String) {
        info_output_chip.text = message
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, UserPasswordVerificationActivity::class.java)
        }
    }
}
