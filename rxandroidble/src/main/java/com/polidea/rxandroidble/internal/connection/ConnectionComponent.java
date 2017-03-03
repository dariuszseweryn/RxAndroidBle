package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;

import dagger.Subcomponent;

@ConnectionScope
@Subcomponent(modules = ConnectionModule.class)
public interface ConnectionComponent {

    @Subcomponent.Builder
    interface Builder {

        ConnectionComponent build();
    }

    @ConnectionScope
    RxBleRadioOperationConnect.Builder connectOperationBuilder();

    @ConnectionScope
    RxBleRadioOperationDisconnect disconnectOperation();

    @ConnectionScope
    RxBleConnection rxBleConnection();

    @ConnectionScope
    RxBleGattCallback gattCallback();
}
