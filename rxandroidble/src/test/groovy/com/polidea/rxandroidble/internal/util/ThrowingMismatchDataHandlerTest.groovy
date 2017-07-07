package com.polidea.rxandroidble.internal.util

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException
import spock.lang.Specification

class ThrowingMismatchDataHandlerTest extends Specification {

    ThrowingMismatchDataHandler objectUnderTest
    IllegalOperationChecker.MismatchData mockData

    void setup() {
        objectUnderTest = new ThrowingMismatchDataHandler()
        mockData = Mock IllegalOperationChecker.MismatchData
    }

    def "should throw BleIllegalOperationException if no property matches"() {
        when:
        objectUnderTest.handleMismatchData(mockData)

        then:
        thrown BleIllegalOperationException
    }
}
