package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble2.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble2.internal.operations.LegacyScanOperation;
import com.polidea.rxandroidble2.internal.operations.Operation;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResultLegacy;
import com.polidea.rxandroidble2.internal.scan.ScanPreconditionsVerifier;
import com.polidea.rxandroidble2.internal.scan.ScanSetup;
import com.polidea.rxandroidble2.internal.scan.ScanSetupBuilder;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;
import com.polidea.rxandroidble2.internal.util.ClientStateObservable;
import com.polidea.rxandroidble2.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.internal.util.UUIDUtil;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import bleshadow.dagger.Lazy;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

class RxBleClientImpl extends RxBleClient {

    private final ClientOperationQueue operationQueue;
    private final UUIDUtil uuidUtil;
    private final RxBleDeviceProvider rxBleDeviceProvider;
    private final ScanSetupBuilder scanSetupBuilder;
    private final ScanPreconditionsVerifier scanPreconditionVerifier;
    private final Function<RxBleInternalScanResult, ScanResult> internalToExternalScanResultMapFunction;
    private final ClientComponent.ClientComponentFinalizer clientComponentFinalizer;
    private final Scheduler bluetoothInteractionScheduler;
    private final Map<Set<UUID>, Observable<RxBleScanResult>> queuedScanOperations = new HashMap<>();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> rxBleAdapterStateObservable;
    private final LocationServicesStatus locationServicesStatus;
    private final Lazy<ClientStateObservable> lazyClientStateObservable;

    @Inject
    RxBleClientImpl(RxBleAdapterWrapper rxBleAdapterWrapper,
                    ClientOperationQueue operationQueue,
                    Observable<BleAdapterState> adapterStateObservable,
                    UUIDUtil uuidUtil,
                    LocationServicesStatus locationServicesStatus,
                    Lazy<ClientStateObservable> lazyClientStateObservable,
                    RxBleDeviceProvider rxBleDeviceProvider,
                    ScanSetupBuilder scanSetupBuilder,
                    ScanPreconditionsVerifier scanPreconditionVerifier,
                    Function<RxBleInternalScanResult, ScanResult> internalToExternalScanResultMapFunction,
                    @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
                    ClientComponent.ClientComponentFinalizer clientComponentFinalizer) {
        this.uuidUtil = uuidUtil;
        this.operationQueue = operationQueue;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.rxBleAdapterStateObservable = adapterStateObservable;
        this.locationServicesStatus = locationServicesStatus;
        this.lazyClientStateObservable = lazyClientStateObservable;
        this.rxBleDeviceProvider = rxBleDeviceProvider;
        this.scanSetupBuilder = scanSetupBuilder;
        this.scanPreconditionVerifier = scanPreconditionVerifier;
        this.internalToExternalScanResultMapFunction = internalToExternalScanResultMapFunction;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.clientComponentFinalizer = clientComponentFinalizer;
    }

    @Override
    protected void finalize() throws Throwable {
        clientComponentFinalizer.onFinalize();
        super.finalize();
    }

    @Override
    public RxBleDevice getBleDevice(@NonNull String macAddress) {
        guardBluetoothAdapterAvailable();
        return rxBleDeviceProvider.getBleDevice(macAddress);
    }

    @Override
    public Set<RxBleDevice> getBondedDevices() {
        guardBluetoothAdapterAvailable();
        Set<RxBleDevice> rxBleDevices = new HashSet<>();
        Set<BluetoothDevice> bluetoothDevices = rxBleAdapterWrapper.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            rxBleDevices.add(getBleDevice(bluetoothDevice.getAddress()));
        }

        return rxBleDevices;
    }

    @Override
    public Observable<ScanResult> scanBleDevices(final ScanSettings scanSettings, final ScanFilter... scanFilters) {
        return Observable.defer(new Callable<ObservableSource<? extends ScanResult>>() {
            @Override
            public Observable<ScanResult> call() {
                scanPreconditionVerifier.verify();
                final ScanSetup scanSetup = scanSetupBuilder.build(scanSettings, scanFilters);
                final Operation<RxBleInternalScanResult> scanOperation = scanSetup.scanOperation;
                return operationQueue.queue(scanOperation)
                        .unsubscribeOn(bluetoothInteractionScheduler)
                        .compose(scanSetup.scanOperationBehaviourEmulatorTransformer)
                        .map(internalToExternalScanResultMapFunction)
                        .mergeWith(RxBleClientImpl.this.<ScanResult>bluetoothAdapterOffExceptionObservable());
            }
        });
    }


    public Observable<RxBleScanResult> scanBleDevices(@Nullable final UUID... filterServiceUUIDs) {
        return Observable.defer(new Callable<ObservableSource<? extends RxBleScanResult>>() {
            @Override
            public ObservableSource<? extends RxBleScanResult> call() throws Exception {
                scanPreconditionVerifier.verify();
                return initializeScan(filterServiceUUIDs);
            }
        });
    }

    private Observable<RxBleScanResult> initializeScan(@Nullable UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);

        synchronized (queuedScanOperations) {
            Observable<RxBleScanResult> matchingQueuedScan = queuedScanOperations.get(filteredUUIDs);

            if (matchingQueuedScan == null) {
                matchingQueuedScan = createScanOperationApi18(filterServiceUUIDs);
                queuedScanOperations.put(filteredUUIDs, matchingQueuedScan);
            }

            return matchingQueuedScan;
        }
    }

    /**
     * This {@link Observable} will not emit values by design. It may only emit {@link BleScanException} if
     * bluetooth adapter is turned down.
     */
    private <T> Observable<T> bluetoothAdapterOffExceptionObservable() {
        return rxBleAdapterStateObservable
                .filter(new Predicate<BleAdapterState>() {
                    @Override
                    public boolean test(BleAdapterState state) throws Exception {
                        return state != BleAdapterState.STATE_ON;
                    }
                })
                .firstElement()
                .flatMap(new Function<BleAdapterState, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(BleAdapterState bleAdapterState) throws Exception {
                        return Maybe.error(new BleScanException(BleScanException.BLUETOOTH_DISABLED));
                    }
                })
                .toObservable();
    }

    private RxBleScanResult convertToPublicScanResult(RxBleInternalScanResultLegacy scanResult) {
        final BluetoothDevice bluetoothDevice = scanResult.getBluetoothDevice();
        final RxBleDevice bleDevice = getBleDevice(bluetoothDevice.getAddress());
        return new RxBleScanResult(bleDevice, scanResult.getRssi(), scanResult.getScanRecord());
    }

    private Observable<RxBleScanResult> createScanOperationApi18(@Nullable final UUID[] filterServiceUUIDs) {
        final Set<UUID> filteredUUIDs = uuidUtil.toDistinctSet(filterServiceUUIDs);
        final LegacyScanOperation
                scanOperation = new LegacyScanOperation(filterServiceUUIDs, rxBleAdapterWrapper, uuidUtil);
        return operationQueue.queue(scanOperation)
                .doFinally(new Action() {
                    @Override
                    public void run() throws Exception {
                        synchronized (queuedScanOperations) {
                            queuedScanOperations.remove(filteredUUIDs);
                        }
                    }
                })
                .mergeWith(this.<RxBleInternalScanResultLegacy>bluetoothAdapterOffExceptionObservable())
                .map(new Function<RxBleInternalScanResultLegacy, RxBleScanResult>() {
                    @Override
                    public RxBleScanResult apply(RxBleInternalScanResultLegacy scanResult) {
                        return convertToPublicScanResult(scanResult);
                    }
                })
                .share();
    }

    private void guardBluetoothAdapterAvailable() {
        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            throw new UnsupportedOperationException("RxAndroidBle library needs a BluetoothAdapter to be available in the system to work."
            + " If this is a test on an emulator then you can use 'https://github.com/Polidea/RxAndroidBle/tree/master/mockrxandroidble'");
        }
    }

    @Override
    public Observable<State> observeStateChanges() {
        return lazyClientStateObservable.get();
    }

    @Override
    public State getState() {
        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            return State.BLUETOOTH_NOT_AVAILABLE;
        }
        if (!locationServicesStatus.isLocationPermissionOk()) {
            return State.LOCATION_PERMISSION_NOT_GRANTED;
        }
        if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
            return State.BLUETOOTH_NOT_ENABLED;
        }
        if (!locationServicesStatus.isLocationProviderOk()) {
            return State.LOCATION_SERVICES_NOT_ENABLED;
        } else {
            return State.READY;
        }
    }
}
