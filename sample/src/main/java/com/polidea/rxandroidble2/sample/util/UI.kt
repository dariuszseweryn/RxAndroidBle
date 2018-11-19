package com.polidea.rxandroidble2.sample.util

import android.app.Activity
import android.support.annotation.IdRes
import android.support.design.widget.Snackbar

internal fun Activity.showSnackbarShort(@IdRes view: Int, text: CharSequence) {
    Snackbar.make(findViewById(view), text, Snackbar.LENGTH_SHORT).show()
}