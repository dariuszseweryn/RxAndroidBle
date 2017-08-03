package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.internal.BluetoothGattCharacteristicProperty;
import com.polidea.rxandroidble.internal.util.CharacteristicPropertiesParser;

import java.util.Locale;

import javax.inject.Inject;

public class IllegalOperationMessageCreator {

    private CharacteristicPropertiesParser propertiesParser;

    @Inject
    public IllegalOperationMessageCreator(CharacteristicPropertiesParser propertiesParser) {
        this.propertiesParser = propertiesParser;
    }

    @SuppressWarnings("WrongConstant")
    public String createMismatchMessage(BluetoothGattCharacteristic characteristic,
                                        @BluetoothGattCharacteristicProperty int neededProperties) {
        return String.format(
                Locale.getDefault(),
                "Characteristic %s supports properties: %s (%d) does not have any property matching %s (%d)",
                characteristic.getUuid(),
                propertiesParser.propertiesIntToString(characteristic.getProperties()),
                characteristic.getProperties(),
                propertiesParser.propertiesIntToString(neededProperties),
                neededProperties
        );
    }
}
