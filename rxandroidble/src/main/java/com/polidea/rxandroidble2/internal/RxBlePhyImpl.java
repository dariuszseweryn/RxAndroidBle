package com.polidea.rxandroidble2.internal;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.PhyPair;
import com.polidea.rxandroidble2.RxBlePhy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public final class RxBlePhyImpl implements RxBlePhy {

    /**
     * Bluetooth LE 1M PHY.
     */
    public static final RxBlePhyImpl PHY_1M = new RxBlePhyImpl("PHY_1M", 1, 1);

    /**
     * Bluetooth LE 2M PHY.
     */
    public static final RxBlePhyImpl PHY_2M = new RxBlePhyImpl("PHY_2M", 1 << 1, 2);

    /**
     * Bluetooth LE Coded PHY.
     */
    public static final RxBlePhyImpl PHY_CODED = new RxBlePhyImpl("PHY_CODED", 1 << 2, 3);

    private static final Set<RxBlePhy> BUILTIN_VALUES;

    static {
        HashSet<RxBlePhyImpl> builtinValues = new HashSet<>();
        builtinValues.add(PHY_1M);
        builtinValues.add(PHY_2M);
        builtinValues.add(PHY_CODED);
        BUILTIN_VALUES = Collections.unmodifiableSet(builtinValues);
    }

    /**
     * Used for user-friendly object toString()
     */
    final String toStringOverride;

    /**
     * Corresponds to e.g. {@link BluetoothDevice#PHY_LE_CODED_MASK}
     */
    final int mask;

    /**
     * Corresponds to e.g. {@link BluetoothDevice#PHY_LE_CODED}
     */
    final int value;

    private RxBlePhyImpl(final String builtInToString, final int mask, final int value) {
        this.toStringOverride = builtInToString;
        this.mask = mask;
        this.value = value;
    }

    private RxBlePhyImpl(final int mask, final int value) {
        this.toStringOverride = null;
        this.mask = mask;
        this.value = value;
    }

    public int getMask() {
        return mask;
    }

    public int getValue() {
        return value;
    }

    @NonNull
    @Override
    public String toString() {
        if (toStringOverride != null) {
            return toStringOverride;
        }
        return "RxBlePhy{[CUSTOM] "
                + "mask=" + mask
                + ", value=" + value
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RxBlePhy)) return false;
        RxBlePhy rxBlePhy = (RxBlePhy) o;
        return mask == rxBlePhy.getMask() && value == rxBlePhy.getValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(mask, value);
    }

    /**
     * Function used to get the PHY static object from an integer.
     *
     * @param value The integer value to try to get he PHY enum value for.
     *
     * @return The PHY value.
     */
    @NonNull
    private static RxBlePhy fromValue(final int value) {
        for (final RxBlePhy entry : RxBlePhyImpl.BUILTIN_VALUES) {
            if (entry.getValue() == value) {
                return entry;
            }
        }
        RxBleLog.e("Encountered an unexpected PHY value=%d. Please consider making a PR to the library.", value);
        return new RxBlePhyImpl(0, value);
    }

    /**
     * Function used to convert the specified enum set to the equivalent values mask.
     *
     * @param set The enum set to compute the values mask for.
     *
     * @return If the set is NULL, empty, then the default value mask, RxBlePhy.PHY_1M, is returned.
     * Otherwise, the resulting values mask is returned.
     */
    public static int enumSetToValuesMask(@Nullable final Set<RxBlePhyImpl> set) {
        if (set == null || set.size() == 0) {
            return RxBlePhyImpl.PHY_1M.getMask();
        }

        final Iterator<RxBlePhyImpl> iterator = set.iterator();

        int result = 0;

        while (iterator.hasNext()) {
            final int requestedValue = iterator.next().getMask();
            result |= requestedValue;
        }

        return result;
    }

    @NonNull
    public static PhyPair toPhyPair(int txPhy, int rxPhy) {
        // GATT callbacks do not use the same LE Coded value as it does for the setPreferredPhy function. GATT
        // callbacks use the unmasked value (BluetoothDevice.PHY_LE_CODED) and setPreferredPhy uses the mask value
        // (BluetoothDevice.PHY_LE_CODED_MASK). Explicitly check for BluetoothDevice.PHY_LE_CODED here and manually
        // set it to RxBlePhy.PHY_CODED since it too uses the masked version. This will abstract that confusion
        // away from the user and make it unified.
        RxBlePhy tx = RxBlePhyImpl.fromValue(txPhy);
        RxBlePhy rx = RxBlePhyImpl.fromValue(rxPhy);

        return new PhyPairImpl(tx, rx);
    }

    public static boolean isBuiltInValue(RxBlePhy phy) {
        return BUILTIN_VALUES.contains(phy);
    }

    public static RxBlePhyImpl custom(final int mask, final int value) {
        return new RxBlePhyImpl(mask, value);
    }
}
