package com.akhutornoy.tastekeystore.security

import javax.crypto.SecretKey

interface AesKeystore {
    fun getKey(): SecretKey?
    fun putKey(key: SecretKey?)
    fun getIv(): ByteArray?
    fun putIv(iv: ByteArray?)
}