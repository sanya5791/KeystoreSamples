package com.akhutornoy.tastekeystore.data

import android.content.SharedPreferences
import com.akhutornoy.tastekeystore.utils.get
import com.akhutornoy.tastekeystore.utils.put

const val SIGNATURE_KEY = "SIGNATURE_KEY"
const val ENCRYPTED_RSA_STRING_KEY = "ENCRYPTED_RSA_STRING_KEY"
class Prefs(private val prefs: SharedPreferences): Repository {
    override var signature: String
        get() = prefs.get(SIGNATURE_KEY, "")
        set(value) { prefs.put(SIGNATURE_KEY, value) }

    override var encryptedRsaString: String
        get() = prefs.getString(ENCRYPTED_RSA_STRING_KEY, "")!!
        set(value) { prefs.put(ENCRYPTED_RSA_STRING_KEY, value) }
}