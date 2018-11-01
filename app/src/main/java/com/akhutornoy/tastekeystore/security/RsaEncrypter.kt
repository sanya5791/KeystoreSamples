package com.akhutornoy.tastekeystore.security

import android.annotation.SuppressLint
import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyProperties
import com.github.ajalt.timberkt.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.interfaces.RSAPublicKey
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.NoSuchPaddingException
import javax.security.auth.x500.X500Principal

private const val ALIAS_ENCR_DECR_STRING = "ALIAS_ENCR_DECR_STRING"

@SuppressLint("InlinedApi")
private const val RSA_CIPHER = KeyProperties.KEY_ALGORITHM_RSA + "/" +
        KeyProperties.BLOCK_MODE_ECB + "/" +
        KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
class RsaEncrypter(private val context: Context): Encrypter {

    override fun createKey() = createRSAKeys()

    override fun isKeyCreated() = isRsaKeyCreated()

    override fun encrypt(data: ByteArray) = encryptRsa(data)

    override fun decrypt(data: ByteArray) = decryptRsa(data)

    private fun getKeyStore(): KeyStore {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)
        return ks
    }

    @Throws(NoSuchProviderException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
    private fun createRSAKeys() {
        Timber.d { "createRSAKeys(): " }
        val ks = getKeyStore()
        if (!ks.containsAlias(ALIAS_ENCR_DECR_STRING)) {
            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR, 25)

            val keyGen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE_PROVIDER
            )

            val spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(ALIAS_ENCR_DECR_STRING)
                .setKeySize(512)
                .setKeyType(KeyProperties.KEY_ALGORITHM_RSA)
                .setEndDate(end.time)
                .setStartDate(start.time)
                .setSerialNumber(BigInteger.ONE)
                .setSubject(X500Principal("CN = Secured Preference Store, O = Devliving Online"))
                .build()

            keyGen.initialize(spec)
            keyGen.generateKeyPair()
        }
    }

    private fun isRsaKeyCreated()
            = getKeyStore().getEntry(ALIAS_ENCR_DECR_STRING, null) != null

    @Throws(KeyStoreException::class, UnrecoverableEntryException::class, NoSuchAlgorithmException::class)
    private fun loadRSAKeys(): Pair<PrivateKey, RSAPublicKey> {
        Timber.d { "loadRSAKeys(): " }
        val ks = getKeyStore()
        if (ks.containsAlias(ALIAS_ENCR_DECR_STRING) && ks.entryInstanceOf(
                ALIAS_ENCR_DECR_STRING,
                KeyStore.PrivateKeyEntry::class.java
            )
        ) {
            val entry = ks.getEntry(ALIAS_ENCR_DECR_STRING, null) as KeyStore.PrivateKeyEntry
            val privateKey = entry.privateKey
            val publicKey = entry.certificate.publicKey as RSAPublicKey

            return Pair(privateKey, publicKey)
        }
        throw IllegalArgumentException("Can't find keys by alias '$ALIAS_ENCR_DECR_STRING'")
    }

    @Throws(
        KeyStoreException::class,
        UnrecoverableEntryException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        NoSuchPaddingException::class,
        InvalidKeyException::class,
        IOException::class
    )
    private fun encryptRsa(bytes: ByteArray): ByteArray {
        val (privateKey, rsaPublicKey) = loadRSAKeys()
        val cipher = Cipher.getInstance(RSA_CIPHER)
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey)

        val outputStream = ByteArrayOutputStream()
        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        cipherOutputStream.write(bytes)
        cipherOutputStream.close()

        return outputStream.toByteArray()
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeyException::class,
        IOException::class
    )
    private fun decryptRsa(bytes: ByteArray): ByteArray {
        val (privateKey, rsaPublicKey) = loadRSAKeys()
        val cipher = Cipher.getInstance(RSA_CIPHER)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)

        return cipher.doFinal(bytes)
    }

}