package com.akhutornoy.tastekeystore.security

interface Encrypter {
    fun createKey()
    fun isKeyCreated(): Boolean
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}