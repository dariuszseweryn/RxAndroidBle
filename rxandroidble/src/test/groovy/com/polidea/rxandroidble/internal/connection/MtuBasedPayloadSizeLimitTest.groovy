package com.polidea.rxandroidble.internal.connection

import com.polidea.rxandroidble.RxBleConnection
import spock.lang.Specification
import spock.lang.Unroll


class MtuBasedPayloadSizeLimitTest extends Specification {

    def mockBleConnection = Mock RxBleConnection

    MtuBasedPayloadSizeLimit objectUnderTest

    private void prepareObjectUnderTest(int gattWriteMtuOverhead) {
        objectUnderTest = new MtuBasedPayloadSizeLimit(mockBleConnection, gattWriteMtuOverhead)
    }

    @Unroll
    def "should return current MTU decreased by the write MTU overhead"() {

        given:
        prepareObjectUnderTest(gattWriteMtuOverhead)
        mockBleConnection.getMtu() >> currentMtu

        expect:
        objectUnderTest.getPayloadSizeLimit() == expectedValue

        where:
        currentMtu | gattWriteMtuOverhead | expectedValue
        10         | 2                    | 8
        2000       | 32                   | 1968
    }
}