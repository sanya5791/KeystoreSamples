package com.akhutornoy.tastekeystore

import android.content.Context
import com.akhutornoy.tastekeystore.data.AesKeystoreImp
import com.akhutornoy.tastekeystore.data.Prefs
import com.akhutornoy.tastekeystore.data.Repository
import com.akhutornoy.tastekeystore.security.AesEncrypter
import com.akhutornoy.tastekeystore.security.DataSignatureVerifier
import com.akhutornoy.tastekeystore.security.Encrypter
import com.akhutornoy.tastekeystore.security.RsaEncrypter

private const val PREFS = "prefs"
object Injections {
    fun provideRepository(context: Context): Repository = Prefs(getSharedPreferences(context))

    fun providePasswordVerifier(context: Context)= DataSignatureVerifier(context)

    fun provideRsaEncrypter(context: Context): Encrypter
            = RsaEncrypter(context)

    fun provideAesEncrypter(context: Context): Encrypter
            = AesEncrypter(AesKeystoreImp(getSharedPreferences(context)))

    private fun getSharedPreferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}