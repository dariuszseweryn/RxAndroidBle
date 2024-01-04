package com.polidea.rxandroidble2;

import com.polidea.rxandroidble2.internal.RxBlePhyOptionImpl;

/**
 * Coding to be used when transmitting on the LE Coded PHY.
 */
public interface RxBlePhyOption {
    /**
     * No preferred coding.
     */
    RxBlePhyOption PHY_OPTION_NO_PREFERRED = RxBlePhyOptionImpl.PHY_OPTION_NO_PREFERRED;

    /**
     * Prefer the S=2 coding.
     */
    RxBlePhyOption PHY_OPTION_S2 = RxBlePhyOptionImpl.PHY_OPTION_S2;

    /**
     * Prefer the S=8 coding.
     */
    RxBlePhyOption PHY_OPTION_S8 = RxBlePhyOptionImpl.PHY_OPTION_S8;

    /**
     *
     * @return integer value representing PHY option, e.g. {@link android.bluetooth.BluetoothDevice#PHY_OPTION_S2}
     */
    int getValue();
}
