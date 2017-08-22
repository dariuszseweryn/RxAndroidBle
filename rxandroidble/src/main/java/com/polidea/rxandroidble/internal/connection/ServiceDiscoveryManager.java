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
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.SerializedSubject;

@ConnectionScope
class ServiceDiscoveryManager {

    private final ConnectionOperationQueue operationQueue;
    private final BluetoothGatt bluetoothGatt;
    private final OperationsProvider operationProvider;
    private Observable<RxBleDeviceServices> deviceServicesObservable;
    private SerializedSubject<TimeoutConfiguration, TimeoutConfiguration> timeoutBehaviorSubject
            = BehaviorSubject.<TimeoutConfiguration>create().toSerialized();
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
            return deviceServicesObservable.doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    timeoutBehaviorSubject.onNext(new TimeoutConfiguration(timeout, timeoutTimeUnit, Schedulers.computation()));
                }
            });
        }
    }

    private void reset() {
        hasCachedResults = false;
        this.deviceServicesObservable = Observable.fromCallable(new Func0<List<BluetoothGattService>>() {
            @Override
            public List<BluetoothGattService> call() {
                return bluetoothGatt.getServices();
            }
        })
                .filter(new Func1<List<BluetoothGattService>, Boolean>() {
                    @Override
                    public Boolean call(List<BluetoothGattService> bluetoothGattServices) {
                        return bluetoothGattServices.size() > 0;
                    }
                })
                .map(new Func1<List<BluetoothGattService>, RxBleDeviceServices>() {
                    @Override
                    public RxBleDeviceServices call(List<BluetoothGattService> bluetoothGattServices) {
                        return new RxBleDeviceServices(bluetoothGattServices);
                    }
                })
                .switchIfEmpty(getTimeoutConfiguration().flatMap(scheduleActualDiscoveryWithTimeout()))
                .doOnNext(new Action1<RxBleDeviceServices>() {
                    @Override
                    public void call(RxBleDeviceServices rxBleDeviceServices) {
                        hasCachedResults = true;
                    }
                })
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
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
    private Func1<TimeoutConfiguration, Observable<RxBleDeviceServices>> scheduleActualDiscoveryWithTimeout() {
        return new Func1<TimeoutConfiguration, Observable<RxBleDeviceServices>>() {
            @Override
            public Observable<RxBleDeviceServices> call(TimeoutConfiguration timeoutConf) {
                final ServiceDiscoveryOperation operation = operationProvider
                        .provideServiceDiscoveryOperation(timeoutConf.timeout, timeoutConf.timeoutTimeUnit);
                return operationQueue.queue(operation);
            }
        };
    }
}
