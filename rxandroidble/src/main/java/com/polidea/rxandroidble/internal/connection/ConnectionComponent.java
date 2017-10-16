package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.ConnectOperation;

import dagger.Subcomponent;
import javax.inject.Named;
import rx.Completable;

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

    class NamedCompletables {
        static final String MTU_WATCHER_COMPLETABLE = "MTU_WATCHER_COMPLETABLE";
        // static final String CONNECTION_PRIORITY_WATCHER_COMPLETABLE = "MTU_WATCHER_COMPLETABLE";
        private NamedCompletables() { }
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

    @ConnectionScope
    @Named(NamedCompletables.MTU_WATCHER_COMPLETABLE)
    Completable mtuWatcherCompletable();
}
