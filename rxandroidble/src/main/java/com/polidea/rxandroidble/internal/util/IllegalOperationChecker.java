package com.polidea.rxandroidble.internal.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

/**
 *  Class for checking whether the requested operation is legal on chosen characteristic.
 */
abstract public class IllegalOperationChecker {

    private int propertyBroadcast;
    private int propertyRead;
    private int propertyWriteNoResponse;
    private int propertyWrite;
    private int propertyNotify;
    private int propertyIndicate;
    private int propertySignedWrite;

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

    protected IllegalOperationChecker(@BluetoothGattCharacteristicProperty int propertyBroadcast,
                                      @BluetoothGattCharacteristicProperty int propertyRead,
                                      @BluetoothGattCharacteristicProperty int propertyWriteNoResponse,
                                      @BluetoothGattCharacteristicProperty int propertyWrite,
                                      @BluetoothGattCharacteristicProperty int propertyNotify,
                                      @BluetoothGattCharacteristicProperty int propertyIndicate,
                                      @BluetoothGattCharacteristicProperty int propertySignedWrite) {
        this.propertyBroadcast = propertyBroadcast;
        this.propertyRead = propertyRead;
        this.propertyWriteNoResponse = propertyWriteNoResponse;
        this.propertyWrite = propertyWrite;
        this.propertyNotify = propertyNotify;
        this.propertyIndicate = propertyIndicate;
        this.propertySignedWrite = propertySignedWrite;
    }


    /**
     * This method checks whether the supplied characteristic possesses properties supporting the requested kind of operation, specified by
     * the supplied bitmask.
     * @param characteristic a {@link BluetoothGattCharacteristic} the operation is done on
     * @param neededProperties properties required for the operation to be successfully completed
     */
    public void checkAnyPropertyMatches(BluetoothGattCharacteristic characteristic,
                                        @BluetoothGattCharacteristicProperty int neededProperties) {
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
            handleMessage(message);
        }
    }

    /**
     * Method for handling the potential non-match message
     * @param message message describing the
     */
    protected abstract void handleMessage(String message);

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