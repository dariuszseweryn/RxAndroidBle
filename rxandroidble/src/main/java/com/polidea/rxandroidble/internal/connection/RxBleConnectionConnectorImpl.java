package com.polidea.rxandroidble.internal.connection;

import static com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider.RxBleOperations;
import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback.Provider gattCallbackProvider;
    private final RxBleConnectionConnectorOperationsProvider operationsProvider;
    private final RxBleRadio rxBleRadio;
    private final BleConnectionCompat connectionCompat;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> adapterStateObservable;

    public RxBleConnectionConnectorImpl(BluetoothDevice bluetoothDevice, RxBleGattCallback.Provider gattCallbackProvider,
                                        RxBleConnectionConnectorOperationsProvider operationsProvider, RxBleRadio rxBleRadio,
                                        BleConnectionCompat connectionCompat, RxBleAdapterWrapper rxBleAdapterWrapper,
                                        Observable<BleAdapterState> adapterStateObservable) {
        this.bluetoothDevice = bluetoothDevice; // TODO: pass in prepareConnection?
        this.gattCallbackProvider = gattCallbackProvider;
        this.operationsProvider = operationsProvider;
        this.rxBleRadio = rxBleRadio;
        this.connectionCompat = connectionCompat;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.adapterStateObservable = adapterStateObservable;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final Context context, final boolean autoConnect) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {
                if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
                    return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                }

                final RxBleGattCallback gattCallback = gattCallbackProvider.provide();
                final RxBleOperations operationsPair =
                        operationsProvider.provide(context, bluetoothDevice, autoConnect, connectionCompat, gattCallback);


                return Observable.merge(
                        rxBleRadio.queue(operationsPair.connect),
                        adapterStateObservable
                                .filter(new Func1<BleAdapterState, Boolean>() {
                                    @Override
                                    public Boolean call(BleAdapterState bleAdapterState) {
                                        return !bleAdapterState.isUsable();
                                    }
                                })
                                .flatMap(new Func1<BleAdapterState, Observable<BluetoothGatt>>() {
                                    @Override
                                    public Observable<BluetoothGatt> call(BleAdapterState bleAdapterState) {
                                        return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                                    }
                                })
                )
                        .first()
                        .flatMap(new Func1<BluetoothGatt, Observable<RxBleConnection>>() {
                            @Override
                            public Observable<RxBleConnection> call(BluetoothGatt bluetoothGatt) {
                                return RxBleConnectionConnectorImpl.this.emitConnectionWithoutCompleting(gattCallback, bluetoothGatt);
                            }
                        })
                        .mergeWith(gattCallback.<RxBleConnection>observeDisconnect())
                        .doOnUnsubscribe(new Action0() {
                            @Override
                            public void call() {
                                RxBleConnectionConnectorImpl.this.enqueueDisconnectOperation(operationsPair.disconnect);
                            }
                        });
            }
        });
    }

    private Observable<RxBleConnection> emitConnectionWithoutCompleting(RxBleGattCallback gattCallback, BluetoothGatt bluetoothGatt) {
        return justOnNext(new RxBleConnectionImpl(rxBleRadio, gattCallback, bluetoothGatt)).cast(RxBleConnection.class);
    }

    private Subscription enqueueDisconnectOperation(RxBleRadioOperationDisconnect operationDisconnect) {
        return rxBleRadio
                .queue(operationDisconnect)
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }

}
