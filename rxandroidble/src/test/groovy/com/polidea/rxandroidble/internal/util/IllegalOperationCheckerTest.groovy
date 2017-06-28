package com.polidea.rxandroidble.internal.util

import android.bluetooth.BluetoothGattCharacteristic
import spock.lang.Specification

class IllegalOperationCheckerTest extends Specification {

    IllegalOperationChecker objectUnderTest
    BluetoothGattCharacteristic bluetoothGattCharacteristic
    boolean messageHandled

    void setup() {
        messageHandled = false
        objectUnderTest = new IllegalOperationChecker(
                BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
        ) {
            @Override
            protected void handleMessage(String message) {
                messageHandled = true
            }
        }

        bluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    }

    def "should handle message if no property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> 0

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)

        then:
        messageHandled
    }

    def "should not handle message if the only needed property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_READ

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)

        then:
        !messageHandled
    }

    def "should not handle message if at least one of the needed properties match"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_WRITE

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

        then:
        !messageHandled
    }
}
