package com.polidea.rxandroidble2.sample.util

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.R
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Helper class to show BleScanException error messages as toasts.
 */
object ScanExceptionHandler {

    /**
     * Mapping of exception reasons to error string resource ids.
     */
    @SuppressLint("UseSparseArrays")
    private val ERROR_MESSAGES = HashMap<Int, Int>()

    /*
      Add new mappings here.
     */
    init {
        ERROR_MESSAGES[BleScanException.BLUETOOTH_NOT_AVAILABLE] = R.string.error_bluetooth_not_available
        ERROR_MESSAGES[BleScanException.BLUETOOTH_DISABLED] = R.string.error_bluetooth_disabled
        ERROR_MESSAGES[BleScanException.LOCATION_PERMISSION_MISSING] = R.string.error_location_permission_missing
        ERROR_MESSAGES[BleScanException.LOCATION_SERVICES_DISABLED] = R.string.error_location_services_disabled
        ERROR_MESSAGES[BleScanException.SCAN_FAILED_ALREADY_STARTED] = R.string.error_scan_failed_already_started
        ERROR_MESSAGES[BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED] =
                R.string.error_scan_failed_application_registration_failed
        ERROR_MESSAGES[BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED] =
                R.string.error_scan_failed_feature_unsupported
        ERROR_MESSAGES[BleScanException.SCAN_FAILED_INTERNAL_ERROR] = R.string.error_scan_failed_internal_error
        ERROR_MESSAGES[BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES] =
                R.string.error_scan_failed_out_of_hardware_resources
        ERROR_MESSAGES[BleScanException.BLUETOOTH_CANNOT_START] = R.string.error_bluetooth_cannot_start
        ERROR_MESSAGES[BleScanException.UNKNOWN_ERROR_CODE] = R.string.error_unknown_error
    }

    /**
     * Show toast with error message appropriate to exception reason.
     *
     * @param context   current Activity context
     * @param exception BleScanException to show error message for
     */
    fun handleException(context: Activity, exception: BleScanException) {
        val text: String
        val reason = exception.reason

        // Special case, as there might or might not be a retry date suggestion
        if (reason == BleScanException.UNDOCUMENTED_SCAN_THROTTLE) {
            text = getUndocumentedScanThrottleErrorMessage(context, exception.retryDateSuggestion)
        } else {
            // Handle all other possible errors
            val resId = ERROR_MESSAGES[reason]
            if (resId != null) {
                text = context.getString(resId)
            } else {
                // unknown error - return default message
                Log.w("Scanning", String.format("No message found for reason=%d. Consider adding one.", reason))
                text = context.getString(R.string.error_unknown_error)
            }
        }

        Log.w("Scanning", text, exception)
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun getUndocumentedScanThrottleErrorMessage(context: Activity, retryDate: Date?): String {
        val stringBuilder = StringBuilder(context.getString(R.string.error_undocumented_scan_throttle))

        if (retryDate != null) {
            val retryText = String.format(
                Locale.getDefault(),
                context.getString(R.string.error_undocumented_scan_throttle_retry),
                secondsTill(retryDate)
            )
            stringBuilder.append(retryText)
        }

        return stringBuilder.toString()
    }

    private fun secondsTill(retryDateSuggestion: Date): Long {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.time - System.currentTimeMillis())
    }
}// Utility class
