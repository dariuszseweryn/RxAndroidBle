package com.polidea.rxandroidble2.internal.connection

import com.polidea.rxandroidble2.RxBleConnection
import spock.lang.Specification
import spock.lang.Unroll


class MtuBasedPayloadSizeLimitTest extends Specification {

    def mockBleConnection = Mock RxBleConnection

    MtuBasedPayloadSizeLimit objectUnderTest

    private void prepareObjectUnderTest(int gattWriteMtuOverhead, int maxAttributeLength) {
        objectUnderTest = new MtuBasedPayloadSizeLimit(mockBleConnection, gattWriteMtuOverhead, maxAttributeLength)
    }

    @Unroll
    def "should return current MTU decreased by the write MTU overhead but no more than the maxAttributeLength"() {

        given:
        prepareObjectUnderTest(gattWriteMtuOverhead, maxAttributeLength)
        mockBleConnection.getMtu() >> currentMtu

        expect:
        objectUnderTest.getPayloadSizeLimit() == expectedValue

        where:
        currentMtu | gattWriteMtuOverhead | maxAttributeLength | expectedValue
        10         | 2                    | 2000               | 8
        2000       | 32                   | 2000               | 1968
        10         | 2                    | 512                | 8
        2000       | 32                   | 512                | 512
    }
}
