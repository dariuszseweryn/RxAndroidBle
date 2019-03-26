package com.polidea.rxandroidble2.internal.util;

import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.internal.BluetoothGattCharacteristicProperty;
import com.polidea.rxandroidble2.internal.RxBleLog;

public class CharacteristicPropertiesParser {

    private final @BluetoothGattCharacteristicProperty int propertyBroadcast;
    private final @BluetoothGattCharacteristicProperty int propertyRead;
    private final @BluetoothGattCharacteristicProperty int propertyWriteNoResponse;
    private final @BluetoothGattCharacteristicProperty int propertyWrite;
    private final @BluetoothGattCharacteristicProperty int propertyNotify;
    private final @BluetoothGattCharacteristicProperty int propertyIndicate;
    private final @BluetoothGattCharacteristicProperty int propertySignedWrite;
    private final int[] possibleProperties;


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
    public String propertiesIntToString(@BluetoothGattCharacteristicProperty int property) {
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

    private @NonNull int[] getPossibleProperties() {
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

    private static boolean propertiesIntContains(@BluetoothGattCharacteristicProperty int properties,
                                                 @BluetoothGattCharacteristicProperty int property) {
        return (properties & property) != 0;
    }

    @NonNull
    private String propertyToString(@BluetoothGattCharacteristicProperty int property) {
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
            RxBleLog.e("Unknown property specified (%d)", property);
            return "UNKNOWN (" + property + " -> check android.bluetooth.BluetoothGattCharacteristic)";
        }
    }
}
