package com.polidea.rxandroidble3.samplekotlin.example7_long_write

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.samplekotlin.SampleApplication
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables
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
class LongWriteExampleActivity : AppCompatActivity() {

    companion object {
        @Suppress("unused")
        fun newInstance(context: Context) = Intent(context, LongWriteExampleActivity::class.java)
    }

    private val bytesToWrite = ByteArray(1024) // a kilobyte array

    private var longWriteDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rxBleClient = SampleApplication.rxBleClient

        rxBleClient.getBleDevice(DUMMY_DEVICE_ADDRESS) // get our assumed device
            .establishConnection(false) // establish the connection
            .flatMap(
                { connection -> setupNotifications(connection) },
                { connection, deviceCallbacks -> performLongWrite(connection, deviceCallbacks) }
            )
            .flatMap { it }
            .take(1) // after the successful write we are no longer interested in the connection so it will be released
            .subscribe(
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                { bytes ->
                    // react
                },
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                { throwable ->
                    // handle error
                }
            )
            .let { longWriteDisposable = it }
    }

    /**
     * Performs long write via given [connection], waiting for callbacks from device through [notifications].
     */
    private fun performLongWrite(
        connection: RxBleConnection,
        notifications: Pair<Observable<ByteArray>, Observable<ByteArray>>
    ): Observable<ByteArray> {
        val (deviceCallback0, deviceCallback1) = notifications

        return connection.createNewLongWriteBuilder() // create a new long write builder
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
              the next batch of data as soon as Android will call `BluetoothGattCallback.onCharacteristicWrite()` but
              we want to postpone it until also DC0 and DC1 will emit.
             */
            .setWriteOperationAckStrategy { bufferNonEmpty ->
                // so we zip three observables
                Observables.zip(
                    deviceCallback0, // DEVICE_CALLBACK_0
                    deviceCallback1, // DEVICE_CALLBACK_1
                    bufferNonEmpty /* previous batch of data was sent - we do not care if value emitted from
                                      the bufferNonEmpty is TRUE or FALSE. But the value will be TRUE unless the previously sent
                                      data batch was the final one */
                ) { _, _, nonEmpty -> nonEmpty }
            }
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        longWriteDisposable?.dispose()
    }
}

/**
 * Sets up notifications from both characteristics and combines them into one [Observable].
 */
private fun setupNotifications(
    connection: RxBleConnection
): Observable<Pair<Observable<ByteArray>, Observable<ByteArray>>> =
    Observables.combineLatest(
        connection.setupNotification(DEVICE_CALLBACK_0),
        connection.setupNotification(DEVICE_CALLBACK_1)
    ) { deviceCallback0, deviceCallback1 -> deviceCallback0 to deviceCallback1 }