package com.polidea.rxandroidble2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    /**
     * Function used to get the PHY enum value from an integer.
     *
     * @param value The integer value to try to get he PHY enum value for.
     *
     * @return The PHY enum value if a valid one was found; otherwise, RxBlePhy.PHY_UNKNOWN is returned.
     */
    @NonNull
    public static RxBlePhy fromInt(final int value) {
        for (final RxBlePhy entry : RxBlePhy.values()) {
            if (entry.getValue() == value) {
                return entry;
            }
        }
        RxBleLog.w("%d is not a valid PHY value.", value);
        return RxBlePhy.PHY_UNKNOWN;
    }

    /**
     * Function used to convert the specified enum set to the equivalent values mask.
     *
     * @param set The enum set to compute the values mask for.
     *
     * @return If the set is NULL, empty, or only contains the unknown value (RxBlePhy.PHY_UNKNOWN), then the default
     *         value mask, RxBlePhy.PHY_1M, is returned. Otherwise, the resulting values mask is returned.
     */
    public static int enumSetToValuesMask(@Nullable final EnumSet<RxBlePhy> set) {
        if (set == null || set.size() == 0) {
            return RxBlePhy.PHY_1M.getValue();
        }

        final Iterator<RxBlePhy> iterator = set.iterator();

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
