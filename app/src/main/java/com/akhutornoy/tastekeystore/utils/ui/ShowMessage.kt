package com.akhutornoy.tastekeystore.utils.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

fun showSnack(view: View, @StringRes msgResId: Int) {
    Snackbar.make(
        view,
        msgResId,
        Snackbar.LENGTH_SHORT
    ).show()
}

fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun showToast(context: Context, @StringRes msgResId: Int) {
    showToast(context, context.getString(msgResId))
}