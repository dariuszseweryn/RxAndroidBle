package com.polidea.rxandroidble3.internal.scan

import spock.lang.Specification
import spock.lang.Unroll

class EmulatedScanFilterMatcherTest extends Specification {

    private EmulatedScanFilterMatcher objectUnderTest

    @Unroll
    def "should return proper value when called isEmpty()"() {

        given:
        objectUnderTest = new EmulatedScanFilterMatcher(scanFilters)

        expect:
        objectUnderTest.isEmpty() == result

        where:
        result | scanFilters
        true   | null
        true   | mockScanFilters()
        true   | mockScanFilters(true)
        false  | mockScanFilters(false)
        true   | mockScanFilters(true, true)
        false  | mockScanFilters(true, false)
        false  | mockScanFilters(false, true)
        true   | mockScanFilters(true, true, true)
        false  | mockScanFilters(false, true, true)
        false  | mockScanFilters(true, false, true)
        false  | mockScanFilters(true, true, false)
    }

    private def mockScanFilters(Boolean... empty) {
        ScanFilterInterface[] scanFilterInterfaces = new ScanFilterInterface[empty.length]
        for (int i = 0; i < empty.length; i++) {
            def scanFilter = Mock(ScanFilterInterface)
            scanFilter.isAllFieldsEmpty() >> empty[i]
            scanFilterInterfaces[i] = scanFilter
        }
        return scanFilterInterfaces
    }
}