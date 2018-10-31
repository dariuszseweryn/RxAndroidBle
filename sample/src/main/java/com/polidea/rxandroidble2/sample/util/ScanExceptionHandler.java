package com.polidea.rxandroidble2.sample.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.sample.R;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to show BleScanException error messages as toasts.
 */
public class ScanExceptionHandler {

    private ScanExceptionHandler() {
        // Utility class
    }

    /**
     * Mapping of exception reasons to error string resource ids.
     */
    @SuppressLint("UseSparseArrays")
    private final static Map<Integer, Integer> errorMessages = new HashMap<>();

    /*
      Add new mappings here.
     */
    static {
        errorMessages.put(BleScanException.BLUETOOTH_NOT_AVAILABLE, R.string.error_bluetooth_not_available);
        errorMessages.put(BleScanException.BLUETOOTH_DISABLED, R.string.error_bluetooth_disabled);
        errorMessages.put(BleScanException.LOCATION_PERMISSION_MISSING, R.string.error_location_permission_missing);
        errorMessages.put(BleScanException.LOCATION_SERVICES_DISABLED, R.string.error_location_services_disabled);
        errorMessages.put(BleScanException.SCAN_FAILED_ALREADY_STARTED, R.string.error_scan_failed_already_started);
        errorMessages.put(
                BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
                R.string.error_scan_failed_application_registration_failed
        );
        errorMessages.put(
                BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED,
                R.string.error_scan_failed_feature_unsupported
        );
        errorMessages.put(BleScanException.SCAN_FAILED_INTERNAL_ERROR, R.string.error_scan_failed_internal_error);
        errorMessages.put(
                BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES,
                R.string.error_scan_failed_out_of_hardware_resources
        );
        errorMessages.put(BleScanException.BLUETOOTH_CANNOT_START, R.string.error_bluetooth_cannot_start);
        errorMessages.put(BleScanException.UNKNOWN_ERROR_CODE, R.string.error_bluetooth_cannot_start);
    }

    /**
     * Show toast with error message appropriate to exception reason.
     *
     * @param context   current Activity context
     * @param exception BleScanException to show error message for
     */
    public static void handleException(final Activity context, final BleScanException exception) {
        final String text;
        final int reason = exception.getReason();

        // Special case, as there might or might not be a retry date suggestion
        if (reason == BleScanException.UNDOCUMENTED_SCAN_THROTTLE) {
            text = getUndocumentedScanThrottleErrorMessage(context, exception.getRetryDateSuggestion());
        } else {
            // Handle all other possible errors
            final Integer resId = errorMessages.get(reason);
            if (resId != null) {
                text = context.getString(resId);
            } else {
                // unknown error - return default message
                text = context.getString(R.string.error_bluetooth_cannot_start);
            }
        }

        Log.w("EXCEPTION", text, exception);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    private static String getUndocumentedScanThrottleErrorMessage(final Activity context, final Date retryDate) {
        final StringBuilder stringBuilder =
                new StringBuilder(context.getString(R.string.error_undocumented_scan_throttle));

        if (retryDate != null) {
            final String retryText = String.format(
                    Locale.getDefault(),
                    context.getString(R.string.error_undocumented_scan_throttle_retry),
                    secondsTill(retryDate)
            );
            stringBuilder.append(retryText);
        }

        return stringBuilder.toString();
    }

    private static long secondsTill(@NonNull final Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }
}
