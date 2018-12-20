package com.akhutornoy.tastekeystore.data

import android.content.SharedPreferences
import android.util.Base64
import com.akhutornoy.tastekeystore.security.AesKeystore
import com.akhutornoy.tastekeystore.data.extenstions.get
import com.akhutornoy.tastekeystore.data.extenstions.put
import java.io.*
import javax.crypto.SecretKey

const val AES_STRING_KEY = "ENCRYPTED_RSA_STRING_KEY"
const val AES_IV_KEY = "AES_IV_KEY"

class AesKeystoreImp(private val prefs: SharedPreferences): AesKeystore {

    override fun getKey(): SecretKey? {
        val keyString = prefs.get(AES_STRING_KEY, "")
        return if (keyString.isEmpty()) {
            null
        } else {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)

            val byteArrayInputStream = ByteArrayInputStream(keyBytes)
            val ois = ObjectInputStream(byteArrayInputStream)
            return ois.readObject() as SecretKey
        }
    }

    override fun putKey(key: SecretKey?) {
        val os = ByteArrayOutputStream()
        val oos = ObjectOutputStream(os)
        oos.writeObject(key)
        val keyBytes = os.toByteArray()
        val keyString = Base64.encodeToString(keyBytes, Base64.DEFAULT)
        prefs.put(AES_STRING_KEY, keyString)
    }

    override fun getIv(): ByteArray? {
        val ivString = prefs.get(AES_IV_KEY, "")
        return if (ivString.isEmpty()) {
            null
        } else {
            Base64.decode(ivString, Base64.DEFAULT)
        }
    }

    override fun putIv(iv: ByteArray?) {
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)
        prefs.put(AES_IV_KEY, ivString)
    }
}