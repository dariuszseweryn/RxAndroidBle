package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationDescriptorRead extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    public RxBleRadioOperationDescriptorRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                             BluetoothGattDescriptor bluetoothGattDescriptor) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
    }

    @Override
    public void run() {
        rxBleGattCallback
                .getOnDescriptorRead()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattDescriptor))
                .take(1)
                .map(bluetoothGattDescriptorPair -> bluetoothGattDescriptorPair.second)
                .subscribe(getSubscriber());
        // TODO: [PU] 29.01.2016 Release radio
        bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
    }
}
