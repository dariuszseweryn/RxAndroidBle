package com.polidea.rxandroidble2.exceptions

import android.bluetooth.BluetoothGattCharacteristic
import spock.lang.Specification

/**
 * Tests BleCannotSetCharacteristicNotificationException
 */
class BleCannotSetCharacteristicNotificationExceptionTest extends Specification {

    BleCannotSetCharacteristicNotificationException objectUnderTest

    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic

    def "toString should include message"() {

        given:
        mockCharacteristic.uuid >> new UUID(1, 2)
        when:
        objectUnderTest = new BleCannotSetCharacteristicNotificationException(mockCharacteristic,
                BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION, new Exception("because"))

        then:
        assert objectUnderTest.toString() ==
                "com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException: " +
                "Cannot set local notification (code 1) with characteristic UUID 00000000-0000-0001-0000-000000000002"
    }
}
