package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

public class RxBleRadioOperationDescriptorRead extends RxBleGattRadioOperation<ByteAssociation<BluetoothGattDescriptor>> {

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    public RxBleRadioOperationDescriptorRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                             BluetoothGattDescriptor bluetoothGattDescriptor, Scheduler timeoutScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.DESCRIPTOR_READ, 30, TimeUnit.SECONDS, timeoutScheduler);
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
    }

    @Override
    protected Observable<ByteAssociation<BluetoothGattDescriptor>> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnDescriptorRead()
                .filter(new Func1<ByteAssociation<BluetoothGattDescriptor>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<BluetoothGattDescriptor> uuidPair) {
                        return uuidPair.first.equals(bluetoothGattDescriptor);
                    }
                });
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
    }
}
