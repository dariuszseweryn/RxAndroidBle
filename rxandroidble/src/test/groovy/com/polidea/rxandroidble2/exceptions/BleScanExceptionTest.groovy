package com.polidea.rxandroidble2.exceptions

import spock.lang.Specification

import static com.polidea.rxandroidble2.exceptions.BleScanException.BLUETOOTH_DISABLED

/**
 * Tests BleScanException
 */
class BleScanExceptionTest extends Specification {

    BleScanException objectUnderTest

    def "toString should include message"() {

        when:
        objectUnderTest = new BleScanException(BLUETOOTH_DISABLED)

        then:
        assert objectUnderTest.toString() ==
                "com.polidea.rxandroidble2.exceptions.BleScanException: Bluetooth disabled (code 1)"
    }
}
