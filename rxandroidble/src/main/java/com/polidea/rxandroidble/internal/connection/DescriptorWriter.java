package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue;
import javax.inject.Inject;
import rx.Observable;

@ConnectionScope
class DescriptorWriter {

    private final ConnectionOperationQueue operationQueue;
    private final OperationsProvider operationsProvider;

    @Inject
    DescriptorWriter(ConnectionOperationQueue operationQueue, OperationsProvider operationsProvider) {
        this.operationQueue = operationQueue;
        this.operationsProvider = operationsProvider;
    }

    Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return operationQueue.queue(operationsProvider.provideWriteDescriptor(bluetoothGattDescriptor, data));
    }
}
