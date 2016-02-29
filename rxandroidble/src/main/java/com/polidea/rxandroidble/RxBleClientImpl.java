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
import com.polidea.rxandroidble.internal.UUIDUtil;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import rx.Observable;

class RxBleClientImpl extends RxBleClient {

    private static RxBleClientImpl CLIENT_INSTANCE;
    private final BluetoothAdapter bluetoothAdapter;
    private final RxBleRadio rxBleRadio;
    private final UUIDUtil uuidUtil;
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
        uuidUtil = new UUIDUtil();
        rxBleRadio = new RxBleRadioImpl();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        rxBleDeviceProvider = new RxBleDeviceProvider(bluetoothAdapter, rxBleRadio);
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {
        return getMatchingQueuedScan(filterServiceUUIDs)
                .switchIfEmpty(createScanOperation(filterServiceUUIDs));
    }

    private Observable<RxBleScanResult> getMatchingQueuedScan(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.from(uuidUtil.toDistinctSet(filterServiceUUIDs))
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
    private Observable<RxBleScanResult> createScanOperation(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.defer(() -> {
            final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
            final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, bluetoothAdapter, uuidUtil);

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

    @Override
    public RxBleDevice getBleDevice(@NonNull String macAddress) {
        return rxBleDeviceProvider.getBleDevice(macAddress);
    }
}
