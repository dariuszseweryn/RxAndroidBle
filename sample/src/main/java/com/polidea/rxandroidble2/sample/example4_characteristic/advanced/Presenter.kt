package com.polidea.rxandroidble2.sample.example4_characteristic.advanced

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.util.hasProperty
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import java.util.Arrays.asList
import java.util.UUID

private val clientCharacteristicConfigDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * Presenter function for [AdvancedCharacteristicOperationExampleActivity]. Prepares the logic for the activity using passed
 * [Observable]s. Implemented as a top-level function to show a purely reactive (stateless) approach.
 *
 *
 * The contract of the function is that it subscribes to the passed Observables only if a specific functionality is possible to use.
 */
internal fun prepareActivityLogic(
    device: RxBleDevice,
    characteristicUuid: UUID,
    connectClicks: Observable<Boolean>,
    connectingClicks: Observable<Boolean>, // used to disconnect the device even before connection is established
    disconnectClicks: Observable<Boolean>,
    readClicks: Observable<Boolean>,
    writeClicks: Observable<ByteArray>,
    enableNotifyClicks: Observable<Boolean>,
    // used to disable notifications before they were enabled (but after enable click)
    enablingNotifyClicks: Observable<Boolean>,
    disableNotifyClicks: Observable<Boolean>,
    enableIndicateClicks: Observable<Boolean>,
    // used to disable indications before they were enabled (but after enable click)
    enablingIndicateClicks: Observable<Boolean>,
    disableIndicateClicks: Observable<Boolean>
): Observable<PresenterEvent> =

    connectClicks.take(1) // subscribe to connectClicks and take one (unsubscribe after)
        .flatMap { _ ->
            device.establishConnection(false) // on click start connecting
                .flatMapSingle { connection ->
                    getCharacteristic(characteristicUuid, connection)
                        .map { connection to it }
                }
        }
        .flatMap { connectionAndCharacteristic ->
            val (connection, characteristic) = connectionAndCharacteristic
            // convenience method to wrap reads
            val readObservable = if (!characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_READ)) {
                Observable.empty()
            } else {
                // else use the readClicks observable from the activity
                readClicks
                    // every click is requesting a read operation from the peripheral
                    .flatMapSingle { connection.readCharacteristic(characteristic) }
                    .compose(transformToPresenterEvent(Type.READ))// if the characteristic is not readable return an empty (dummy) observable
            }
            // basically the same logic as in the reads
            val writeObservable = if (!characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)) {
                Observable.empty()
            } else {
                // with exception that clicks emit byte[] to write
                writeClicks
                    .flatMapSingle { bytes -> connection.writeCharacteristic(characteristic, bytes) }
                    .compose(transformToPresenterEvent(Type.WRITE))
            }
            // checking if characteristic will potentially need a compatibility mode notifications
            val notificationSetupMode =
                if (characteristic.getDescriptor(clientCharacteristicConfigDescriptorUuid) == null) {
                    NotificationSetupMode.COMPAT
                } else {
                    NotificationSetupMode.DEFAULT
                }
            /*
             * wrapping observables for notifications and indications so they will emit FALSE and TRUE respectively.
             * this is needed because only one of them may be active at the same time and we need to differentiate
             * the clicks
             */
            val enableNotifyClicksObservable = if (!characteristic.hasProperty(PROPERTY_NOTIFY)) {
                Observable.never()
            } else {
                enableNotifyClicks.take(1).map { false }
            }
            /*
            * if property for notifications is not available return Observable.never() dummy observable.
            * Observable.never() is needed because of the Observable.amb() below which repeats
            * the behaviour of Observable that first emits or terminates and it will be checking both
            * notifyClicks and indicateClicks
            */
            // only the first click to enableNotifyClicks is taken to account
            val enableIndicateClicksObservable = if (!characteristic.hasProperty(PROPERTY_INDICATE)) {
                Observable.never()
            } else {
                enableIndicateClicks.take(1).map { true }
            }

            // checking which notify or indicate will be clicked first the other is unsubscribed on click
            val notifyAndIndicateObservable = Observable.amb(
                asList(enableNotifyClicksObservable, enableIndicateClicksObservable)
            )
                .flatMap { isIndication ->
                    if (isIndication) {
                        // if indication was clicked
                        connection
                            // we setup indications
                            .setupIndication(characteristicUuid, notificationSetupMode)
                            // use a convenience transformer for tearing down the notifications
                            .compose(takeUntil(enablingIndicateClicks, disableIndicateClicks))
                            // and wrap the emissions with a convenience function
                            .compose(transformToNotificationPresenterEvent(Type.INDICATE))
                    } else {
                        // if notification was clicked
                        connection
                            .setupNotification(characteristicUuid, notificationSetupMode)
                            .compose(takeUntil(enablingNotifyClicks, disableNotifyClicks))
                            .compose(transformToNotificationPresenterEvent(Type.NOTIFY))
                    }
                }
                /*
                * whenever the notification or indication is finished (by the user or an error) repeat from
                * the clicks on notify / indicate
                */
                .compose(repeatAfterCompleted())
                // at the beginning inform the activity about whether compat mode is being used
                .startWith(
                    CompatibilityModeEvent(
                        characteristic.hasProperty(PROPERTY_NOTIFY or PROPERTY_INDICATE)
                                && notificationSetupMode == NotificationSetupMode.COMPAT
                    )
                )

            // merge all events from reads, writes, notifications and indications
            Observable.merge(
                readObservable,
                writeObservable,
                notifyAndIndicateObservable
            )
                // start by informing the Activity that connection is established
                .startWith(InfoEvent("Hey, connection has been established!"))
        }
        // convenience transformer to close the connection
        .compose(takeUntil(connectingClicks, disconnectClicks))
        // in case of a connection error inform the activity
        .onErrorReturn { throwable -> InfoEvent("Connection error: $throwable") }
        .compose(repeatAfterCompleted())

// when connected discover services
private fun getCharacteristic(
    characteristicUuid: UUID,
    connection: RxBleConnection
): Single<BluetoothGattCharacteristic> =
    connection
        .discoverServices()
        .flatMap { services -> services.getCharacteristic(characteristicUuid) }

/**
 * A convenience function creating a transformer that will use two observables for completing the returned observable (and
 * un-subscribing from the passed observable) [beforeEmission] will be used to complete the passed observable before its first
 * emission and [afterEmission] will be used to do the same after the first emission
 *
 * @param beforeEmission the observable that will control completing the returned observable before its first emission
 * @param afterEmission  the observable that will control completing the returned observable after its first emission
 * @param <T>            the type of the passed observable
 * @return the observable
 */
private fun <T> takeUntil(beforeEmission: Observable<*>, afterEmission: Observable<*>): ObservableTransformer<T, T> =
    ObservableTransformer { observable ->
        observable.publish { publishedObservable ->
            publishedObservable
                .take(1)
                .ignoreElements()
                .andThen(afterEmission).let {
                    Observable.amb(asList(publishedObservable, publishedObservable.takeUntil(beforeEmission)))
                        .takeUntil(it)
                }
        }
    }

/**
 * A convenience function creating a transformer that will wrap the emissions in either [ResultEvent] or [ErrorEvent]
 * with a given [Type]
 *
 * @param type the type to wrap with
 * @return transformer that will emit an observable that will be emitting ResultEvent or ErrorEvent with a given type
 */
private fun transformToPresenterEvent(type: Type): ObservableTransformer<ByteArray, PresenterEvent> =
    ObservableTransformer {
        it
            .map { writtenBytes -> ResultEvent(writtenBytes, type) as PresenterEvent }
            .onErrorReturn { throwable -> ErrorEvent(throwable, type) }
    }

/**
 * A convenience function creating a transformer that will wrap the emissions in either [ResultEvent] or [ErrorEvent]
 * with a given [Type] for notification type `Observable<Observable<ByteArray>>`
 *
 * @param type the type to wrap with
 * @return the transformer
 */
private fun transformToNotificationPresenterEvent(
    type: Type
): ObservableTransformer<Observable<ByteArray>, PresenterEvent> =
    ObservableTransformer { observableObservable ->
        observableObservable
            .flatMap { it.map { bytes -> ResultEvent(bytes, type) as PresenterEvent } }
            .onErrorReturn { throwable -> ErrorEvent(throwable, type) }
    }

/**
 * A convenience function creating a transformer that will repeat the source observable whenever it will complete
 *
 * @param <T> the type of the transformed observable
 * @return transformer that will emit observable that will never complete (source will be subscribed again)
 */
private fun <T> repeatAfterCompleted(): ObservableTransformer<T, T> =
    ObservableTransformer { observable -> observable.repeatWhen { it } }
