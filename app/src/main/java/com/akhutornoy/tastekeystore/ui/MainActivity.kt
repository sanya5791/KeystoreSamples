package com.akhutornoy.tastekeystore.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.ui.EncryptActivity.Companion.EncryptionMode.AES
import com.akhutornoy.tastekeystore.ui.EncryptActivity.Companion.EncryptionMode.RSA
import com.github.ajalt.timberkt.Timber
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    private val systemPasswordAsker: SystemPasswordAsker by lazy { SystemPasswordAsker(findViewById(android.R.id.content)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        verify_passwords_button.setOnClickListener { onVerifyPasswordsClicked() }
        encrypt_rsa_button.setOnClickListener { onEncryptWithRsaClicked() }
        encrypt_aes_button.setOnClickListener { onEncryptWithAesClicked() }
        ask_password_button.setOnClickListener { onAskSystemPasswordClicked() }
    }

    private fun onVerifyPasswordsClicked() {
        startActivity(UserPasswordVerificationActivity.getIntent(this))
    }

    private fun onEncryptWithRsaClicked() {
        startActivity(EncryptActivity.newIntent(this, RSA))
    }

    private fun onEncryptWithAesClicked() {
        startActivity(EncryptActivity.newIntent(this, AES))
    }

    private fun onAskSystemPasswordClicked() {
        systemPasswordAsker.askPassword(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (systemPasswordAsker.onActivityResult(requestCode, resultCode, data)) {
            return
        }
        Timber.d { "onActivityResult(): NOT PASSWORD requestCode=$requestCode" }
    }
}
