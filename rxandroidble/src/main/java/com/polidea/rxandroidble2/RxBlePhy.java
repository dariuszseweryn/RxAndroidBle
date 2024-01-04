package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble2.internal.RxBlePhyImpl;

import java.util.Set;

/**
 * The interface used in {@link Set} for requesting PHY when calling {@link RxBleConnection#setPreferredPhy(Set, Set, RxBlePhyOption)} and
 * inside {@link PhyPair} as results of {@link RxBleConnection#readPhy()} and
 * {@link RxBleConnection#setPreferredPhy(Set, Set, RxBlePhyOption)}
 */
public interface RxBlePhy {

    /**
     * Bluetooth LE 1M PHY.
     */
    RxBlePhy PHY_1M = RxBlePhyImpl.PHY_1M;

    /**
     * Bluetooth LE 2M PHY.
     */
    RxBlePhy PHY_2M = RxBlePhyImpl.PHY_2M;

    /**
     * Bluetooth LE Coded PHY.
     */
    RxBlePhy PHY_CODED = RxBlePhyImpl.PHY_CODED;

    /**
     * Corresponds to e.g. {@link BluetoothDevice#PHY_LE_CODED_MASK}
     */
    int getMask();

    /**
     * Corresponds to e.g. {@link BluetoothDevice#PHY_LE_CODED}
     */
    int getValue();
}
