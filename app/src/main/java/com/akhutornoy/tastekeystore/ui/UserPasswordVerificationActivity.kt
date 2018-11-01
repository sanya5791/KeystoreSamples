package com.akhutornoy.tastekeystore.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.Injections
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.data.Repository
import com.akhutornoy.tastekeystore.security.DataSignatureVerifier
import com.akhutornoy.tastekeystore.utils.ui.hideKeyboard
import kotlinx.android.synthetic.main.activity_user_password_verification_activity.*
import kotlinx.android.synthetic.main.content_user_password_verification_acivity.*

class UserPasswordVerificationActivity : AppCompatActivity() {

    private val repository: Repository by lazy { Injections.provideRepository(this) }
    private val passwordVerifier: DataSignatureVerifier by lazy { Injections.providePasswordVerifier(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_password_verification_activity)
        setupActionBar()

        create_keys_button.setOnClickListener { onCreateKeysClicked() }
        sign_password_button.setOnClickListener (this::onSignPasswordClicked)
        verify_password_button.setOnClickListener (this::onVerifyClicked)
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
        info_output_text_view.setText(R.string.keys_generated)
    }

    private fun onSignPasswordClicked(v: View) {
        v.hideKeyboard()

        val password = password_edit_text.text.toString()
        val signatureStr = passwordVerifier.signData(password)
        repository.signature = signatureStr

        info_output_text_view.setText(R.string.password_signed)
    }

    private fun onVerifyClicked(v: View) {
        v.hideKeyboard()

        val verifyPasswString = verify_password_edit_text.text.toString()
        val signatureStr = repository.signature
        val result = passwordVerifier.verifyData(verifyPasswString, signatureStr)

        val text = "Passwords are same = $result"
        info_output_text_view.text = text
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, UserPasswordVerificationActivity::class.java)
        }
    }
}
