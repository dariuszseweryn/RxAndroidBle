package com.polidea.rxandroidble.sample.example4_characteristic.advanced;


import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.NotificationSetupMode;
import com.polidea.rxandroidble.RxBleDevice;
import java.util.UUID;
import rx.Observable;

/**
 * Presenter class for {@link AdvancedCharacteristicOperationExampleActivity}. Prepares the logic for the activity using passed
 * {@link Observable}s. Contains only static methods to show a purely reactive (stateless) approach.
 * <p>
 * The contract of the class is that it subscribes to the passed Observables only if a specific functionality is possible to use.
 */
final class Presenter {

    private static UUID clientCharacteristicConfigDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Presenter() {
        // not instantiable
    }

    static Observable<PresenterEvent> prepareActivityLogic(
            final RxBleDevice rxBleDevice,
            final UUID characteristicUuid,
            final Observable<Boolean> connectClicks,
            final Observable<Boolean> connectingClicks, // used to disconnect the device even before connection is established
            final Observable<Boolean> disconnectClicks,
            final Observable<Boolean> readClicks,
            final Observable<byte[]> writeClicks,
            final Observable<Boolean> enableNotifyClicks,
            // used to disable notifications before they were enabled (but after enable click)
            final Observable<Boolean> enablingNotifyClicks,
            final Observable<Boolean> disableNotifyClicks,
            final Observable<Boolean> enableIndicateClicks,
            // used to disable indications before they were enabled (but after enable click)
            final Observable<Boolean> enablingIndicateClicks,
            final Observable<Boolean> disableIndicateClicks
    ) {

        return connectClicks.take(1) // subscribe to connectClicks and take one (unsubscribe after)
                .flatMap(connectClick -> rxBleDevice.establishConnection(false) // on click start connecting
                        .flatMap(
                                connection -> connection
                                        .discoverServices() // when connected discover services
                                        .flatMap(services -> services.getCharacteristic(characteristicUuid)), // and get characteristic
                                (connection, characteristic) -> { // when we have connection and characteristic

                                    final Observable<PresenterEvent> readObservable =
                                            !hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ)
                                                    // if the characteristic is not readable return an empty (dummy) observable
                                                    ? Observable.empty()
                                                    : readClicks // else use the readClicks observable from the activity
                                                    // every click is requesting a read operation from the peripheral
                                                    .flatMap(ignoredClick -> connection.readCharacteristic(characteristic))
                                                    .compose(transformToPresenterEvent(Type.READ)); // convenience method to wrap reads

                                    final Observable<PresenterEvent> writeObservable = // basically the same logic as in the reads
                                            !hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE)
                                                    ? Observable.empty()
                                                    : writeClicks // with exception that clicks emit byte[] to write
                                                    .flatMap(bytes -> connection.writeCharacteristic(characteristic, bytes))
                                                    .compose(transformToPresenterEvent(Type.WRITE));

                                    // checking if characteristic will potentially need a compatibility mode notifications
                                    final NotificationSetupMode notificationSetupMode =
                                            characteristic.getDescriptor(clientCharacteristicConfigDescriptorUuid) == null
                                                    ? NotificationSetupMode.COMPAT
                                                    : NotificationSetupMode.DEFAULT;
                                    /*
                                     * wrapping observables for notifications and indications so they will emit FALSE and TRUE respectively.
                                     * this is needed because only one of them may be active at the same time and we need to differentiate
                                     * the clicks
                                     */
                                    final Observable<Boolean> enableNotifyClicksObservable = !hasProperty(characteristic, PROPERTY_NOTIFY)
                                            /*
                                             * if property for notifications is not available return Observable.never() dummy observable.
                                             * Observable.never() is needed because of the Observable.amb() below which repeats
                                             * the behaviour of Observable that first emits or terminates and it will be checking both
                                             * notifyClicks and indicateClicks
                                             */
                                            ? Observable.never()
                                            // only the first click to enableNotifyClicks is taken to account
                                            : enableNotifyClicks.take(1).map(aBoolean -> Boolean.FALSE);
                                    final Observable<Boolean> enableIndicateClicksObservable =
                                            !hasProperty(characteristic, PROPERTY_INDICATE)
                                                    ? Observable.never()
                                                    : enableIndicateClicks.take(1).map(aBoolean -> Boolean.TRUE);

                                    // checking which notify or indicate will be clicked first the other is unsubscribed on click
                                    final Observable<PresenterEvent> notifyAndIndicateObservable = Observable.amb(
                                            enableNotifyClicksObservable,
                                            enableIndicateClicksObservable
                                    )
                                            .flatMap(isIndication -> {
                                                if (isIndication) { // if indication was clicked
                                                    return connection
                                                            // we setup indications
                                                            .setupIndication(characteristicUuid, notificationSetupMode)
                                                            // use a convenience transformer for tearing down the notifications
                                                            .compose(takeUntil(enablingIndicateClicks, disableIndicateClicks))
                                                            // and wrap the emissions with a convenience function
                                                            .compose(transformToNotificationPresenterEvent(Type.INDICATE));
                                                } else { // if notification was clicked
                                                    return connection
                                                            .setupNotification(characteristicUuid, notificationSetupMode)
                                                            .compose(takeUntil(enablingNotifyClicks, disableNotifyClicks))
                                                            .compose(transformToNotificationPresenterEvent(Type.NOTIFY));
                                                }
                                            })
                                            /*
                                             * whenever the notification or indication is finished (by the user or an error) repeat from
                                             * the clicks on notify / indicate
                                             */
                                            .compose(repeatAfterCompleted())
                                            // at the beginning inform the activity about whether compat mode is being used
                                            .startWith(new CompatibilityModeEvent(
                                                    hasProperty(characteristic, PROPERTY_NOTIFY | PROPERTY_INDICATE)
                                                            && notificationSetupMode == NotificationSetupMode.COMPAT
                                            ));

                                    // merge all events from reads, writes, notifications and indications
                                    return Observable.merge(
                                            readObservable,
                                            writeObservable,
                                            notifyAndIndicateObservable
                                    )
                                            // start by informing the Activity that connection is established
                                            .startWith(new InfoEvent("Hey, connection has been established!"));
                                }
                        )
                        // flatMap the observable to itself to get the PresenterEvents
                        .flatMap(presenterEventObservable -> presenterEventObservable)
                        .compose(takeUntil(connectingClicks, disconnectClicks)) // convenience transformer to close the connection
                        // in case of a connection error inform the activity
                        .onErrorReturn(throwable -> new InfoEvent("Connection error: " + throwable))
                )
                .compose(repeatAfterCompleted()); // if the the above will complete - start from the beginning
    }

    private static boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return (characteristic.getProperties() & property) > 0;
    }

    /**
     * A convenience function creating a transformer that will use two observables for completing the returned observable (and
     * un-subscribing from the passed observable) beforeEmission will be used to complete the passed observable before it's first
     * emission and afterEmission will be used to do the same after the first emission
     *
     * @param beforeEmission the observable that will control completing the returned observable before it's first emission
     * @param afterEmission  the observable that will control completing the returned observable after it's first emission
     * @param <T>            the type of the passed observable
     * @return the observable
     */
    @NonNull
    private static <T> Observable.Transformer<T, T> takeUntil(Observable<?> beforeEmission, Observable<?> afterEmission) {
        return observable -> observable.publish(publishedObservable -> {
                    final Observable<?> afterEmissionTakeUntil = publishedObservable
                            .take(1)
                            .toCompletable()
                            .andThen(afterEmission);
                    return Observable.amb(
                            publishedObservable,
                            publishedObservable.ignoreElements().takeUntil(beforeEmission)
                    )
                            .takeUntil(afterEmissionTakeUntil);
                }
        );
    }

    /**
     * A convenience function creating a transformer that will wrap the emissions in either {@link ResultEvent} or {@link ErrorEvent}
     * with a given {@link Type}
     *
     * @param type the type to wrap with
     * @return transformer that will emit an observable that will be emitting ResultEvent or ErrorEvent with a given type
     */
    @NonNull
    private static Observable.Transformer<byte[], PresenterEvent> transformToPresenterEvent(Type type) {
        return observable -> observable.map(writtenBytes -> ((PresenterEvent) new ResultEvent(writtenBytes, type)))
                .onErrorReturn(throwable -> new ErrorEvent(throwable, type));
    }

    /**
     * A convenience function creating a transformer that will wrap the emissions in either {@link ResultEvent} or {@link ErrorEvent}
     * with a given {@link Type} for notification type {@link Observable} (Observable<Observable<byte[]>>)
     *
     * @param type the type to wrap with
     * @return the transformer
     */
    @NonNull
    private static Observable.Transformer<Observable<byte[]>, PresenterEvent> transformToNotificationPresenterEvent(Type type) {
        return observableObservable -> observableObservable
                .flatMap(observable -> observable
                        .map(bytes -> ((PresenterEvent) new ResultEvent(bytes, type)))
                )
                .onErrorReturn(throwable -> new ErrorEvent(throwable, type));
    }

    /**
     * A convenience function creating a transformer that will repeat the source observable whenever it will complete
     *
     * @param <T> the type of the transformed observable
     * @return transformer that will emit observable that will never complete (source will be subscribed again)
     */
    @NonNull
    private static <T> Observable.Transformer<T, T> repeatAfterCompleted() {
        return observable -> observable.repeatWhen(completedNotification -> completedNotification);
    }
}
