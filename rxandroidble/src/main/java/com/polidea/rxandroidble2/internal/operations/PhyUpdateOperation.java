package com.polidea.rxandroidble2.internal.operations;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.PhyPair;
import com.polidea.rxandroidble2.RxBlePhy;
import com.polidea.rxandroidble2.RxBlePhyOption;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import java.util.EnumSet;
import java.util.Iterator;

import bleshadow.javax.inject.Inject;
import io.reactivex.Single;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
public class PhyUpdateOperation extends SingleResponseOperation<PhyPair> {

    EnumSet<RxBlePhy> txPhy;
    EnumSet<RxBlePhy> rxPhy;
    RxBlePhyOption phyOptions;

    @Inject
    PhyUpdateOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration,
            EnumSet<RxBlePhy> txPhy, EnumSet<RxBlePhy> rxPhy, RxBlePhyOption phyOptions) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.PHY_UPDATE, timeoutConfiguration);
        this.txPhy = txPhy;
        this.rxPhy = rxPhy;
        this.phyOptions = phyOptions;
    }

    @Override
    protected Single<PhyPair> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnPhyUpdate().firstOrError();
    }

    @Override
    @SuppressLint("MissingPermission")
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        final Iterator<RxBlePhy> txPhyIterator = txPhy.iterator();
        final Iterator<RxBlePhy> rxPhyIterator = rxPhy.iterator();

        // Set the default value to PHY 1Mbps, which is the default value for Bluetooth 4.2+. This will handle cases
        // where RxBlePhy.PHY_UNKNOWN is passed as the desired value.
        int tx = RxBlePhy.PHY_1M.getValue();
        int rx = RxBlePhy.PHY_1M.getValue();

        while (txPhyIterator.hasNext()) {
            tx |= txPhyIterator.next().getValue();
        }

        while (rxPhyIterator.hasNext()) {
            rx |= rxPhyIterator.next().getValue();
        }

        bluetoothGatt.setPreferredPhy(tx, rx, phyOptions.getValue());
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return "PhyUpdateOperation{"
                + super.toString()
                + ", txPhy=" + txPhy
                + ", rxPhy=" + rxPhy
                + ", phyOptions=" + phyOptions
                + '}';
    }
}
