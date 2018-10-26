package com.akhutornoy.tastekeystore

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import com.github.ajalt.timberkt.Timber

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.security.auth.x500.X500Principal

private const val ALIAS = "alias"
private const val PASSWORD = "PASSWORD"

private const val KEYSTORE_PROVIDER_ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TYPE_RSA = "RSA"
private const val SIGNATURE_SHA256withRSA = "SHA256withRSA"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        create_keys_button.setOnClickListener { createKeys(this) }
        sign_data_button.setOnClickListener { signData(PASSWORD) }
        verify_data_button.setOnClickListener { signDataAndVerify() }
        read_data_button.setOnClickListener { readFromKeystore() }

        fab.setOnClickListener { action() }
    }

    private fun signDataAndVerify() {
        val signature = signData(PASSWORD)
        val isValid = verifyData(input = PASSWORD, signatureStr = signature)
        Timber.d { "signDataAndVerify(): isDataValid=$isValid" }
    }

    private fun action() {
        printAllKeystoreAliases()
    }

    private fun printAllKeystoreAliases() {
        val ks = getKeyStore()

        val aliases = ks.aliases()
        while (aliases.hasMoreElements()) {
            Timber.d { "action(): alias=${aliases.nextElement()}" }
        }
    }

    private fun getKeyStore(): KeyStore {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER_ANDROID_KEYSTORE)
        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)
        return ks
    }

    private fun readFromKeystore(): String {
        val ks = getKeyStore()
        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(ALIAS, null)

        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // BEGIN_INCLUDE(verify_data)
        // Verify the data.
        s.initVerify((entry as KeyStore.PrivateKeyEntry).certificate)

        Timber.d { "readFromKeystore(): $s" }

        return ""
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
            Timber.d { "verifyData(): Invalid signature." }
            Timber.d { "verifyData(): Exiting verifyData()..." }
            return false
        }

        try {
            // The signature is going to be examined as a byte array,
            // not as a base64 encoded string.
            signature = Base64.decode(signatureStr, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            // signatureStr wasn't null, but might not have been encoded properly.
            // It's not a valid Base64 string.
            return false
        }

        // END_INCLUDE(decode_signature)

        val ks = KeyStore.getInstance("AndroidKeyStore")

        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)

        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(ALIAS, null)

        if (entry == null) {
            Timber.d { "verifyData(): No key found under alias: $ALIAS" }
            Timber.d { "verifyData(): Exiting verifyData()..." }
            return false
        }

        if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.d { "verifyData(): Not an instance of a PrivateKeyEntry" }
            return false
        }

        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // BEGIN_INCLUDE(verify_data)
        // Verify the data.
        s.initVerify(entry.certificate)
        s.update(data)
        return s.verify(signature)
        // END_INCLUDE(verify_data)
    }


    /**
     * Creates a public and private key and stores it using the Android Key Store, so that only
     * this application will be able to access the keys.
     */
    @Throws(NoSuchProviderException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
    fun createKeys(context: Context?) {
        // BEGIN_INCLUDE(create_valid_dates)
        // Create a start and end time, for the validity range of the key pair that's about to be
        // generated.
        val start = GregorianCalendar()
        val end = GregorianCalendar()
        end.add(Calendar.YEAR, 1)
        //END_INCLUDE(create_valid_dates)

        // BEGIN_INCLUDE(create_keypair)
        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
        // and the KeyStore.  This example uses the AndroidKeyStore.
        val kpGenerator = KeyPairGenerator
            .getInstance(
                TYPE_RSA,
                KEYSTORE_PROVIDER_ANDROID_KEYSTORE
            )
        // END_INCLUDE(create_keypair)

        // BEGIN_INCLUDE(create_spec)
        // The KeyPairGeneratorSpec object is how parameters for your key pair are passed
        // to the KeyPairGenerator.
        val spec: AlgorithmParameterSpec

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Below Android M, use the KeyPairGeneratorSpec.Builder.

            spec = KeyPairGeneratorSpec.Builder(context!!)
                // You'll use the alias later to retrieve the key.  It's a key for the key!
                .setAlias(ALIAS)
                // The subject used for the self-signed certificate of the generated pair
                .setSubject(X500Principal("CN=$ALIAS"))
                // The serial number used for the self-signed certificate of the
                // generated pair.
                .setSerialNumber(BigInteger.valueOf(1337))
                // Date range of validity for the generated pair.
                .setStartDate(start.time)
                .setEndDate(end.time)
                .build()


        } else {
            // On Android M or above, use the KeyGenparameterSpec.Builder and specify permitted
            // properties  and restrictions of the key.
            spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setCertificateSubject(X500Principal("CN=$ALIAS"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSerialNumber(BigInteger.valueOf(1337))
                .setCertificateNotBefore(start.time)
                .setCertificateNotAfter(end.time)
                .build()
        }

        kpGenerator.initialize(spec)

        val kp = kpGenerator.generateKeyPair()
        // END_INCLUDE(create_spec)
        Timber.d { "createKeys(): Public Key is: ${kp.public}" }
        Timber.d { "createKeys(): Private Key is: ${kp.private}" }
    }

    /**
     * Signs the data using the key pair stored in the Android Key Store.  This signature can be
     * used with the data later to verify it was signed by this application.
     * @return A string encoding of the data signature generated
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
    fun signData(inputStr: String): String? {
        val data = inputStr.toByteArray()

        // BEGIN_INCLUDE(sign_load_keystore)
        val ks = getKeyStore()

        // Load the key pair from the Android Key Store
        val entry = ks.getEntry(ALIAS, null)

        /* If the entry is null, keys were never stored under this alias.
         * Debug steps in this situation would be:
         * -Check the list of aliases by iterating over Keystore.aliases(), be sure the alias
         *   exists.
         * -If that's empty, verify they were both stored and pulled from the same keystore
         *   "AndroidKeyStore"
         */
        if (entry == null) {
            Timber.d { "signData(): No key found under alias: $ALIAS" }
            Timber.d { "signData(): Exiting signData()..." }
            return null
        }

        /* If entry is not a KeyStore.PrivateKeyEntry, it might have gotten stored in a previous
         * iteration of your application that was using some other mechanism, or been overwritten
         * by something else using the same keystore with the same alias.
         * You can determine the type using entry.getClass() and debug from there.
         */
        if (entry !is KeyStore.PrivateKeyEntry) {
            Timber.d { "signData(): Not an instance of a PrivateKeyEntry" }
            Timber.d { "signData(): Exiting signData()..." }
            return null
        }
        // END_INCLUDE(sign_data)

        // BEGIN_INCLUDE(sign_create_signature)
        // This class doesn't actually represent the signature,
        // just the engine for creating/verifying signatures, using
        // the specified algorithm.
        val s = Signature.getInstance(SIGNATURE_SHA256withRSA)

        // Initialize Signature using specified private key
        s.initSign(entry.privateKey)

        // Sign the data, store the result as a Base64 encoded String.
        s.update(data)
        val signatureBytes = s.sign()
        // END_INCLUDE(sign_data)

        val signatureString = Base64.encodeToString(signatureBytes, Base64.DEFAULT)
        Timber.d { "signData(): signature=$signatureString" }
        return signatureString
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
