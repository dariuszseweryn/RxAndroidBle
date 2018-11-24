package com.polidea.rxandroidble2.sample.example7_long_write

import android.os.Bundle
import com.polidea.rxandroidble2.sample.SampleApplication
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import java.util.UUID

private const val DUMMY_DEVICE_ADDRESS = "AA:AA:AA:AA:AA:AA"

private val DEVICE_CALLBACK_0 = UUID.randomUUID()

private val DEVICE_CALLBACK_1 = UUID.randomUUID()

private val WRITE_CHARACTERISTIC = UUID.randomUUID()

/**
 * For the sake of this example lets assume that we have a Bluetooth Device that is retrieved by:
 *
 *
 * rxBleClient.getBleDevice(DUMMY_DEVICE_ADDRESS) // (it can be retrieved by scanning as well)
 *
 *
 * This device has two notification characteristics:
 * DEVICE_CALLBACK_0 (DC0) notifies when the previously sent batch was received
 * DEVICE_CALLBACK_1 (DC1) notifies when the device is ready to receive the next packet
 *
 *
 * Lets assume that we do not know if the DC0 or DC1 will notify first.
 * It may also happen that Android OS will inform that the batch was transmitted after both DC0 and DC1 notify.
 *
 *
 * We need to write 1024 bytes of data to the device
 */
class LongWriteExampleActivity : RxAppCompatActivity() {

    private val bytesToWrite = ByteArray(1024) // a kilobyte array

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rxBleClient = SampleApplication.rxBleClient

        disposable = rxBleClient.getBleDevice(DUMMY_DEVICE_ADDRESS) // get our assumed device
            .establishConnection(false) // establish the connection
            .flatMap({ connection ->
                Observable.combineLatest(
                    // after establishing the connection let's setup the notifications
                    connection.setupNotification(DEVICE_CALLBACK_0),
                    connection.setupNotification(DEVICE_CALLBACK_1),
                    BiFunction<Observable<ByteArray>, Observable<ByteArray>, Pair<Observable<ByteArray>, Observable<ByteArray>>> { deviceCallback0, deviceCallback1 ->
                        deviceCallback0 to deviceCallback1
                    }
                )
            }, { // after the setup let's start the long write
                    connection, callbackObservablePair ->
                val (deviceCallback0, deviceCallback1) = callbackObservablePair

                connection.createNewLongWriteBuilder() // create a new long write builder
                    .setBytes(bytesToWrite) // REQUIRED - set the bytes to write
                    /*
                     * REQUIRED - To perform a write you need to specify to which characteristic you want to write. You can do it
                     * either by calling {@link LongWriteOperationBuilder#setCharacteristicUuid(UUID)} or
                     * {@link LongWriteOperationBuilder#setCharacteristic(BluetoothGattCharacteristic)}
                     */
                    .setCharacteristicUuid(WRITE_CHARACTERISTIC) // set the UUID of the characteristic to write
                    // .setCharacteristic( /* some BluetoothGattCharacteristic */ ) // alternative to setCharacteristicUuid()
                    /*
                     * If you want to send batches with length other than default.
                     * Default value is 20 bytes if MTU was not negotiated. If the MTU was negotiated prior to the Long Write
                     * Operation execution then the batch size default is the new MTU.
                     */
                    // .setMaxBatchSize( /* your batch size */ )
                    /*
                      Inform the Long Write when we want to send the next batch of data. If not set the operation will try to write
                      the next batch of data as soon as the Android will call `BluetoothGattCallback.onCharacteristicWrite()` but
                      we want to postpone it until also DC0 and DC1 will emit.
                     */
                    .setWriteOperationAckStrategy { booleanObservable ->
                        Observable.zip(
                            // so we zip three observables
                            deviceCallback0, // DEVICE_CALLBACK_0
                            deviceCallback1, // DEVICE_CALLBACK_1
                            booleanObservable, /* previous batch of data was sent - we do not care if value emitted from
                                            the booleanObservable is TRUE or FALSE. But the value will be TRUE unless the previously sent
                                            data batch was the final one */
                            Function3 { _, _, aBoolean -> aBoolean } // value of the returned Boolean is not important
                        )
                    }
                    .build()
            })
            .flatMap { it }
            .take(1) // after the successful write we are no longer interested in the connection so it will be released
            .subscribe(
                { bytes ->
                    // react
                },
                { throwable ->
                    // handle error
                }
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        disposable = null
    }
}
