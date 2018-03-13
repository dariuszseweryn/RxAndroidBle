package com.polidea.rxandroidble2.exceptions

import spock.lang.Specification

/**
 * Created by jallen on 2017-11-01.
 */
class BleDisconnectedExceptionTest extends Specification {

    BleDisconnectedException objectUnderTest

    def "toString should include message"() {

        when:
        objectUnderTest = new BleDisconnectedException("myBluetoothAddress")

        then:
        assert objectUnderTest.toString() ==
                "com.polidea.rxandroidble.exceptions.BleDisconnectedException: Disconnected from myBluetoothAddress"
    }
}
