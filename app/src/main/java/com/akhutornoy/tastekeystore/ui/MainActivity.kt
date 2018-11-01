package com.akhutornoy.tastekeystore.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.ui.EncryptActivity.Companion.EncryptionMode.AES
import com.akhutornoy.tastekeystore.ui.EncryptActivity.Companion.EncryptionMode.RSA
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        verify_passwords_button.setOnClickListener { onVerifyPasswordsClicked() }
        encrypt_rsa_button.setOnClickListener { onEncryptWithRsaClicked() }
        encrypt_aes_button.setOnClickListener { onEncryptWithAesClicked() }
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
}
