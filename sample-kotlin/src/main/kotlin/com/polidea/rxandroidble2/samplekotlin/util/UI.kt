package com.polidea.rxandroidble2.samplekotlin.util

import android.app.Activity
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

internal fun Activity.showSnackbarShort(text: CharSequence) {
    Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
}

internal fun Activity.showSnackbarShort(@StringRes text: Int) {
    Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
}