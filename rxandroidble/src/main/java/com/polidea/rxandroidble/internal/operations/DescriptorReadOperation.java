package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.utils.BytePrinter;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

public class DescriptorReadOperation extends SingleResponseOperation<ByteAssociation<BluetoothGattDescriptor>> {

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    @Inject
    DescriptorReadOperation(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                            @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                            BluetoothGattDescriptor descriptor, OperationEventLogger eventLogger) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.DESCRIPTOR_READ, timeoutConfiguration, eventLogger);
        bluetoothGattDescriptor = descriptor;
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

    @NonNull
    @Override
    protected OperationDescription createOperationDescription() {
        return new OperationDescription(new OperationAttribute(OperationExtras.UUID, bluetoothGattDescriptor.getUuid().toString()));
    }

    @Nullable
    @Override
    protected String createOperationResultDescription(ByteAssociation<BluetoothGattDescriptor> result) {
        return BytePrinter.toPrettyFormattedHexString(result.second);
    }
}
