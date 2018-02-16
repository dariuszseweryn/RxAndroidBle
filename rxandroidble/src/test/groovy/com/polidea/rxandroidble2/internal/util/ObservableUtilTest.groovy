package com.polidea.rxandroidble2.internal.util

import spock.lang.Specification

class ObservableUtilTest extends Specification {

    def "should pass through parameter on subscribe and not complete"() {
        given:
        String someObject = "someText"

        when:
        def testObserver = ObservableUtil.justOnNext(someObject).test()

        then:
        testObserver.assertValue(someObject)
        testObserver.assertNotComplete()
    }
}
