package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.support.v4.util.Pair;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import rx.Observable;
import rx.Subscription;

import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback.Provider gattCallbackProvider;
    private final RxBleConnectionConnectorOperationsProvider operationsProvider;
    private final RxBleRadio rxBleRadio;
    private final BleConnectionCompat connectionCompat;

    public RxBleConnectionConnectorImpl(BluetoothDevice bluetoothDevice, RxBleGattCallback.Provider gattCallbackProvider,
                                        RxBleConnectionConnectorOperationsProvider operationsProvider, RxBleRadio rxBleRadio,
                                        BleConnectionCompat connectionCompat) {
        this.bluetoothDevice = bluetoothDevice; // TODO: pass in prepareConnection?
        this.gattCallbackProvider = gattCallbackProvider;
        this.operationsProvider = operationsProvider;
        this.rxBleRadio = rxBleRadio;
        this.connectionCompat = connectionCompat;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {
            final RxBleGattCallback gattCallback = gattCallbackProvider.provide();
            final Pair<RxBleRadioOperationConnect, RxBleRadioOperationDisconnect> operationsPair =
                    operationsProvider.provide(context, bluetoothDevice, autoConnect, connectionCompat, gattCallback);

            final RxBleRadioOperationConnect operationConnect = operationsPair.first;
            final RxBleRadioOperationDisconnect operationDisconnect = operationsPair.second;

            return rxBleRadio.queue(operationConnect)
                    .flatMap(bluetoothGatt -> createBleConnection(gattCallback, bluetoothGatt))
                    .cast(RxBleConnection.class)
                    .doOnUnsubscribe(() -> enqueueDisconnectOperation(operationDisconnect));
        });
    }

    private Observable<RxBleConnectionImpl> createBleConnection(RxBleGattCallback gattCallback, BluetoothGatt bluetoothGatt) {
        return justOnNext(new RxBleConnectionImpl(rxBleRadio, gattCallback, bluetoothGatt))
                .mergeWith(gattCallback.disconnectedErrorObservable());
    }

    private Subscription enqueueDisconnectOperation(RxBleRadioOperationDisconnect operationDisconnect) {
        return rxBleRadio
                .queue(operationDisconnect)
                .subscribe(
                        ignored -> {
                        },
                        ignored -> {
                        }
                );
    }

}
