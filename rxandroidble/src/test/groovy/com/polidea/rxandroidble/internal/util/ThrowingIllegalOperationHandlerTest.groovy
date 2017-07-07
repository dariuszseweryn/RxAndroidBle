package com.polidea.rxandroidble.internal.util

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException
import com.polidea.rxandroidble.internal.connection.ThrowingIllegalOperationHandler
import spock.lang.Specification

class ThrowingIllegalOperationHandlerTest extends Specification {

    ThrowingIllegalOperationHandler objectUnderTest
    String message = ""
    UUID mockUuid = null
    int supportedProperties = 0
    int neededProperties = 0


    void setup() {
        objectUnderTest = new ThrowingIllegalOperationHandler()
    }

    def "should throw BleIllegalOperationException if no property matches"() {
        when:
        objectUnderTest.handleMismatchData(message, mockUuid, supportedProperties, neededProperties)

        then:
        thrown BleIllegalOperationException
    }
}
