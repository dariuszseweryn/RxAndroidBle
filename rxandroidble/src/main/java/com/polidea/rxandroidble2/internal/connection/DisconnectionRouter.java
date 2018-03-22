package com.polidea.rxandroidble2.internal.connection;


import android.util.Log;

import com.polidea.rxandroidble2.RxBleAdapterStateObservable;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.internal.DeviceModule;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import java.util.LinkedList;
import java.util.Queue;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter implements DisconnectionRouterInput, DisconnectionRouterOutput {

    private static final String TAG = "DisconnectionRouter";
    private final Queue<ObservableEmitter<BleException>> exceptionEmitters = new LinkedList<>();
    private BleException exceptionOccurred;
    private Disposable adapterMonitoringDisposable;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            final RxBleAdapterWrapper adapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
    ) {
        /*
         The below .subscribe() is only to make the above .cache() to start working as soon as possible.
         We are not tracking the resulting `Subscription`. This is because of the contract of this class which is supposed to be called
         when a disconnection happens from one of three places:
            1. adapterStateObservable: the adapter turning into state other than STATE_ON
            2. onDisconnectedException
            3. onGattConnectionStateException
         One of those events must happen eventually. Then the adapterStateObservable (which uses BroadcastReceiver on a Context) will
         get unsubscribed. The rest of this chain lives only in the @ConnectionScope context and will get Garbage Collected eventually.
         */
        adapterMonitoringDisposable = awaitAdapterNotUsable(adapterWrapper, adapterStateObservable)
                .map(new Function<Boolean, BleException>() {
                    @Override
                    public BleException apply(Boolean isAdapterUsable) {
                        return new BleDisconnectedException(macAddress); // TODO: Introduce BleDisabledException?
                    }
                })
                .firstElement()
                .subscribe(new Consumer<BleException>() {
                    @Override
                    public void accept(BleException exception) throws Exception {
                        Log.d(TAG, "An exception received, indicating that the adapter has became unusable.");
                        exceptionOccurred = exception;
                        notifySubscribersAboutException();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.w(TAG, "Failed to monitor adapter state.", throwable);
                    }
                });
    }

    private static Observable<Boolean> awaitAdapterNotUsable(RxBleAdapterWrapper adapterWrapper,
                                                             Observable<RxBleAdapterStateObservable.BleAdapterState> stateChanges) {
        return stateChanges
                .map(new Function<RxBleAdapterStateObservable.BleAdapterState, Boolean>() {
                    @Override
                    public Boolean apply(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                        return bleAdapterState.isUsable();
                    }
                })
                .startWith(adapterWrapper.isBluetoothEnabled())
                .filter(new Predicate<Boolean>() {
                    @Override
                    public boolean test(Boolean isAdapterUsable) {
                        return !isAdapterUsable;
                    }
                });
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDisconnectedException(BleDisconnectedException disconnectedException) {
        onExceptionOccurred(disconnectedException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onGattConnectionStateException(BleGattException disconnectedGattException) {
        onExceptionOccurred(disconnectedGattException);
    }

    private void onExceptionOccurred(BleException exception) {
        if (exceptionOccurred == null) {
            exceptionOccurred = exception;
            notifySubscribersAboutException();
        }
    }

    private void notifySubscribersAboutException() {
        if (adapterMonitoringDisposable != null) {
            adapterMonitoringDisposable.dispose();
        }

        while (!exceptionEmitters.isEmpty()) {
            final ObservableEmitter<BleException> exceptionEmitter = exceptionEmitters.poll();
            exceptionEmitter.onNext(exceptionOccurred);
            exceptionEmitter.onComplete();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public Observable<BleException> asValueOnlyObservable() {
        return Observable.create(new ObservableOnSubscribe<BleException>() {
            @Override
            public void subscribe(final ObservableEmitter<BleException> emitter) throws Exception {
                if (exceptionOccurred != null) {
                    emitter.onNext(exceptionOccurred);
                    emitter.onComplete();
                } else {
                    storeEmitterToBeNotifiedInTheFuture(emitter);
                }
            }
        });
    }

    private void storeEmitterToBeNotifiedInTheFuture(final ObservableEmitter<BleException> emitter) {
        exceptionEmitters.add(emitter);
        emitter.setCancellable(new Cancellable() {
            @Override
            public void cancel() throws Exception {
                exceptionEmitters.remove(emitter);
            }
        });
    }

    /**
     * @inheritDoc
     */
    @Override
    public <T> Observable<T> asErrorOnlyObservable() {
        return asValueOnlyObservable()
                .flatMap(new Function<BleException, Observable<T>>() {
                    @Override
                    public Observable<T> apply(BleException e) {
                        return Observable.error(e);
                    }
                });
    }
}
