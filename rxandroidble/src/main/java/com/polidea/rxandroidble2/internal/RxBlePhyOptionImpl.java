package com.polidea.rxandroidble2.internal;

import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.RxBlePhyOption;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Coding to be used when transmitting on the LE Coded PHY.
 */
public final class RxBlePhyOptionImpl implements RxBlePhyOption {
    /**
     * No preferred coding.
     */
    public static final RxBlePhyOption PHY_OPTION_NO_PREFERRED = new RxBlePhyOptionImpl("PHY_OPTION_NO_PREFERRED", 0);

    /**
     * Prefer the S=2 coding.
     */
    public static final RxBlePhyOption PHY_OPTION_S2 = new RxBlePhyOptionImpl("PHY_OPTION_S2", 1);

    /**
     * Prefer the S=8 coding.
     */
    public static final RxBlePhyOption PHY_OPTION_S8 = new RxBlePhyOptionImpl("PHY_OPTION_S8", 2);

    private static final Set<RxBlePhyOption> BUILTIN_VALUES;

    static {
        HashSet<RxBlePhyOption> builtinValues = new HashSet<>();
        builtinValues.add(PHY_OPTION_NO_PREFERRED);
        builtinValues.add(PHY_OPTION_S2);
        builtinValues.add(PHY_OPTION_S8);
        BUILTIN_VALUES = Collections.unmodifiableSet(builtinValues);
    }

    /**
     * Used for user-friendly object toString()
     */
    private final String toStringOverride;

    private final int value;

    public RxBlePhyOptionImpl(String toStringOverride, final int value) {
        this.toStringOverride = toStringOverride;
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        if (toStringOverride != null) {
            return toStringOverride;
        }
        return "RxBlePhyOption{[CUSTOM] "
                + " value=" + value
                + '}';
    }

    public static RxBlePhyOptionImpl fromInterface(RxBlePhyOption phyOption) {
        int phyOptionsValue = phyOption.getValue();
        if (phyOption.getClass() != RxBlePhyOptionImpl.class || !BUILTIN_VALUES.contains(phyOption)) {
            RxBleLog.w("Using a custom RxBlePhyOption with value=%d. Please consider making a PR to the library.", phyOptionsValue);
        }
        return phyOption.getClass() == RxBlePhyOptionImpl.class
                ? (RxBlePhyOptionImpl) phyOption
                : new RxBlePhyOptionImpl(null, phyOptionsValue);
    }
}
