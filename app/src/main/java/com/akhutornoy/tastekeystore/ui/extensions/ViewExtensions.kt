package com.akhutornoy.tastekeystore.ui.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

fun View.showSnack(@StringRes msgResId: Int) =
        showSnack(this.context.getString(msgResId))

fun View.showSnack(msg: String) =
        Snackbar.make(
                this,
                msg,
                Snackbar.LENGTH_SHORT
        ).show()

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
