package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.exceptions.BleScanException;
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

public class RxBleClientImpl implements RxBleClient {

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private final RxBleRadio rxBleRadio = new RxBleRadioImpl();
    private final HashMap<String, RxBleDevice> availableDevices = new HashMap<>(); // TODO: clean? don't cache?
    private final UUIDParser uuidParser = new UUIDParser();
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final Context context;

    public RxBleClientImpl(Context context) {
        this.context = context;
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
            final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, bluetoothAdapter,
                    rxBleRadio, uuidParser);

            final Observable<RxBleScanResult> scanResultObservable = rxBleRadio.queue(scanOperation)
                    .doOnUnsubscribe(() -> {
                        scanOperation.stop();
                        queuedScanOperations.remove(filteredUUIDs);
                    })
                    .mergeWith(bluetoothAdapterOffExceptionObservable())
                    .share();
            queuedScanOperations.put(filteredUUIDs, scanResultObservable);
            return scanResultObservable;
        });
    }

    @NonNull
    private Set<UUID> toSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }

    @Override
    public RxBleDevice getBleDevice(String bluetoothAddress) {
        final RxBleDevice rxBleDevice = availableDevices.get(bluetoothAddress);
        if (rxBleDevice != null) {
            return rxBleDevice;
        }

        final RxBleDeviceImpl newRxBleDevice = new RxBleDeviceImpl(bluetoothAdapter.getRemoteDevice(bluetoothAddress), rxBleRadio);
        availableDevices.put(bluetoothAddress, newRxBleDevice);
        return newRxBleDevice;
    }
}
