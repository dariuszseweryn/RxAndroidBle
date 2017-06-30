package com.polidea.rxandroidble;


import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper class for time periods used in the API (i.e. timeout time period)
 */
@SuppressWarnings("WeakerAccess")
public class TimePeriod {

    public final long time;

    public final TimeUnit timeUnit;

    public TimePeriod(@IntRange(from = 1) long time, @NonNull TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }
}
