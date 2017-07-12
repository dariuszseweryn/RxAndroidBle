package com.polidea.rxandroidble.internal.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CharacteristicPropertiesParser {

    private final int propertyBroadcast;
    private final int propertyRead;
    private final int propertyWriteNoResponse;
    private final int propertyWrite;
    private final int propertyNotify;
    private final int propertyIndicate;
    private final int propertySignedWrite;
    private final int[] possibleProperties;

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

    public CharacteristicPropertiesParser(@BluetoothGattCharacteristicProperty int propertyBroadcast,
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
        possibleProperties = getPossibleProperties();
    }

    @NonNull
    public String propertiesIntToString(int property) {
        StringBuilder builder = new StringBuilder();
        builder.append("[ ");
        for (int possibleProperty : possibleProperties) {
            if (propertiesIntContains(property, possibleProperty)) {
                builder.append(propertyToString(possibleProperty));
                builder.append(" ");
            }
        }
        builder.append("]");
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
        } else if (property == 0) {
            return "";
        } else {
            // This case is unicorny and only left for my peace of mind. The property is matched against known dictionary before
            // being passed here, so it MUST match one of the values [MK]
            RxBleLog.e("Unknown property specified");
            return "UNKNOWN (" + property + " -> check android.bluetooth.BluetoothGattCharacteristic)";
        }
    }
}
