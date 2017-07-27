package com.polidea.rxandroidble.internal.operations

import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.internal.connection.ConnectionComponent
import com.polidea.rxandroidble.internal.connection.ConnectionModule
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue
import rx.Observable

public class MockConnectionComponentBuilder implements ConnectionComponent.Builder {
    private final RxBleConnection rxBleConnection
    private final RxBleGattCallback rxBleGattCallback
    private final RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect
    private final RxBleRadioOperationConnect rxBleRadioOperationConnect

    MockConnectionComponentBuilder(RxBleConnection rxBleConnection,
                                   RxBleGattCallback rxBleGattCallback,
                                   RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect,
                                   RxBleRadioOperationConnect rxBleRadioOperationConnect) {
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
            RxBleRadioOperationDisconnect disconnectOperation() {
                return rxBleRadioOperationDisconnect
            }

            @Override
            RxBleConnection rxBleConnection() {
                return rxBleConnection
            }

            @Override
            RxBleGattCallback gattCallback() {
                return rxBleGattCallback
            }

            @Override
            ConnectionOperationQueue connectionOperationQueue() {
                return new ConnectionOperationQueue() {

                    @Override
                    void terminate(BleDisconnectedException disconnectedException) {

                    }

                    @Override
                    def <T> Observable<T> queue(Operation<T> operation) {
                        return null
                    }
                }
            }
        }
    }
}