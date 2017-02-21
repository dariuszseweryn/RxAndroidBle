package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.LocationManager;
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
import com.polidea.rxandroidble.internal.util.CheckerLocationPermission;
import com.polidea.rxandroidble.internal.util.CheckerLocationProvider;
import com.polidea.rxandroidble.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble.internal.util.ProviderApplicationTargetSdk;
import com.polidea.rxandroidble.internal.util.ProviderDeviceSdk;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

class RxBleClientImpl extends RxBleClient {

    private final RxBleRadio rxBleRadio;
    private final UUIDUtil uuidUtil;
    private final RxBleDeviceProvider rxBleDeviceProvider;
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> rxBleAdapterStateObservable;
    private final LocationServicesStatus locationServicesStatus;

    RxBleClientImpl(RxBleAdapterWrapper rxBleAdapterWrapper,
                    RxBleRadio rxBleRadio,
                    Observable<BleAdapterState> adapterStateObservable,
                    UUIDUtil uuidUtil,
                    LocationServicesStatus locationServicesStatus,
                    RxBleDeviceProvider rxBleDeviceProvider) {
        this.uuidUtil = uuidUtil;
        this.rxBleRadio = rxBleRadio;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.rxBleAdapterStateObservable = adapterStateObservable;
        this.locationServicesStatus = locationServicesStatus;
        this.rxBleDeviceProvider = rxBleDeviceProvider;
    }

    public static RxBleClientImpl getInstance(@NonNull Context context) {
        final Context applicationContext = context.getApplicationContext();
        final RxBleAdapterWrapper rxBleAdapterWrapper = new RxBleAdapterWrapper(BluetoothAdapter.getDefaultAdapter());
        final RxBleRadioImpl rxBleRadio = new RxBleRadioImpl(getRxBleRadioScheduler());
        final RxBleAdapterStateObservable adapterStateObservable = new RxBleAdapterStateObservable(applicationContext);
        final BleConnectionCompat bleConnectionCompat = new BleConnectionCompat(context);
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Scheduler gattCallbacksProcessingScheduler = Schedulers.from(executor);
        final LocationManager locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        final CheckerLocationPermission checkerLocationPermission = new CheckerLocationPermission(applicationContext);
        final CheckerLocationProvider checkerLocationProvider = new CheckerLocationProvider(locationManager);
        final ProviderApplicationTargetSdk providerApplicationTargetSdk = new ProviderApplicationTargetSdk(applicationContext);
        final ProviderDeviceSdk providerDeviceSdk = new ProviderDeviceSdk();
        return new RxBleClientImpl(
                rxBleAdapterWrapper,
                rxBleRadio,
                adapterStateObservable,
                new UUIDUtil(),
                new LocationServicesStatus(
                        checkerLocationProvider,
                        checkerLocationPermission,
                        providerDeviceSdk,
                        providerApplicationTargetSdk
                ),
                new RxBleDeviceProvider(
                        rxBleAdapterWrapper,
                        rxBleRadio,
                        bleConnectionCompat,
                        adapterStateObservable,
                        gattCallbacksProcessingScheduler
                )
        ) {
            @Override
            protected void finalize() throws Throwable {
                super.finalize();
                executor.shutdown();
            }
        };
    }

    @Override
    public RxBleDevice getBleDevice(@NonNull String macAddress) {
        return rxBleDeviceProvider.getBleDevice(macAddress);
    }

    @Override
    public Set<RxBleDevice> getBondedDevices() {
        Set<RxBleDevice> rxBleDevices = new HashSet<>();
        Set<BluetoothDevice> bluetoothDevices = rxBleAdapterWrapper.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            rxBleDevices.add(getBleDevice(bluetoothDevice.getAddress()));
        }

        return rxBleDevices;
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID... filterServiceUUIDs) {

        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE));
        } else if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED));
        } else if (!locationServicesStatus.isLocationPermissionOk()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_PERMISSION_MISSING));
        } else if (!locationServicesStatus.isLocationProviderOk()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_SERVICES_DISABLED));
        } else {
            return initializeScan(filterServiceUUIDs);
        }
    }

    private Observable<RxBleScanResult> initializeScan(@Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);

        synchronized (queuedScanOperations) {
            Observable<RxBleScanResult> matchingQueuedScan = queuedScanOperations.get(filteredUUIDs);

            if (matchingQueuedScan == null) {
                matchingQueuedScan = createScanOperation(filterServiceUUIDs);
                queuedScanOperations.put(filteredUUIDs, matchingQueuedScan);
            }

            return matchingQueuedScan;
        }
    }

    private Observable<RxBleInternalScanResult> bluetoothAdapterOffExceptionObservable() {
        return rxBleAdapterStateObservable
                .filter(new Func1<BleAdapterState, Boolean>() {
                    @Override
                    public Boolean call(BleAdapterState state) {
                        return state != BleAdapterState.STATE_ON;
                    }
                })
                .first()
                .flatMap(new Func1<BleAdapterState, Observable<? extends RxBleInternalScanResult>>() {
                    @Override
                    public Observable<? extends RxBleInternalScanResult> call(BleAdapterState status) {
                        return Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED));
                    }
                });
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResult scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanResult.getScanRecord());
    }

    private Observable<RxBleScanResult> createScanOperation(@Nullable final UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
        final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, rxBleAdapterWrapper, uuidUtil);
        return rxBleRadio.queue(scanOperation)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {

                        synchronized (queuedScanOperations) {
                            scanOperation.stop();
                            queuedScanOperations.remove(filteredUUIDs);
                        }
                    }
                })
                .mergeWith(bluetoothAdapterOffExceptionObservable())
                .map(new Func1<RxBleInternalScanResult, RxBleScanResult>() {
                    @Override
                    public RxBleScanResult call(RxBleInternalScanResult scanResult) {
                        return RxBleClientImpl.this.convertToPublicScanResult(scanResult);
                    }
                })
                .share();
    }

    /**
     * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
     * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
     * on the main thread.
     */
    private static Scheduler getRxBleRadioScheduler() {
        return AndroidSchedulers.mainThread();
    }
}
