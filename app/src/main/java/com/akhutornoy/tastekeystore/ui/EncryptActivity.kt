package com.akhutornoy.tastekeystore.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.akhutornoy.tastekeystore.Injections
import com.akhutornoy.tastekeystore.R
import com.akhutornoy.tastekeystore.security.Encrypter
import com.akhutornoy.tastekeystore.utils.ui.hideKeyboard
import com.github.ajalt.timberkt.Timber
import kotlinx.android.synthetic.main.activity_encrypt.*
import kotlinx.android.synthetic.main.content_encrypt.*

class EncryptActivity : AppCompatActivity() {

    private lateinit var encrypter: Encrypter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encrypt)

        setupActionBar()

        when (getModeArg()) {
            EncryptionMode.RSA -> initRsaMode()
            EncryptionMode.AES -> initAesMode()
        }

        encrypt_data_button.setOnClickListener (this::onEncryptRsaClicked)
        decrypt_data_button.setOnClickListener (this::onDecryptRsaClicked)
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

    private fun initRsaMode() {
        encrypter = Injections.provideRsaEncrypter(this)

        supportActionBar?.setTitle(R.string.rsa_encryption)
        title_text_view.setText(R.string.rsa_encryption_decryption)
    }

    private fun initAesMode() {
        encrypter = Injections.provideAesEncrypter(this)

        supportActionBar?.setTitle(R.string.aes_encryption)
        title_text_view.setText(R.string.aes_encryption_decryption)
    }

    private fun onEncryptRsaClicked(v: View) {
        v.hideKeyboard()

        val plainText = input_edit_text.text.toString()
        if (plainText.isEmpty()) {
            Toast.makeText(this, "Nothing to encrypt", Toast.LENGTH_SHORT).show()
            return
        }
        Timber.d { "onEncryptRsaClicked(): plainText=$plainText" }

        if (!encrypter.isKeyCreated()) {
            Timber.d { "onEncryptRsaClicked(): keys NOT exist" }
            encrypter.createKey()
        }

        val encryptedData = encrypter.encrypt(plainText.toByteArray(Charsets.UTF_8))
        val encryptedRsaText = Base64.encodeToString(encryptedData, Base64.DEFAULT)
        Timber.d { "onEncryptRsaClicked(): encryptedRsaText=$encryptedRsaText" }

        output_edit_text.setText(encryptedRsaText)
    }

    private fun onDecryptRsaClicked(v: View) {
        v.hideKeyboard()

        val encryptedRsaText = input_edit_text.text.toString()
        if (encryptedRsaText.isEmpty()) {
            Timber.d { "onDecryptRsaClicked(): nothing to decrypt" }
            return
        }

        Timber.d { "onDecryptRsaClicked(): encryptedText=$encryptedRsaText" }
        val decodedEncrypted = Base64.decode(encryptedRsaText, Base64.DEFAULT)
        val decryptedData = encrypter.decrypt(decodedEncrypted)
        val plainText = String(decryptedData)
        Timber.d { "onDecryptRsaClicked(): plainText=$plainText" }

        output_edit_text.setText(plainText)
    }

    private fun getModeArg(): EncryptionMode {
        val encrModeString = intent.extras?.getString(ENCRYPTION_MODE_ARG)
        return if (encrModeString == null) {
            EncryptionMode.RSA
        } else {
            EncryptionMode.valueOf(encrModeString)
        }
    }

    companion object {
        private const val ENCRYPTION_MODE_ARG = "ENCRYPTION_MODE_ARG"
        enum class EncryptionMode {RSA, AES}

        fun newIntent(context: Context, mode: EncryptionMode): Intent {
            val intent = Intent(context, EncryptActivity::class.java)
            val args = Bundle()
            args.putString(ENCRYPTION_MODE_ARG, mode.name)
            intent.putExtras(args)
            return intent
        }
    }
}
