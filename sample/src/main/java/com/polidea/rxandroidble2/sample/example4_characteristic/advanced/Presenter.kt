package com.polidea.rxandroidble2.sample.example4_characteristic.advanced

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.sample.example4_characteristic.advanced.Type.INDICATE
import com.polidea.rxandroidble2.sample.example4_characteristic.advanced.Type.NOTIFY
import com.polidea.rxandroidble2.sample.util.hasProperty
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.functions.Function
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
        // establish connection and get characteristic
        .flatMap(connectAndGetCharacteristic(device, characteristicUuid))
        // react to clicks by triggering either read, write, notify or indicate
        .flatMap(
            readWriteNotifyIndicate(
                characteristicUuid,
                readClicks,
                writeClicks,
                enableNotifyClicks,
                enableIndicateClicks,
                enablingIndicateClicks,
                disableIndicateClicks,
                enablingNotifyClicks,
                disableNotifyClicks
            )
        )
        // convenience transformer to close the connection
        .compose(takeUntil(connectingClicks, disconnectClicks))
        // in case of a connection error inform the activity
        .onErrorReturn { throwable -> InfoEvent("Connection error: $throwable") }
        .compose(repeatAfterCompleted())

/**
 * Establishes connection, gets characteristic and returns them as [Pair].
 */
private fun connectAndGetCharacteristic(
    device: RxBleDevice,
    characteristicUuid: UUID
): Function<Boolean, Observable<Pair<RxBleConnection, BluetoothGattCharacteristic>>> =
    Function {
        device.establishConnection(false) // on click start connecting
            .flatMapSingle { connection ->
                getCharacteristic(characteristicUuid, connection).map { connection to it }
            }
    }

/**
 * Gets characteristic from [connection] for the given [characteristicUuid].
 */
private fun getCharacteristic(
    characteristicUuid: UUID,
    connection: RxBleConnection
): Single<BluetoothGattCharacteristic> =
    connection
        // when connected discover services
        .discoverServices()
        .flatMap { it.getCharacteristic(characteristicUuid) }

/**
 * Reacts to user clicks, triggers operation he selected on received [RxBleConnection] and
 * [BluetoothGattCharacteristic] (read, write, notify or indicate) and emits the result as a [PresenterEvent].
 */
private fun readWriteNotifyIndicate(
    characteristicUuid: UUID,
    readClicks: Observable<Boolean>,
    writeClicks: Observable<ByteArray>,
    enableNotifyClicks: Observable<Boolean>,
    enableIndicateClicks: Observable<Boolean>,
    enablingIndicateClicks: Observable<Boolean>,
    disableIndicateClicks: Observable<Boolean>,
    enablingNotifyClicks: Observable<Boolean>,
    disableNotifyClicks: Observable<Boolean>
): Function<Pair<RxBleConnection, BluetoothGattCharacteristic>, Observable<PresenterEvent>> =
    Function { connectionAndCharacteristic ->
        val (connection, characteristic) = connectionAndCharacteristic

        // clicks trigger read/write operations from the peripheral
        val readObservable = readClicks.readCharacteristic(connection, characteristic)
        val writeObservable = writeClicks.writeCharacteristic(connection, characteristic)
        // checking if characteristic will potentially need a compatibility mode notifications
        val notificationSetupMode = characteristic.notificationSetupMode

        /*
         * wrapping observables for notifications and indications so they will emit FALSE and TRUE respectively.
         * this is needed because only one of them may be active at the same time and we need to differentiate
         * the clicks
         */
        val enableNotifyClicksObservable = enableNotifyClicks
            .compose(characteristic.enableNotifyOrIndicate(PROPERTY_NOTIFY))
        val enableIndicateClicksObservable = enableIndicateClicks
            .compose(characteristic.enableNotifyOrIndicate(PROPERTY_INDICATE))

        val notifyAndIndicateObservable = selectNotificationOrIndication(
            connection,
            characteristic,
            characteristicUuid,
            notificationSetupMode,
            enableNotifyClicksObservable,
            enableIndicateClicksObservable,
            enablingIndicateClicks,
            disableIndicateClicks,
            enablingNotifyClicks,
            disableNotifyClicks
        )

        // merge all events from reads, writes, notifications and indications
        Observable.merge(readObservable, writeObservable, notifyAndIndicateObservable)
            // start by informing the Activity that connection is established
            .startWith(InfoEvent("Hey, connection has been established!"))
    }

private fun Observable<Boolean>.readCharacteristic(
    connection: RxBleConnection,
    characteristic: BluetoothGattCharacteristic
): Observable<PresenterEvent> =
    if (!characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_READ)) {
        // if the characteristic is not readable return an empty (dummy) observable
        Observable.empty()
    } else {
        // else use the readClicks observable from the activity
        // every click is requesting a read operation from the peripheral
        flatMapSingle { connection.readCharacteristic(characteristic) }
            .compose(transformToPresenterEvent(Type.READ)) // convenience method to wrap reads
    }

private fun Observable<ByteArray>.writeCharacteristic(
    connection: RxBleConnection,
    characteristic: BluetoothGattCharacteristic
): Observable<PresenterEvent> =
// basically the same logic as in the reads
    if (!characteristic.hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)) {
        Observable.empty()
    } else {
        // with exception that clicks emit byte[] to write
        flatMapSingle { bytes -> connection.writeCharacteristic(characteristic, bytes) }
            .compose(transformToPresenterEvent(Type.WRITE))
    }

private fun BluetoothGattCharacteristic.enableNotifyOrIndicate(property: Int): ObservableTransformer<Boolean, Boolean> =
    ObservableTransformer {
        if (!hasProperty(property)) {
            /*
             * if property for notifications/indications is not available return Observable.never() dummy observable.
             * Observable.never() is needed because of the Observable.amb() later in the chain which repeats
             * the behaviour of Observable that first emits or terminates and it will be checking both
             * notifyClicks and indicateClicks
             */
            Observable.never()
        } else {
            // only the first click to source clicks Observable is taken to account
            // we map to true/false to differentiate clicks to notify/indicate
            it.take(1).map { property == PROPERTY_NOTIFY }
        }
    }

private fun selectNotificationOrIndication(
    connection: RxBleConnection,
    characteristic: BluetoothGattCharacteristic,
    uuid: UUID,
    mode: NotificationSetupMode,
    enableNotifyClicksObservable: Observable<Boolean>,
    enableIndicateClicksObservable: Observable<Boolean>,
    enablingIndicateClicks: Observable<Boolean>,
    disableIndicateClicks: Observable<Boolean>,
    enablingNotifyClicks: Observable<Boolean>,
    disableNotifyClicks: Observable<Boolean>
): Observable<PresenterEvent> =
// checking which notify or indicate will be clicked first the other is unsubscribed on click
    Observable.amb(asList(enableNotifyClicksObservable, enableIndicateClicksObservable))
        .flatMap { isIndication ->
            if (isIndication) {
                // we clicked indication
                setupNotificationOrIndication(
                    connection,
                    uuid,
                    mode,
                    enablingIndicateClicks,
                    disableIndicateClicks,
                    INDICATE
                )
            } else {
                // we clicked notification
                setupNotificationOrIndication(
                    connection,
                    uuid,
                    mode,
                    enablingNotifyClicks,
                    disableNotifyClicks,
                    NOTIFY
                )
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
                        && mode == NotificationSetupMode.COMPAT
            )
        )

@Suppress("ReplaceSingleLineLet")
private fun setupNotificationOrIndication(
    connection: RxBleConnection,
    uuid: UUID,
    mode: NotificationSetupMode,
    beforeEmission: Observable<Boolean>,
    afterEmission: Observable<Boolean>,
    type: Type
): Observable<PresenterEvent> =
    if (type == Type.INDICATE) {
        // we setup indication
        connection.setupIndication(uuid, mode)
    } else {
        // we setup notification
        connection.setupNotification(uuid, mode)
    }.let {
        it
            // use a convenience transformer for tearing down the indications/notifications
            .compose(takeUntil(beforeEmission, afterEmission))
            // and wrap the emissions with a convenience function
            .compose(transformToNotificationPresenterEvent(type))
    }

/**
 * A convenience function creating a transformer that will use two observables for completing the returned observable (and
 * unsubscribing from the passed observable) [beforeEmission] will be used to complete the passed observable before its first
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

private val BluetoothGattCharacteristic.notificationSetupMode: NotificationSetupMode
    get() = if (getDescriptor(clientCharacteristicConfigDescriptorUuid) == null) {
        NotificationSetupMode.COMPAT
    } else {
        NotificationSetupMode.DEFAULT
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
