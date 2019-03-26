package com.polidea.rxandroidble2.sample.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
    private static final Map<Integer, Integer> ERROR_MESSAGES = new HashMap<>();

    /*
      Add new mappings here.
     */
    static {
        ERROR_MESSAGES.put(BleScanException.BLUETOOTH_NOT_AVAILABLE, R.string.error_bluetooth_not_available);
        ERROR_MESSAGES.put(BleScanException.BLUETOOTH_DISABLED, R.string.error_bluetooth_disabled);
        ERROR_MESSAGES.put(BleScanException.LOCATION_PERMISSION_MISSING, R.string.error_location_permission_missing);
        ERROR_MESSAGES.put(BleScanException.LOCATION_SERVICES_DISABLED, R.string.error_location_services_disabled);
        ERROR_MESSAGES.put(BleScanException.SCAN_FAILED_ALREADY_STARTED, R.string.error_scan_failed_already_started);
        ERROR_MESSAGES.put(
                BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED,
                R.string.error_scan_failed_application_registration_failed
        );
        ERROR_MESSAGES.put(
                BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED,
                R.string.error_scan_failed_feature_unsupported
        );
        ERROR_MESSAGES.put(BleScanException.SCAN_FAILED_INTERNAL_ERROR, R.string.error_scan_failed_internal_error);
        ERROR_MESSAGES.put(
                BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES,
                R.string.error_scan_failed_out_of_hardware_resources
        );
        ERROR_MESSAGES.put(BleScanException.BLUETOOTH_CANNOT_START, R.string.error_bluetooth_cannot_start);
        ERROR_MESSAGES.put(BleScanException.UNKNOWN_ERROR_CODE, R.string.error_unknown_error);
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
            final Integer resId = ERROR_MESSAGES.get(reason);
            if (resId != null) {
                text = context.getString(resId);
            } else {
                // unknown error - return default message
                Log.w("Scanning", String.format("No message found for reason=%d. Consider adding one.", reason));
                text = context.getString(R.string.error_unknown_error);
            }
        }

        Log.w("Scanning", text, exception);
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
