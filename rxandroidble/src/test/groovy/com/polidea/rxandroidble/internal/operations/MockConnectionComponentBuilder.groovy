package com.polidea.rxandroidble.internal.operations

import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.ConnectionComponent
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback

public class MockConnectionComponentBuilder implements ConnectionComponent.Builder {
    private final RxBleConnection rxBleConnection
    private final RxBleGattCallback rxBleGattCallback
    private final RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect
    private final RxBleRadioOperationConnect.Builder rxBleRadioOperationConnectBuilder

    MockConnectionComponentBuilder(RxBleConnection rxBleConnection,
                                   RxBleGattCallback rxBleGattCallback,
                                   RxBleRadioOperationDisconnect rxBleRadioOperationDisconnect,
                                   RxBleRadioOperationConnect.Builder rxBleRadioOperationConnectBuilder) {
        this.rxBleRadioOperationConnectBuilder = rxBleRadioOperationConnectBuilder
        this.rxBleConnection = rxBleConnection
        this.rxBleGattCallback = rxBleGattCallback
        this.rxBleRadioOperationDisconnect = rxBleRadioOperationDisconnect
    }

    @Override
    ConnectionComponent build() {
        return new ConnectionComponent() {

            @Override
            RxBleRadioOperationConnect.Builder connectOperationBuilder() {
                return rxBleRadioOperationConnectBuilder
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
        }
    }
}