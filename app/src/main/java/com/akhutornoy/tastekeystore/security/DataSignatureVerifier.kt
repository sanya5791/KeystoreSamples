package com.akhutornoy.tastekeystore.security

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import com.github.ajalt.timberkt.Timber
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal

private const val TYPE_RSA = "RSA"
private const val ALIAS_VERIFY_SIGNATURE = "ALIAS_VERIFY_SIGNATURE"
private const val SIGNATURE_SHA256withRSA = "SHA256withRSA"

class DataSignatureVerifier(private val context: Context) {
    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    @Throws(NoSuchProviderException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
    fun createKeys() {
        Timber.d { "createKeys(): " }
        // Create a start and end time, for the validity range of the key pair that's about to be
        // generated.
        val start = GregorianCalendar()
        val end = GregorianCalendar()
        end.add(Calendar.YEAR, 1)

        // The KeyPairGeneratorSpec object is how parameters for your key pair are passed
        // to the KeyPairGenerator.
        val spec: AlgorithmParameterSpec =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                getRsaKeySpecPreM(start.time, end.time)
            else
                getRsaKeySpecM(start.time, end.time)

        generateKeyPair(spec)
    }

    private fun generateKeyPair(spec: AlgorithmParameterSpec) {
        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
        // and the KeyStore.  This example uses the AndroidKeyStore.
        val kpGenerator = KeyPairGenerator
            .getInstance(
                TYPE_RSA,
                ANDROID_KEYSTORE_PROVIDER
            )

        kpGenerator.initialize(spec)
        kpGenerator.generateKeyPair()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRsaKeySpecM(start: Date, end: Date)=
        KeyGenParameterSpec.Builder(ALIAS_VERIFY_SIGNATURE, KeyProperties.PURPOSE_SIGN)
            .setCertificateSubject(X500Principal("CN=$ALIAS_VERIFY_SIGNATURE"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSerialNumber(BigInteger.valueOf(1337))
            .setCertificateNotBefore(start)
            .setCertificateNotAfter(end)
            .build()

    @Suppress("DEPRECATION")
    fun getRsaKeySpecPreM(start: Date, end: Date) =
        KeyPairGeneratorSpec.Builder(context!!)
            // You'll use the alias later to retrieve the key.  It's a key for the key!
            .setAlias(ALIAS_VERIFY_SIGNATURE)
            // The subject used for the self-signed certificate of the generated pair
            .setSubject(X500Principal("CN=$ALIAS_VERIFY_SIGNATURE"))
            // The serial number used for the self-signed certificate of the
            // generated pair.
            .setSerialNumber(BigInteger.valueOf(1337))
            // Date range of validity for the generated pair.
            .setStartDate(start)
            .setEndDate(end)
            .build()

    /**
     * Signs the data using the key pair stored in the Android Key Store.  This signature can be
     * used with the data later to verify it was signed by this application.
     * @return Base64 encoded string of generated encrypted Signature data
     */
    @Throws(
        KeyStoreException::class,
        UnrecoverableEntryException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IOException::class,
        CertificateException::class
    )
    fun signData(inputStr: String): String {

        val privateKeyEntry =
            getPrivateKey() ?: throw KeyStoreException("Can't obtain 'Private Key'")

        // BEGIN_INCLUDE(sign_create_signature)
        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // Initialize Signature using specified private key
        s.initSign(privateKeyEntry.privateKey)

        // Sign the data, store the result as a Base64 encoded String.
        s.update(inputStr.toByteArray())
        val signatureBytes = s.sign()
        // END_INCLUDE(sign_data)

        val signatureString = Base64.encodeToString(signatureBytes, Base64.DEFAULT)
        Timber.d { "signData(): signature=$signatureString" }
        return signatureString
    }

    /**
     * Given some data and a signature, uses the key pair stored in the Android Key Store to verify
     * that the data was signed by this application, using that key pair.
     * @param input The data to be verified.
     * @param signatureStr The signature provided for the data.
     * @return A boolean value telling you whether the signature is valid or not.
     */
    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        IOException::class,
        UnrecoverableEntryException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    fun verifyData(input: String, signatureStr: String?): Boolean {
        val data = input.toByteArray()
        val signature: ByteArray

        // BEGIN_INCLUDE(decode_signature)
        // Make sure the signature string exists.  If not, bail out, nothing to do.
        if (signatureStr == null) {
            Timber.e { "verifyData(): Invalid signature." }
            Timber.e { "verifyData(): Exiting verifyData()..." }
            return false
        }

        try {
            // The signature is going to be examined as a byte array,
            // not as a base64 encoded string.
            signature = Base64.decode(signatureStr, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            // signatureStr wasn't null, but might not have been encoded properly.
            // It's not a valid Base64 string.
            Timber.e { "verifyData(): String '$signatureStr' cannot be encoded properly. It's not a valid Base64 string." }
            return false
        }
        // END_INCLUDE(decode_signature)

        val privateKeyEntry = getPrivateKey() ?: return false

        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // BEGIN_INCLUDE(verify_data)
        // Verify the data.
        s.initVerify(privateKeyEntry.certificate)
        s.update(data)
        return s.verify(signature)
        // END_INCLUDE(verify_data)
    }

    private fun getPrivateKey(): KeyStore.PrivateKeyEntry? {
        val ks = getKeyStore()

        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(ALIAS_VERIFY_SIGNATURE, null)

        /* If the entry is null, keys were never stored under this alias.
         * Debug steps in this situation would be:
         * -Check the list of aliases by iterating over Keystore.aliases(), be sure the alias
         *   exists.
         * -If that's empty, verify they were both stored and pulled from the same keystore
         *   "AndroidKeyStore"
         */
        if (entry == null) {
            Timber.e { "getPrivateKey(): No key found under alias: $ALIAS_VERIFY_SIGNATURE" }
            return null
        }

        /* If entry is not a KeyStore.PrivateKeyEntry, it might have gotten stored in a previous
         * iteration of your application that was using some other mechanism, or been overwritten
         * by something else using the same keystore with the same alias.
         * You can determine the type using entry.getClass() and debug from there.
         */
        if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.e { "getPrivateKey(): Not an instance of a PrivateKeyEntry" }
            return null
        }
        return entry
    }

    private fun getKeyStore(): KeyStore {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
        // it's obligatory to load 'AndroidKeyStore' with default parameters
        ks.load(null)
        return ks
    }

}