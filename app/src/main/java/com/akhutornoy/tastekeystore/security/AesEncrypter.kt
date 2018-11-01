package com.akhutornoy.tastekeystore.security

import android.security.keystore.KeyProperties
import com.github.ajalt.timberkt.Timber
import java.io.UnsupportedEncodingException
import java.lang.IllegalArgumentException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec

private const val AES_CIPHER_COMPAT = KeyProperties.KEY_ALGORITHM_AES + "/" +
        KeyProperties.BLOCK_MODE_CBC + "/" +
        KeyProperties.ENCRYPTION_PADDING_PKCS7

class AesEncrypter(
    private val keystore: AesKeystore
): Encrypter {

    private var aesKey: SecretKey?
        get() = keystore.getKey()
        set(value) = keystore.putKey(value)

    private var iv: ByteArray?
        get() = keystore.getIv()
        set(value) = keystore.putIv(value)


    override fun createKey() {
        aesKey = generateAesKey()
    }

    override fun isKeyCreated(): Boolean {
        return ((aesKey != null) and (iv != null))
    }

    override fun encrypt(data: ByteArray): ByteArray {
        if (aesKey == null) {
            throw IllegalArgumentException("AES key should be set first")
        }

        return encryptAesCompat(data, aesKey!!)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        if (aesKey == null) {
            throw IllegalArgumentException("AES key should be set first")
        }
        if (iv == null) {
            throw IllegalArgumentException("IV should be set first")
        }

        return decryptAesCompat(data, aesKey!!, iv!!)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun generateAesKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")

        keyGen.init(128)
        val sKey = keyGen.generateKey()
        Timber.d { "generateAesKey(): Generated" }
        return sKey
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        UnsupportedEncodingException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun encryptAesCompat(bytes: ByteArray, aesKey: SecretKey): ByteArray {
        val c = Cipher.getInstance(AES_CIPHER_COMPAT)
        c.init(Cipher.ENCRYPT_MODE, aesKey)
        val encryptedData = c.doFinal(bytes)
        iv = c.iv
        return encryptedData
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        UnsupportedEncodingException::class,
        InvalidAlgorithmParameterException::class
    )

    private fun decryptAesCompat(bytes: ByteArray, aesKey: SecretKey, IV: ByteArray): ByteArray {
        val c = Cipher.getInstance(AES_CIPHER_COMPAT)
        c.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(IV))
        return c.doFinal(bytes)
    }
}