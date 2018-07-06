package com.polidea.rxandroidble.exceptions

import spock.lang.Specification

/**
 * Created by jallen on 2017-11-01.
 */
class BleDisconnectedExceptionTest extends Specification {

    BleDisconnectedException objectUnderTest

    def "toString should include message with unknown status"() {

        when:
        objectUnderTest = new BleDisconnectedException("myBluetoothAddress")

        then:
        assert objectUnderTest.toString() ==
            "com.polidea.rxandroidble.exceptions.BleDisconnectedException: Disconnected from myBluetoothAddress with status -1 (UNKNOWN)"
    }

    def "toString should include message with status"() {
        given:
        def expectedStatus = 0x81

        when:
        objectUnderTest = new BleDisconnectedException("myBluetoothAddress", expectedStatus)

        then:
        assert objectUnderTest.toString() ==
                "com.polidea.rxandroidble.exceptions.BleDisconnectedException: Disconnected from myBluetoothAddress with status $expectedStatus (GATT_INTERNAL_ERROR)"
    }
}
