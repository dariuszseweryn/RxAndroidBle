package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;

import dagger.Subcomponent;

@ConnectionScope
@Subcomponent(modules = {ConnectionModule.class, ConnectionModuleBinder.class})
public interface ConnectionComponent {

    class NamedBooleans {
        public static final String AUTO_CONNECT = "autoConnect";
        public static final String SUPPRESS_OPERATION_CHECKS = "suppressOperationChecks";
        private NamedBooleans() { }
    }

    @Subcomponent.Builder
    interface Builder {

        Builder connectionModule(ConnectionModule connectionModule);

        ConnectionComponent build();
    }

    @ConnectionScope
    RxBleRadioOperationConnect connectOperation();

    @ConnectionScope
    RxBleRadioOperationDisconnect disconnectOperation();

    @ConnectionScope
    RxBleConnection rxBleConnection();

    @ConnectionScope
    RxBleGattCallback gattCallback();
}
