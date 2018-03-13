package com.polidea.rxandroidble2.internal.connection;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.internal.operations.ConnectOperation;
import bleshadow.dagger.Subcomponent;
import java.util.Set;

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
        static final String GATT_MTU_MINIMUM = "GATT_MTU_MINIMUM";
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
    RxBleConnection rxBleConnection();

    @ConnectionScope
    RxBleGattCallback gattCallback();

    @ConnectionScope
    Set<ConnectionSubscriptionWatcher> connectionSubscriptionWatchers();
}
