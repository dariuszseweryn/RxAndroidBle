package com.polidea.rxandroidble2.internal.connection;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.internal.operations.OperationsProvider;
import com.polidea.rxandroidble2.internal.operations.ServiceDiscoveryOperation;
import com.polidea.rxandroidble2.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble2.internal.serialization.ConnectionOperationQueue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

@ConnectionScope
class ServiceDiscoveryManager {

    private final ConnectionOperationQueue operationQueue;
    private final BluetoothGatt bluetoothGatt;
    private final OperationsProvider operationProvider;
    private Single<RxBleDeviceServices> deviceServicesObservable;
    private Subject<TimeoutConfiguration> timeoutBehaviorSubject = BehaviorSubject.<TimeoutConfiguration>create().toSerialized();
    private boolean hasCachedResults = false;

    @Inject
    ServiceDiscoveryManager(ConnectionOperationQueue operationQueue, BluetoothGatt bluetoothGatt, OperationsProvider operationProvider) {
        this.operationQueue = operationQueue;
        this.bluetoothGatt = bluetoothGatt;
        this.operationProvider = operationProvider;
        reset();
    }

    Single<RxBleDeviceServices> getDiscoverServicesSingle(final long timeout, final TimeUnit timeoutTimeUnit) {
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
        this.deviceServicesObservable = getListOfServicesFromGatt()
                .map(wrapIntoRxBleDeviceServices())
                .switchIfEmpty(getTimeoutConfiguration().flatMap(scheduleActualDiscoveryWithTimeout()))
                .doOnSuccess(Functions.actionConsumer(new Action() {
                    @Override
                    public void run() throws Exception {
                        hasCachedResults = true;
                    }
                }))
                .doOnError(Functions.actionConsumer(new Action() {
                    @Override
                    public void run() {
                        reset();
                    }
                }))
                .cache();
    }

    @NonNull
    private Function<List<BluetoothGattService>, RxBleDeviceServices> wrapIntoRxBleDeviceServices() {
        return new Function<List<BluetoothGattService>, RxBleDeviceServices>() {
            @Override
            public RxBleDeviceServices apply(List<BluetoothGattService> bluetoothGattServices) {
                return new RxBleDeviceServices(bluetoothGattServices);
            }
        };
    }

    private Maybe<List<BluetoothGattService>> getListOfServicesFromGatt() {
        return Single.fromCallable(new Callable<List<BluetoothGattService>>() {
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
                });
    }

    @NonNull
    private Single<TimeoutConfiguration> getTimeoutConfiguration() {
        return timeoutBehaviorSubject.firstOrError();
    }

    @NonNull
    private Function<TimeoutConfiguration, Single<RxBleDeviceServices>> scheduleActualDiscoveryWithTimeout() {
        return new Function<TimeoutConfiguration, Single<RxBleDeviceServices>>() {
            @Override
            public Single<RxBleDeviceServices> apply(TimeoutConfiguration timeoutConf) {
                final ServiceDiscoveryOperation operation = operationProvider
                        .provideServiceDiscoveryOperation(timeoutConf.timeout, timeoutConf.timeoutTimeUnit);
                return operationQueue.queue(operation)
                        .firstOrError();
            }
        };
    }
}
