package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioImpl;
import com.polidea.rxandroidble.internal.UUIDParser;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import rx.Observable;

class RxBleClientImpl extends RxBleClient {

    private static RxBleClientImpl CLIENT_INSTANCE;
    private final BluetoothAdapter bluetoothAdapter;
    private final RxBleRadio rxBleRadio;
    private final UUIDParser uuidParser;
    private final RxBleDeviceProvider rxBleDeviceProvider;
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final Context context;

    public static RxBleClientImpl getInstance(Context context) {

        if (CLIENT_INSTANCE == null) {

            synchronized (RxBleClient.class) {

                if (CLIENT_INSTANCE == null) {
                    CLIENT_INSTANCE = new RxBleClientImpl(context.getApplicationContext());
                }
            }
        }

        return CLIENT_INSTANCE;
    }

    public RxBleClientImpl(Context context) {
        this.context = context;
        uuidParser = new UUIDParser();
        rxBleRadio = new RxBleRadioImpl();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        rxBleDeviceProvider = new RxBleDeviceProvider(bluetoothAdapter, rxBleRadio);
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {
        return getMatchingQueuedScan(filterServiceUUIDs)
                .switchIfEmpty(startFreshScan(filterServiceUUIDs));
    }

    public Observable<RxBleScanResult> getMatchingQueuedScan(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.just(filterServiceUUIDs)
                .map(this::toSet)
                .map(queuedScanOperations::get)
                .filter(rxBleScanResultObservable -> rxBleScanResultObservable != null)
                .flatMap(rxBleScanResultObservable -> rxBleScanResultObservable);
    }

    private <T> Observable<T> bluetoothAdapterOffExceptionObservable() {
        return new RxBleAdapterStateObservable(context)
                .filter(state -> state != BleAdapterState.STATE_ON)
                .first()
                .flatMap(status -> Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED)));
    }

    @NonNull
    private Observable<RxBleScanResult> startFreshScan(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.defer(() -> {
            final Set<UUID> filteredUUIDs = toSet(filterServiceUUIDs);
            final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, bluetoothAdapter, uuidParser);

            final Observable<RxBleScanResult> scanResultObservable = rxBleRadio.queue(scanOperation)
                    .doOnUnsubscribe(() -> {
                        scanOperation.stop();
                        queuedScanOperations.remove(filteredUUIDs);
                    })
                    .mergeWith(bluetoothAdapterOffExceptionObservable())
                    .map(this::convertToPublicScanResult)
                    .share();
            queuedScanOperations.put(filteredUUIDs, scanResultObservable);
            return scanResultObservable;
        });
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResult scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanResult.getScanRecord());
    }

    @NonNull
    private Set<UUID> toSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }

    @Override
    public RxBleDevice getBleDevice(String macAddress) {
        return rxBleDeviceProvider.getBleDevice(macAddress);
    }
}
