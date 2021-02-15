package com.polidea.rxandroidble2.internal.scan

import com.polidea.rxandroidble2.exceptions.BleScanException
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.TestScheduler
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import spock.lang.Unroll

class ScanPreconditionsVerifierApi24Test extends Specification {

    private static final boolean[] TRUE_FALSE = [true, false]

    private TestScheduler testScheduler = new TestScheduler()
    private ScanPreconditionsVerifierApi18 mockScanPreconditionVerifierApi18 = Mock ScanPreconditionsVerifierApi18
    private ScanPreconditionsVerifierApi24 objectUnderTest = new ScanPreconditionsVerifierApi24(mockScanPreconditionVerifierApi18, testScheduler)

    def setup() {
        testScheduler.advanceTimeTo(1, TimeUnit.MINUTES)
    }

    @Unroll
    def "should perform checks in proper order"() {

        given:
        Scheduler scheduler = Mock Scheduler
        objectUnderTest = new ScanPreconditionsVerifierApi24(mockScanPreconditionVerifierApi18, scheduler)
        scheduler.now(TimeUnit.MILLISECONDS) >> TimeUnit.SECONDS.toMillis(5)

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        1 * mockScanPreconditionVerifierApi18.verify(checkLocationServices)

        then:
        thrown BleScanException

        where:
        checkLocationServices << [true, false]
    }

    @Unroll
    def "should proxy exception thrown by ScanPreconditionsVerifierApi18"() {

        given:
        def testException = new BleScanException(BleScanException.UNKNOWN_ERROR_CODE, new Date())
        mockScanPreconditionVerifierApi18.verify(_) >> { throw testException }

        when:
        objectUnderTest.verify(checkLocationServices)

        then:
        thrown BleScanException

        where:
        checkLocationServices << [true, false]
    }

    @Unroll
    def "should throw BleScanException.UNDOCUMENTED_SCAN_THROTTLE if called 6th time during a 30 second window"() {

        given:
        objectUnderTest.verify(checkLocationServices0)
        objectUnderTest.verify(checkLocationServices1)
        objectUnderTest.verify(checkLocationServices2)
        objectUnderTest.verify(checkLocationServices3)
        objectUnderTest.verify(checkLocationServices4)

        when:
        objectUnderTest.verify(checkLocationServices5)

        then:
        BleScanException e = thrown BleScanException
        e.getReason() == BleScanException.UNDOCUMENTED_SCAN_THROTTLE
        e.getRetryDateSuggestion() == (new Date(testScheduler.now(TimeUnit.MILLISECONDS) + TimeUnit.SECONDS.toMillis(30)))

        where:
        [
                checkLocationServices0,
                checkLocationServices1,
                checkLocationServices2,
                checkLocationServices3,
                checkLocationServices4,
                checkLocationServices5
        ] << [TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE].combinations()

    }

    @Unroll
    def "should not throw BleScanException.UNDOCUMENTED_SCAN_THROTTLE if called 6th time after a 30 second window"() {

        given:
        objectUnderTest.verify(checkLocationServices0)
        objectUnderTest.verify(checkLocationServices1)
        objectUnderTest.verify(checkLocationServices2)
        objectUnderTest.verify(checkLocationServices3)
        objectUnderTest.verify(checkLocationServices4)
        testScheduler.advanceTimeBy(31, TimeUnit.SECONDS)

        when:
        objectUnderTest.verify(checkLocationServices5)

        then:
        notThrown Throwable

        where:
        [
                checkLocationServices0,
                checkLocationServices1,
                checkLocationServices2,
                checkLocationServices3,
                checkLocationServices4,
                checkLocationServices5
        ] << [TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE, TRUE_FALSE].combinations()
    }
}