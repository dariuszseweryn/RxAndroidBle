package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.operations.DisconnectOperation;
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue;
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;

@ConnectionScope
public class DisconnectAction implements Action0 {

    private final ConnectionOperationQueue connectionOperationQueue;
    private final ClientOperationQueue clientOperationQueue;
    private final DisconnectOperation operationDisconnect;
    private final BluetoothDevice bluetoothDevice;

    @Inject
    public DisconnectAction(ConnectionOperationQueue connectionOperationQueue, ClientOperationQueue clientOperationQueue,
                            DisconnectOperation operationDisconnect, BluetoothDevice bluetoothDevice) {
        this.connectionOperationQueue = connectionOperationQueue;
        this.clientOperationQueue = clientOperationQueue;
        this.operationDisconnect = operationDisconnect;
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public void call() {
        connectionOperationQueue.terminate(new BleDisconnectedException(bluetoothDevice.getAddress()));
        enqueueDisconnectOperation(operationDisconnect);
    }

    private Subscription enqueueDisconnectOperation(DisconnectOperation operationDisconnect) {
        return clientOperationQueue
                .queue(operationDisconnect)
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }
}
