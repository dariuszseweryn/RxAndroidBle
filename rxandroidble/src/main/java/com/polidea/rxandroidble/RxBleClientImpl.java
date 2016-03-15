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
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;
import com.polidea.rxandroidble.internal.radio.RxBleRadioImpl;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import rx.Observable;

class RxBleClientImpl extends RxBleClient {

    private static RxBleClientImpl CLIENT_INSTANCE;
    private final RxBleRadio rxBleRadio;
    private final UUIDUtil uuidUtil;
    private final RxBleDeviceProvider rxBleDeviceProvider;
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> rxBleAdapterStateObservable;

    public static RxBleClientImpl getInstance(@NonNull Context context) {

        if (CLIENT_INSTANCE == null) {

            synchronized (RxBleClient.class) {

                if (CLIENT_INSTANCE == null) {
                    final Context applicationContext = context.getApplicationContext();
                    CLIENT_INSTANCE = new RxBleClientImpl(
                            new RxBleAdapterWrapper(BluetoothAdapter.getDefaultAdapter()),
                            new RxBleRadioImpl(),
                            new RxBleAdapterStateObservable(applicationContext),
                            new UUIDUtil(),
                            new BleConnectionCompat(context));
                }
            }
        }

        return CLIENT_INSTANCE;
    }

    public RxBleClientImpl(RxBleAdapterWrapper rxBleAdapterWrapper, RxBleRadio rxBleRadio,
                           Observable<BleAdapterState> adapterStateObservable, UUIDUtil uuidUtil, BleConnectionCompat bleConnectionCompat) {
        this.uuidUtil = uuidUtil;
        this.rxBleRadio = rxBleRadio;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        rxBleDeviceProvider = new RxBleDeviceProvider(this.rxBleAdapterWrapper, this.rxBleRadio, bleConnectionCompat);
        rxBleAdapterStateObservable = adapterStateObservable;
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {

        if (rxBleAdapterWrapper.hasBluetoothAdapter()) {
            return getMatchingQueuedScan(filterServiceUUIDs)
                    .switchIfEmpty(createScanOperation(filterServiceUUIDs));
        } else {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE));
        }
    }

    private Observable<RxBleScanResult> getMatchingQueuedScan(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.just(filterServiceUUIDs)
                .map(uuidUtil::toDistinctSet)
                .map(queuedScanOperations::get)
                .filter(rxBleScanResultObservable -> rxBleScanResultObservable != null)
                .flatMap(rxBleScanResultObservable -> rxBleScanResultObservable);
    }

    private <T> Observable<T> bluetoothAdapterOffExceptionObservable() {
        return rxBleAdapterStateObservable
                .filter(state -> state != BleAdapterState.STATE_ON)
                .first()
                .flatMap(status -> Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED)));
    }

    @NonNull
    private Observable<RxBleScanResult> createScanOperation(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.defer(() -> {
            final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
            final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, rxBleAdapterWrapper, uuidUtil);

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
