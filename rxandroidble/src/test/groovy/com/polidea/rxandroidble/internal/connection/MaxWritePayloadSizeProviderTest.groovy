package com.polidea.rxandroidble.internal.connection

import spock.lang.Specification
import spock.lang.Unroll


class MaxWritePayloadSizeProviderTest extends Specification {

    def mockMtuProvider = Mock IntProvider

    MaxWritePayloadSizeProvider objectUnderTest

    private void prepareObjectUnderTest(int gattWriteMtuOverhead) {
        objectUnderTest = new MaxWritePayloadSizeProvider(mockMtuProvider, gattWriteMtuOverhead)
    }

    @Unroll
    def "should return current MTU decreased by the write MTU overhead"() {

        given:
        prepareObjectUnderTest(gattWriteMtuOverhead)
        mockMtuProvider.getValue() >> currentMtu

        expect:
        objectUnderTest.getValue() == expectedValue

        where:
        currentMtu | gattWriteMtuOverhead | expectedValue
        10         | 2                    | 8
        2000       | 32                   | 1968
    }
}