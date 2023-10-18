package com.polidea.rxandroidble2;

import androidx.annotation.NonNull;

import java.util.EnumSet;

public enum RxBlePhy {
    /**
     * Unknown Bluetooth PHY. Used if the PHY couldn't be determined.
     *
     * @implNote If this value is used when calling the setPreferredPhy then it will use the radio's default PHY (1 Mb/s)
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
    public static final EnumSet<RxBlePhy> PHY_ALL = EnumSet.allOf(RxBlePhy.class);

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
        return RxBlePhy.PHY_UNKNOWN;
    }
}
