package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import rx.Observable;
import rx.Subscription;

import static com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider.RxBleOperations;
import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback.Provider gattCallbackProvider;
    private final RxBleConnectionConnectorOperationsProvider operationsProvider;
    private final RxBleRadio rxBleRadio;
    private final BleConnectionCompat connectionCompat;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;

    public RxBleConnectionConnectorImpl(BluetoothDevice bluetoothDevice, RxBleGattCallback.Provider gattCallbackProvider,
                                        RxBleConnectionConnectorOperationsProvider operationsProvider, RxBleRadio rxBleRadio,
                                        BleConnectionCompat connectionCompat, RxBleAdapterWrapper rxBleAdapterWrapper) {
        this.bluetoothDevice = bluetoothDevice; // TODO: pass in prepareConnection?
        this.gattCallbackProvider = gattCallbackProvider;
        this.operationsProvider = operationsProvider;
        this.rxBleRadio = rxBleRadio;
        this.connectionCompat = connectionCompat;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {
            if (!rxBleAdapterWrapper.isBluetoothEnabled()) return Observable.error(new BleDisconnectedException());

            final RxBleGattCallback gattCallback = gattCallbackProvider.provide();
            final RxBleOperations operationsPair =
                    operationsProvider.provide(context, bluetoothDevice, autoConnect, connectionCompat, gattCallback);

            return rxBleRadio.queue(operationsPair.connect)
                    .flatMap(bluetoothGatt -> emitConnectionWithoutCompleting(gattCallback, bluetoothGatt))
                    .mergeWith(gattCallback.observeDisconnect())
                    .doOnUnsubscribe(() -> enqueueDisconnectOperation(operationsPair.disconnect));
        });
    }

    private Observable<RxBleConnection> emitConnectionWithoutCompleting(RxBleGattCallback gattCallback, BluetoothGatt bluetoothGatt) {
        return justOnNext(new RxBleConnectionImpl(rxBleRadio, gattCallback, bluetoothGatt)).cast(RxBleConnection.class);
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
