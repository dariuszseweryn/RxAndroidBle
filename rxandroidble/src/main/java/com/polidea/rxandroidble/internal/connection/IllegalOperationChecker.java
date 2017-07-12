package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import rx.Completable;

/**
 * Class for checking whether the requested operation is legal on chosen characteristic.
 */
public class IllegalOperationChecker {

    private IllegalOperationHandler resultHandler;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true,
            value = {BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY})
    @interface BluetoothGattCharacteristicProperty { }

    @Inject
    public IllegalOperationChecker(IllegalOperationHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    /**
     * This method checks whether the supplied characteristic possesses properties supporting the requested kind of operation, specified by
     * the supplied bitmask.
     *
     * @param characteristic   a {@link BluetoothGattCharacteristic} the operation is done on
     * @param neededProperties properties required for the operation to be successfully completed
     * @return {@link Completable} deferring execution of the check till subscription
     */
    public Completable checkAnyPropertyMatches(final BluetoothGattCharacteristic characteristic,
                                               final @BluetoothGattCharacteristicProperty int neededProperties) {
        return Completable.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final int characteristicProperties = characteristic.getProperties();

                if ((characteristicProperties & neededProperties) == 0) {
                    resultHandler.handleMismatchData(characteristic, neededProperties);
                }
                return null;
            }
        });
    }
}