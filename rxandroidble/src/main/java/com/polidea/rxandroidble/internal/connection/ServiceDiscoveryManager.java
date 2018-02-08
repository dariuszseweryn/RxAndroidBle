package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.operations.ServiceDiscoveryOperation;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@ConnectionScope
class ServiceDiscoveryManager {

    private final ConnectionOperationQueue operationQueue;
    private final BluetoothGatt bluetoothGatt;
    private final OperationsProvider operationProvider;
    private Observable<RxBleDeviceServices> deviceServicesObservable;
    private Subject<TimeoutConfiguration> timeoutBehaviorSubject = BehaviorSubject.<TimeoutConfiguration>create().toSerialized();
    private boolean hasCachedResults = false;

    @Inject
    ServiceDiscoveryManager(ConnectionOperationQueue operationQueue, BluetoothGatt bluetoothGatt, OperationsProvider operationProvider) {
        this.operationQueue = operationQueue;
        this.bluetoothGatt = bluetoothGatt;
        this.operationProvider = operationProvider;
        reset();
    }

    Observable<RxBleDeviceServices> getDiscoverServicesObservable(final long timeout, final TimeUnit timeoutTimeUnit) {
        if (hasCachedResults) {
            // optimisation to decrease the number of allocations
            return deviceServicesObservable;
        } else {
            return deviceServicesObservable.doOnSubscribe(
                    new Consumer<Disposable>() {
                        @Override
                        public void accept(Disposable disposable) throws Exception {
                            timeoutBehaviorSubject.onNext(new TimeoutConfiguration(timeout, timeoutTimeUnit, Schedulers.computation()));
                        }
                    });
        }
    }

    private void reset() {
        hasCachedResults = false;
        this.deviceServicesObservable = Observable.fromCallable(new Callable<List<BluetoothGattService>>() {
            @Override
            public List<BluetoothGattService> call() {
                return bluetoothGatt.getServices();
            }
        })
                .filter(new Predicate<List<BluetoothGattService>>() {
                    @Override
                    public boolean test(List<BluetoothGattService> bluetoothGattServices) {
                        return bluetoothGattServices.size() > 0;
                    }
                })
                .map(new Function<List<BluetoothGattService>, RxBleDeviceServices>() {
                    @Override
                    public RxBleDeviceServices apply(List<BluetoothGattService> bluetoothGattServices) {
                        return new RxBleDeviceServices(bluetoothGattServices);
                    }
                })
                .switchIfEmpty(getTimeoutConfiguration().flatMap(scheduleActualDiscoveryWithTimeout()))
                .doOnNext(new Consumer<RxBleDeviceServices>() {
                    @Override
                    public void accept(RxBleDeviceServices rxBleDeviceServices) {
                        hasCachedResults = true;
                    }
                })
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        reset();
                    }
                })
                .cacheWithInitialCapacity(1);
    }

    @NonNull
    private Observable<TimeoutConfiguration> getTimeoutConfiguration() {
        return timeoutBehaviorSubject.take(1);
    }

    @NonNull
    private Function<TimeoutConfiguration, Observable<RxBleDeviceServices>> scheduleActualDiscoveryWithTimeout() {
        return new Function<TimeoutConfiguration, Observable<RxBleDeviceServices>>() {
            @Override
            public Observable<RxBleDeviceServices> apply(TimeoutConfiguration timeoutConf) {
                final ServiceDiscoveryOperation operation = operationProvider
                        .provideServiceDiscoveryOperation(timeoutConf.timeout, timeoutConf.timeoutTimeUnit);
                return operationQueue.queue(operation);
            }
        };
    }
}
