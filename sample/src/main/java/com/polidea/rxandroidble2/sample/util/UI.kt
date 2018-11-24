package com.polidea.rxandroidble2.sample.util

import android.app.Activity
import android.support.annotation.IdRes
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import com.polidea.rxandroidble2.sample.R

internal fun Activity.showSnackbarShort(@IdRes view: Int = R.id.content, text: CharSequence) {
    Snackbar.make(findViewById(view), text, Snackbar.LENGTH_SHORT).show()
}

internal fun Activity.showSnackbarShort(@IdRes view: Int = R.id.content, @StringRes text: Int) {
    Snackbar.make(findViewById(view), text, Snackbar.LENGTH_SHORT).show()
}