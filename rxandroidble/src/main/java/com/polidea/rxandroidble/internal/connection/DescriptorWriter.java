package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattDescriptor;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import javax.inject.Inject;
import rx.Observable;

@ConnectionScope
class DescriptorWriter {

    private final RxBleRadio rxBleRadio;
    private final OperationsProvider operationsProvider;

    @Inject
    DescriptorWriter(RxBleRadio rxBleRadio, OperationsProvider operationsProvider) {
        this.rxBleRadio = rxBleRadio;
        this.operationsProvider = operationsProvider;
    }

    Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return rxBleRadio.queue(operationsProvider.provideWriteDescriptor(bluetoothGattDescriptor, data));
    }
}
