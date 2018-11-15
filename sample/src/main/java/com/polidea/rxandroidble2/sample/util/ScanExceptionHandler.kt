package com.polidea.rxandroidble2.sample.util

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.R
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Helper functions to show BleScanException error messages as toasts.
 */

/**
 * Mapping of exception reasons to error string resource ids. Add new mappings here.
 */
private val ERROR_MESSAGES = mapOf(
    BleScanException.BLUETOOTH_NOT_AVAILABLE to R.string.error_bluetooth_not_available,
    BleScanException.BLUETOOTH_DISABLED to R.string.error_bluetooth_disabled,
    BleScanException.LOCATION_PERMISSION_MISSING to R.string.error_location_permission_missing,
    BleScanException.LOCATION_SERVICES_DISABLED to R.string.error_location_services_disabled,
    BleScanException.SCAN_FAILED_ALREADY_STARTED to R.string.error_scan_failed_already_started,
    BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED to
            R.string.error_scan_failed_application_registration_failed,
    BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED to R.string.error_scan_failed_feature_unsupported,
    BleScanException.SCAN_FAILED_INTERNAL_ERROR to R.string.error_scan_failed_internal_error,
    BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES to R.string.error_scan_failed_out_of_hardware_resources,
    BleScanException.BLUETOOTH_CANNOT_START to R.string.error_bluetooth_cannot_start,
    BleScanException.UNKNOWN_ERROR_CODE to R.string.error_unknown_error
)

/**
 * Show toast in this Activity with error message appropriate to exception reason.
 *
 * @param exception BleScanException to show error message for
 */
internal fun Activity.handleException(exception: BleScanException) {
    val text: String
    val reason = exception.reason

    // Special case, as there might or might not be a retry date suggestion
    if (reason == BleScanException.UNDOCUMENTED_SCAN_THROTTLE) {
        text = getUndocumentedScanThrottleErrorMessage(exception.retryDateSuggestion)
    } else {
        // Handle all other possible errors
        val resId = ERROR_MESSAGES[reason]
        if (resId != null) {
            text = getString(resId)
        } else {
            // unknown error - return default message
            Log.w("Scanning", "No message found for reason=$reason. Consider adding one.")
            text = getString(R.string.error_unknown_error)
        }
    }

    Log.w("Scanning", text, exception)
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

private fun Activity.getUndocumentedScanThrottleErrorMessage(retryDate: Date?): String =
    StringBuilder(getString(R.string.error_undocumented_scan_throttle)).run {
        if (retryDate != null) {
            val retryText = String.format(
                Locale.getDefault(),
                getString(R.string.error_undocumented_scan_throttle_retry),
                secondsTill(retryDate)
            )
            append(retryText)
        }
        toString()
    }

private fun secondsTill(retryDateSuggestion: Date): Long =
    TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.time - System.currentTimeMillis())
