package com.polidea.rxandroidble2.exceptions

import spock.lang.Specification

/**
 * Tests BleGattException.
 */
class BleGattExceptionTest extends Specification {

    BleGattException objectUnderTest

    def "toString should include message"() {

        when:
        objectUnderTest = new BleGattException(10, BleGattOperationType.CONNECTION_STATE)

        then:
        assert objectUnderTest.toString() ==
                "com.polidea.rxandroidble.exceptions.BleGattException: GATT exception from MAC address null, status 10, " +
                "type BleGattOperation{description='CONNECTION_STATE'}. " +
                "(Look up status 0x0a here " +
                "https://android.googlesource.com/platform/external/bluetooth/bluedroid/+/android-5.1.0_r1/stack/include/gatt_api.h)"
    }
}
