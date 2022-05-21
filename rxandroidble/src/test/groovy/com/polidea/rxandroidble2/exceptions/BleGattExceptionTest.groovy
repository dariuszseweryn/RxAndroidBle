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
                "com.polidea.rxandroidble2.exceptions.BleGattException: GATT exception from MAC=null, status 10 (GATT_NOT_FOUND), " +
                "type BleGattOperation{description='CONNECTION_STATE'}. " +
                "(Look up status 0x0a here " +
                "https://cs.android.com/android/platform/superproject/+/master:packages/modules/Bluetooth/system/stack/include/gatt_api.h)"
    }
}
