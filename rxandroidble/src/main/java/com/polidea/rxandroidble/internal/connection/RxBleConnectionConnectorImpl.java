package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v4.util.Pair;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.util.ObservableUtil;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;

import rx.Observable;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback.Provider gattCallbackProvider;

    private final RxBleConnectionConnectorOperationsProvider operationsProvider;

    private final RxBleRadio rxBleRadio;

    public RxBleConnectionConnectorImpl(BluetoothDevice bluetoothDevice, RxBleGattCallback.Provider gattCallbackProvider,
                                        RxBleConnectionConnectorOperationsProvider operationsProvider, RxBleRadio rxBleRadio) {
        this.bluetoothDevice = bluetoothDevice; // TODO: pass in prepareConnection?
        this.gattCallbackProvider = gattCallbackProvider;
        this.operationsProvider = operationsProvider;
        this.rxBleRadio = rxBleRadio;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {
            final RxBleGattCallback gattCallback = gattCallbackProvider.provide();
            final Pair<RxBleRadioOperationConnect, RxBleRadioOperationDisconnect> operationsPair =
                    operationsProvider.provide(context, bluetoothDevice, autoConnect, gattCallback);

            final RxBleRadioOperationConnect operationConnect = operationsPair.first;
            final RxBleRadioOperationDisconnect operationDisconnect = operationsPair.second;

            return rxBleRadio.queue(operationConnect)
                    .<RxBleConnection>flatMap(bluetoothGatt -> ObservableUtil
                                    .justOnNext(new RxBleConnectionImpl(rxBleRadio, gattCallback, bluetoothGatt))
                                    .mergeWith(gattCallback.disconnectedErrorObservable())
                    )
                    .doOnUnsubscribe(() ->
                                    rxBleRadio
                                            .queue(operationDisconnect)
                                            .subscribe(
                                                    ignored -> {
                                                    },
                                                    ignored -> {
                                                    }
                                            )
                    );
        });
    }

}
