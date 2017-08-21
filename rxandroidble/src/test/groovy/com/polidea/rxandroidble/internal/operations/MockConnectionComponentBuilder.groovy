package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.DummyOperationQueue
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.ConnectionComponent
import com.polidea.rxandroidble.internal.connection.ConnectionModule
import com.polidea.rxandroidble.internal.connection.DisconnectAction
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue

public class MockConnectionComponentBuilder implements ConnectionComponent.Builder {
    private final RxBleConnection rxBleConnection
    private final RxBleGattCallback rxBleGattCallback
    private final RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect
    private final RxBleRadioOperationConnect rxBleRadioOperationConnect
    private final BluetoothDevice bluetoothDevice
    private final ClientOperationQueue clientOperationQueue

    MockConnectionComponentBuilder(ClientOperationQueue clientOperationQueue,
                                   RxBleConnection rxBleConnection,
                                   BluetoothDevice bluetoothDevice,
                                   RxBleGattCallback rxBleGattCallback,
                                   RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect,
                                   RxBleRadioOperationConnect rxBleRadioOperationConnect) {
        this.clientOperationQueue = clientOperationQueue
        this.bluetoothDevice = bluetoothDevice
        this.rxBleRadioOperationConnect = rxBleRadioOperationConnect
        this.rxBleConnection = rxBleConnection
        this.rxBleGattCallback = rxBleGattCallback
        this.rxBleRadioOperationDisconnect = rxBleRadioOperationDisconnect
    }

    @Override
    ConnectionComponent.Builder connectionModule(ConnectionModule connectionModule) {
        return this
    }

    @Override
    ConnectionComponent build() {
        return new ConnectionComponent() {

            @Override
            RxBleRadioOperationConnect connectOperation() {
                return rxBleRadioOperationConnect
            }

            @Override
            DisconnectAction disconnectAction() {
                return new DisconnectAction(
                        new DummyOperationQueue(),
                        clientOperationQueue,
                        rxBleRadioOperationDisconnect,
                        bluetoothDevice
                )
            }

            @Override
            RxBleConnection rxBleConnection() {
                return rxBleConnection
            }

            @Override
            RxBleGattCallback gattCallback() {
                return rxBleGattCallback
            }
        }
    }
}