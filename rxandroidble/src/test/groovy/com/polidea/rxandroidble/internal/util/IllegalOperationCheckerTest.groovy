package com.polidea.rxandroidble.internal.util

import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.internal.connection.IllegalOperationChecker
import com.polidea.rxandroidble.internal.connection.IllegalOperationHandler
import spock.lang.Specification

class IllegalOperationCheckerTest extends Specification {

    IllegalOperationChecker objectUnderTest
    BluetoothGattCharacteristic bluetoothGattCharacteristic
    IllegalOperationHandler mockHandler
    final int NO_PROPERTIES = 0

    void setup() {
        mockHandler = Mock IllegalOperationHandler
        objectUnderTest = new IllegalOperationChecker(mockHandler)

        bluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    }

    def "should handle message if no property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> NO_PROPERTIES

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)
                .subscribe()

        then:
        1 * mockHandler.handleMismatchData(_, _)
    }

    def "should not handle message if the only needed property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_READ

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)
                .subscribe()

        then:
        0 * mockHandler.handleMismatchData(_, _)
    }

    def "should not handle message if at least one of the needed properties match"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_WRITE

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                .subscribe()

        then:
        0 * mockHandler.handleMismatchData(_, _)
    }
}
