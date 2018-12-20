package com.akhutornoy.tastekeystore.ui.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import android.widget.Toast

fun Context.showToast(msg: String) =
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun Context.showToast(@StringRes msgResId: Int) =
    showToast(this.getString(msgResId))
