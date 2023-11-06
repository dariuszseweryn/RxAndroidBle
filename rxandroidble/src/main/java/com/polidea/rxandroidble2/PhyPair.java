package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PhyPair {
    public final RxBlePhy txPhy;
    public final RxBlePhy rxPhy;

    public PhyPair(@NonNull final RxBlePhy txPhy, @NonNull final RxBlePhy rxPhy) {
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
    }

    @NonNull
    public static PhyPair fromGattCallback(int txPhy, int rxPhy, int status) {
        RxBlePhy tx = RxBlePhy.PHY_UNKNOWN;
        RxBlePhy rx = RxBlePhy.PHY_UNKNOWN;

        // GATT callbacks do not use the same LE Coded value as it does for the setPreferredPhy function. GATT
        // callbacks use the unmasked value (BluetoothDevice.PHY_LE_CODED) and setPreferredPhy uses the mask value
        // (BluetoothDevice.PHY_LE_CODED_MASK). Explicitly check for BluetoothDevice.PHY_LE_CODED here and manually
        // set it to RxBlePhy.PHY_CODED since it too uses the masked version. This will abstract that confusion
        // away from the user and make it unified.
        if (status == BluetoothGatt.GATT_SUCCESS) {
            tx = txPhy == BluetoothDevice.PHY_LE_CODED ? RxBlePhy.PHY_CODED : RxBlePhy.fromInt(txPhy);
            rx = rxPhy == BluetoothDevice.PHY_LE_CODED ? RxBlePhy.PHY_CODED : RxBlePhy.fromInt(rxPhy);
        }

        return new PhyPair(tx, rx);
    }

    @Override
    public int hashCode() {
        return txPhy.hashCode() ^ rxPhy.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return txPhy.equals(rxPhy);
    }
}
