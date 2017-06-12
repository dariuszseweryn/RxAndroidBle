package com.polidea.rxandroidble.internal.util

import spock.lang.Specification

class UUIDUtilTest extends Specification {

    UUIDUtil objectUnderTest = new UUIDUtil()

    def "should extract 32-bit UUIDs"() {

        given:
        def advertisement = [ 0x05, 0x04, 0xef, 0xbe, 0xad,  0xde] as byte[]

        when:
        def result = objectUnderTest.extractUUIDs(advertisement)

        then:
        result.get(0).toString() == "deadbeef-0000-1000-8000-00805f9b34fb"
    }
}
