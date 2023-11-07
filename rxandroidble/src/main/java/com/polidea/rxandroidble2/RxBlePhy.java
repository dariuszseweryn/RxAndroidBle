package com.polidea.rxandroidble2;

import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.internal.RxBleLog;

import java.util.EnumSet;
import java.util.Iterator;

public enum RxBlePhy {
    /**
     * Unknown Bluetooth PHY. Used if the PHY couldn't be determined.
     *
     * @implNote If this value is used when calling the setPreferredPhy then it will default to PHY_1M
     */
    PHY_UNKNOWN(0),

    /**
     * Bluetooth LE 1M PHY.
     */
    PHY_1M(1),

    /**
     * Bluetooth LE 2M PHY.
     */
    PHY_2M(1 << 1),

    /**
     * Bluetooth LE Coded PHY.
     */
    PHY_CODED(1 << 2);

    private final int value;

    RxBlePhy(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    public static RxBlePhy fromInt(final int i) {
        for (final RxBlePhy entry : RxBlePhy.values()) {
            if (entry.getValue() == i) {
                return entry;
            }
        }
        RxBleLog.w("%d is not a valid PHY value.", i);
        return RxBlePhy.PHY_UNKNOWN;
    }

    public static int enumSetToInt(EnumSet<RxBlePhy> set) {
        final Iterator<RxBlePhy> iterator = set.iterator();

        if (set.size() == 0) {
            return RxBlePhy.PHY_1M.getValue();
        }

        if (set.size() == 1) {
            final int requestedValue = iterator.next().getValue();
            final boolean isUnknown = requestedValue == RxBlePhy.PHY_UNKNOWN.getValue();
            return isUnknown ? RxBlePhy.PHY_1M.getValue() : requestedValue;
        }

        int result = 0;

        while (iterator.hasNext()) {
            final int requestedValue = iterator.next().getValue();
            result |= requestedValue;
        }

        return result;
    }
}
