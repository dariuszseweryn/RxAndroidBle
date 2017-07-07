package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.concurrent.Callable;

import rx.Completable;

/**
 * Class for checking whether the requested operation is legal on chosen characteristic.
 */
public class IllegalOperationChecker {

    private int propertyBroadcast;
    private int propertyRead;
    private int propertyWriteNoResponse;
    private int propertyWrite;
    private int propertyNotify;
    private int propertyIndicate;
    private int propertySignedWrite;
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

    public IllegalOperationChecker(@BluetoothGattCharacteristicProperty int propertyBroadcast,
                                   @BluetoothGattCharacteristicProperty int propertyRead,
                                   @BluetoothGattCharacteristicProperty int propertyWriteNoResponse,
                                   @BluetoothGattCharacteristicProperty int propertyWrite,
                                   @BluetoothGattCharacteristicProperty int propertyNotify,
                                   @BluetoothGattCharacteristicProperty int propertyIndicate,
                                   @BluetoothGattCharacteristicProperty int propertySignedWrite,
                                   IllegalOperationHandler resultHandler) {
        this.propertyBroadcast = propertyBroadcast;
        this.propertyRead = propertyRead;
        this.propertyWriteNoResponse = propertyWriteNoResponse;
        this.propertyWrite = propertyWrite;
        this.propertyNotify = propertyNotify;
        this.propertyIndicate = propertyIndicate;
        this.propertySignedWrite = propertySignedWrite;
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
                    int[] possibleProperties = getPossibleProperties();
                    String message = String.format(
                            Locale.getDefault(),
                            "Characteristic %s supports properties:[%s] (%d) does not have any property matching [%s] (%d)",
                            characteristic.getUuid(),
                            propertiesIntToString(characteristicProperties, possibleProperties),
                            characteristicProperties,
                            propertiesIntToString(neededProperties, possibleProperties),
                            neededProperties
                    );
                    resultHandler.handleMismatchData(message, characteristic.getUuid(), characteristicProperties, neededProperties);
                }
                return null;
            }
        });
    }

    @NonNull
    private String propertiesIntToString(int property, int[] possibleProperties) {
        StringBuilder builder = new StringBuilder();
        builder.append(" ");
        for (int possibleProperty : possibleProperties) {
            if (propertiesIntContains(property, possibleProperty)) {
                builder.append(propertyToString(possibleProperty));
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    private int[] getPossibleProperties() {
        int[] propertyDictionary = new int[7];
        propertyDictionary[0] = propertyBroadcast;
        propertyDictionary[1] = propertyRead;
        propertyDictionary[2] = propertyWriteNoResponse;
        propertyDictionary[3] = propertyWrite;
        propertyDictionary[4] = propertyNotify;
        propertyDictionary[5] = propertyIndicate;
        propertyDictionary[6] = propertySignedWrite;
        return propertyDictionary;
    }

    private static boolean propertiesIntContains(int properties, int property) {
        return (properties & property) != 0;
    }

    @NonNull
    private String propertyToString(int property) {
        if (property == propertyRead) {
            return "READ";
        } else if (property == propertyWrite) {
            return "WRITE";
        } else if (property == propertyWriteNoResponse) {
            return "WRITE_NO_RESPONSE";
        } else if (property == propertySignedWrite) {
            return "SIGNED_WRITE";
        } else if (property == propertyIndicate) {
            return "INDICATE";
        } else if (property == propertyBroadcast) {
            return "BROADCAST";
        } else if (property == propertyNotify) {
            return "NOTIFY";
        } else {
            RxBleLog.e("Unknown property specified");
            throw new IllegalArgumentException("Unknown property specified");
        }
    }
}