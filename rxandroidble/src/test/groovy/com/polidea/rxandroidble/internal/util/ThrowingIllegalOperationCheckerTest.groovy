package com.polidea.rxandroidble.internal.util

import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.exceptions.BleIllegalOperationException
import spock.lang.Specification

class ThrowingIllegalOperationCheckerTest extends Specification {

    ThrowingIllegalOperationChecker objectUnderTest
    BluetoothGattCharacteristic bluetoothGattCharacteristic

    void setup() {
        objectUnderTest = new ThrowingIllegalOperationChecker(
                BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
        )

        bluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    }

    def "should throw BleIllegalOperationException if no property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_WRITE

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)

        then:
        thrown BleIllegalOperationException
    }

    def "should not throw exception if at least one of the needed properties match"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_WRITE

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

        then:
        noExceptionThrown()
    }
}
