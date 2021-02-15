package com.polidea.rxandroidble3.sample.example7_long_write;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.sample.SampleApplication;

import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * For the sake of this example lets assume that we have a Bluetooth Device that is retrieved by:
 * <p>
 * rxBleClient.getBleDevice(DUMMY_DEVICE_ADDRESS) // (it can be retrieved by scanning as well)
 * <p>
 * This device has two notification characteristics:
 * DEVICE_CALLBACK_0 (DC0) notifies when the previously sent batch was received
 * DEVICE_CALLBACK_1 (DC1) notifies when the device is ready to receive the next packet
 * <p>
 * Lets assume that we do not know if the DC0 or DC1 will notify first.
 * It may also happen that Android OS will inform that the batch was transmitted after both DC0 and DC1 notify.
 * <p>
 * We need to write 1024 bytes of data to the device
 */
public class LongWriteExampleActivity extends AppCompatActivity {

    public static final String DUMMY_DEVICE_ADDRESS = "AA:AA:AA:AA:AA:AA";
    private static final UUID DEVICE_CALLBACK_0 = UUID.randomUUID();
    private static final UUID DEVICE_CALLBACK_1 = UUID.randomUUID();
    private static final UUID WRITE_CHARACTERISTIC = UUID.randomUUID();
    private byte[] bytesToWrite = new byte[1024]; // a kilobyte array
    private Disposable disposable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final RxBleClient rxBleClient = SampleApplication.getRxBleClient(this);
        disposable = rxBleClient.getBleDevice(DUMMY_DEVICE_ADDRESS) // get our assumed device
                .establishConnection(false) // establish the connection
                .flatMap(rxBleConnection -> Observable.combineLatest(
                        // after establishing the connection lets setup the notifications
                        rxBleConnection.setupNotification(DEVICE_CALLBACK_0),
                        rxBleConnection.setupNotification(DEVICE_CALLBACK_1),
                        Pair::new
                ), (rxBleConnection, callbackObservablePair) -> { // after the setup lets start the long write
                    Observable<byte[]> deviceCallback0 = callbackObservablePair.first;
                    Observable<byte[]> deviceCallback1 = callbackObservablePair.second;
                    return rxBleConnection.createNewLongWriteBuilder() // create a new long write builder
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
                            .setWriteOperationAckStrategy(new RxBleConnection.WriteOperationAckStrategy() {
                                @Override
                                public ObservableSource<Boolean> apply(Observable<Boolean> booleanObservable) {
                                    return Observable.zip(
                                            // so we zip three observables
                                            deviceCallback0, // DEVICE_CALLBACK_0
                                            deviceCallback1, // DEVICE_CALLBACK_1
                                            booleanObservable, /* previous batch of data was sent - we do not care if value emitted from
                                            the booleanObservable is TRUE or FALSE. But the value will be TRUE unless the previously sent
                                            data batch was the final one */
                                            (callback0, callback1, aBoolean) -> aBoolean // value of the returned Boolean is not important
                                    );
                                }
                            })
                            .build();
                })
                .flatMap(observable -> observable)
                .take(1) // after the successful write we are no longer interested in the connection so it will be released
                .subscribe(
                        bytes -> {
                            // react
                        },
                        throwable -> {
                            // handle error
                        }
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }
}
