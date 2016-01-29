package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationDescriptorWrite extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    private final byte[] data;

    public RxBleRadioOperationDescriptorWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                              BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
        this.data = data;
    }

    @Override
    public void run() {
        rxBleGattCallback
                .getOnDescriptorWrite()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattDescriptor))
                .take(1)
                .map(bluetoothGattDescriptorPair -> bluetoothGattDescriptorPair.second)
                .subscribe(getSubscriber());
        // TODO: [PU] 29.01.2016 Release radio
        bluetoothGattDescriptor.setValue(data);
        bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
    }
}
