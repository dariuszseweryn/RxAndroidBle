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
    private final DisconnectOperation disconnectOperation
    private final ConnectOperation connectOperation
    private final BluetoothDevice bluetoothDevice
    private final ClientOperationQueue clientOperationQueue

    MockConnectionComponentBuilder(ClientOperationQueue clientOperationQueue,
                                   RxBleConnection rxBleConnection,
                                   BluetoothDevice bluetoothDevice,
                                   RxBleGattCallback rxBleGattCallback,
                                   DisconnectOperation disconnectOperation,
                                   ConnectOperation connectOperation) {
        this.clientOperationQueue = clientOperationQueue
        this.bluetoothDevice = bluetoothDevice
        this.connectOperation = connectOperation
        this.rxBleConnection = rxBleConnection
        this.rxBleGattCallback = rxBleGattCallback
        this.disconnectOperation = disconnectOperation
    }

    @Override
    ConnectionComponent.Builder connectionModule(ConnectionModule connectionModule) {
        return this
    }

    @Override
    ConnectionComponent build() {
        return new ConnectionComponent() {

            @Override
            ConnectOperation connectOperation() {
                return connectOperation
            }

            @Override
            DisconnectAction disconnectAction() {
                return new DisconnectAction(
                        new DummyOperationQueue(),
                        clientOperationQueue,
                        disconnectOperation,
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