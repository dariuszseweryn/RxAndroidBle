package com.polidea.rxandroidble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleInternalScanResultV21;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScanV21;
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
        final RxBleRadioImpl rxBleRadio = new RxBleRadioImpl();
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
        return scanBleDevices(filterServiceUUIDs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Observable<RxBleScanResult> scanBleDevices(@NonNull ScanSettings settings, @Nullable UUID... filterServiceUUIDs) {
        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE));
        } else if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
            return Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED));
        } else if (!locationServicesStatus.isLocationPermissionOk()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_PERMISSION_MISSING));
        } else if (!locationServicesStatus.isLocationProviderOk()) {
            return Observable.error(new BleScanException(BleScanException.LOCATION_SERVICES_DISABLED));
        } else {
            return initializeScan(settings, filterServiceUUIDs);
        }
    }

    private Observable<RxBleScanResult> initializeScan(ScanSettings settings, @Nullable UUID[] filterServiceUUIDs) {
        //TODO add settings to the key
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);

        synchronized (queuedScanOperations) {
            Observable<RxBleScanResult> matchingQueuedScan = queuedScanOperations.get(filteredUUIDs);

            if (matchingQueuedScan == null) {
                matchingQueuedScan = createScanOperation(settings, filterServiceUUIDs);
                queuedScanOperations.put(filteredUUIDs, matchingQueuedScan);
            }

            return matchingQueuedScan;
        }
    }

    private <T> Observable<T> bluetoothAdapterOffExceptionObservable() {
        return rxBleAdapterStateObservable
                .filter(state -> state != BleAdapterState.STATE_ON)
                .first()
                .flatMap(status -> Observable.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED)));
    }

    private Observable<RxBleScanResult> createScanOperation(ScanSettings settings, @Nullable UUID[] filterServiceUUIDs) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ? createScanOperationV18(filterServiceUUIDs)
                : createScanOperationV21(settings, filterServiceUUIDs);
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResult scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        final RxBleScanRecord scanRecord = uuidUtil.parseFromBytes(scanResult.getScanRecord());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanRecord);
    }

    private Observable<RxBleScanResult> createScanOperationV18(@Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
        final RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(filterServiceUUIDs, rxBleAdapterWrapper, uuidUtil);
        return rxBleRadio.queue(scanOperation)
                .doOnUnsubscribe(() -> {

                    synchronized (queuedScanOperations) {
                        scanOperation.stop();
                        queuedScanOperations.remove(filteredUUIDs);
                    }
                })
                .mergeWith(bluetoothAdapterOffExceptionObservable())
                .map(this::convertToPublicScanResult)
                .share();
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResultV21 scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanResult.getScanRecord());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Observable<RxBleScanResult> createScanOperationV21(ScanSettings settings, @Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
        final RxBleRadioOperationScanV21 scanOperation = new RxBleRadioOperationScanV21(settings, filterServiceUUIDs, rxBleAdapterWrapper);
        return rxBleRadio.queue(scanOperation)
                .doOnUnsubscribe(() -> {

                    synchronized (queuedScanOperations) {
                        scanOperation.stop();
                        queuedScanOperations.remove(filteredUUIDs);
                    }
                })
                .mergeWith(bluetoothAdapterOffExceptionObservable())
                .map(this::convertToPublicScanResult)
                .share();
    }

}
