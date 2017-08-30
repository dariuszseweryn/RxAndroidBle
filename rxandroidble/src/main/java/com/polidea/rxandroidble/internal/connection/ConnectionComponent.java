package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.ConnectOperation;

import dagger.Subcomponent;

@ConnectionScope
@Subcomponent(modules = {ConnectionModule.class, ConnectionModuleBinder.class})
public interface ConnectionComponent {

    class NamedBooleans {
        public static final String AUTO_CONNECT = "autoConnect";
        public static final String SUPPRESS_OPERATION_CHECKS = "suppressOperationChecks";
        private NamedBooleans() { }
    }

    class NamedInts {
        static final String GATT_WRITE_MTU_OVERHEAD = "GATT_WRITE_MTU_OVERHEAD";
        private NamedInts() { }
    }

    @Subcomponent.Builder
    interface Builder {

        Builder connectionModule(ConnectionModule connectionModule);

        ConnectionComponent build();
    }

    @ConnectionScope
    ConnectOperation connectOperation();

    @ConnectionScope
    DisconnectAction disconnectAction();

    @ConnectionScope
    RxBleConnection rxBleConnection();

    @ConnectionScope
    RxBleGattCallback gattCallback();
}
