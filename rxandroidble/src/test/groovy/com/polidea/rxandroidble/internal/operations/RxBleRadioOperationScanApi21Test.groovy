package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.support.annotation.Nullable
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult
import com.polidea.rxandroidble.internal.scan.AndroidScanObjectsConverter
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import com.polidea.rxandroidble.scan.ScanFilter
import com.polidea.rxandroidble.scan.ScanSettings
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

public class RxBleRadioOperationScanApi21Test extends Specification {

    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper

    Semaphore mockSemaphore = Mock Semaphore

    TestSubscriber testSubscriber = new TestSubscriber()

    InternalScanResultCreator mockInternalScanResultCreator = Mock InternalScanResultCreator

    AndroidScanObjectsConverter mockAndroidScanObjectsCreator = Mock AndroidScanObjectsConverter

    EmulatedScanFilterMatcher mockEmulatedScanFilterMatecher = Mock EmulatedScanFilterMatcher

    RxBleRadioOperationScan objectUnderTest

    private prepareObjectUnderTest(ScanSettings scanSettings, ScanFilter[] offloadedScanFilters) {
        prepareObjectUnderTest(mockAdapterWrapper, scanSettings, offloadedScanFilters)
    }

    private prepareObjectUnderTest(RxBleAdapterWrapper rxBleAdapterWrapper, ScanSettings scanSettings, ScanFilter[] offloadedScanFilters) {
        objectUnderTest = new RxBleRadioOperationScanApi21(rxBleAdapterWrapper, mockInternalScanResultCreator, mockAndroidScanObjectsCreator, scanSettings, mockEmulatedScanFilterMatecher, offloadedScanFilters)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
    }

    def "should call AndroidScanObjectsCreator and RxBleAdapterWrapper.startLeScan() when run() and release radio"() {

        given:
        ScanSettings scanSettings = Mock(ScanSettings)
        ScanFilter[] scanFilters = new ScanFilter[0];
        def mockAndroidScanSettings = Mock(android.bluetooth.le.ScanSettings)
        def mockAndroidScanFilters = new ArrayList<android.bluetooth.le.ScanFilter>()
        prepareObjectUnderTest(scanSettings, scanFilters)

        when:
        objectUnderTest.run()

        then:
        1 * mockAndroidScanObjectsCreator.toNativeFilters(scanFilters) >> mockAndroidScanFilters
        1 * mockAndroidScanObjectsCreator.toNativeSettings(scanSettings) >> mockAndroidScanSettings

        and:
        1 * mockAdapterWrapper.startLeScan(mockAndroidScanFilters, mockAndroidScanSettings, _)

        and:
        (1.._) * mockSemaphore.release()
    }

    def "asObservable() should not emit error when BluetoothLeScannerCompat.startScan() will not throw"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothLeScannerCompat.startScan() will throw"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        mockAdapterWrapper.startLeScan(_, _, _) >> { _, _1, _2 -> throw new Throwable("test") }
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError(BleScanException)
    }

    @Unroll
    def "should call InternalScanResultCreator(int, android.bluetooth.le.ScanResult) and emit a value when ScanCallback will get a single scan result"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        AtomicReference<ScanCallback> scanCallbackAtomicReference = captureScanCallback()
        objectUnderTest.asObservable().subscribe(testSubscriber)
        objectUnderTest.run()
        def mockAndroidScanResult = Mock(ScanResult)
        def mockInternalScanResult = Mock(RxBleInternalScanResult)
        mockEmulatedScanFilterMatecher.matches(_) >> true

        when:
        scanCallbackAtomicReference.get().onScanResult(callbackType, mockAndroidScanResult)

        then:
        mockInternalScanResultCreator.create(callbackType, mockAndroidScanResult) >> mockInternalScanResult

        and:
        testSubscriber.assertValue(mockInternalScanResult)

        where:
        callbackType << [
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                0,
                9999
        ]
    }

    def "should call InternalScanResultCreator(int, android.bluetooth.le.ScanResult) and emit a values when ScanCallback will get a batched scan result"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        AtomicReference<ScanCallback> scanCallbackAtomicReference = captureScanCallback()
        objectUnderTest.asObservable().subscribe(testSubscriber)
        objectUnderTest.run()
        def mockAndroidScanResult = Mock(ScanResult)
        def mockInternalScanResult = Mock(RxBleInternalScanResult)
        def mockAndroidScanResult1 = Mock(ScanResult)
        def mockInternalScanResult1 = Mock(RxBleInternalScanResult)
        mockInternalScanResultCreator.create(mockAndroidScanResult) >> mockInternalScanResult
        mockInternalScanResultCreator.create(mockAndroidScanResult1) >> mockInternalScanResult1
        mockEmulatedScanFilterMatecher.matches(_) >> true

        when:
        scanCallbackAtomicReference.get().onBatchScanResults(Arrays.asList(
                mockAndroidScanResult,
                mockAndroidScanResult1
        ))

        then:
        testSubscriber.assertValues(mockInternalScanResult, mockInternalScanResult1)
    }

    @Unroll
    def "should call EmulatedScanFilterMatcher and emit value depending on returned values (if any matches) when ScanCallback will get a single scan result"() {
        given:
        def capturedLeScanCallbackRef = captureScanCallback()
        def mockInternalScanResult = Mock RxBleInternalScanResult
        mockInternalScanResultCreator.create(_, _) >> mockInternalScanResult
        prepareObjectUnderTest(Mock(ScanSettings), null)
        objectUnderTest.asObservable().subscribe(testSubscriber)
        objectUnderTest.run()

        when:
        capturedLeScanCallbackRef.get().onScanResult(0, Mock(ScanResult))

        then:
        1 * mockEmulatedScanFilterMatecher.matches(mockInternalScanResult) >> emulatedFiltersMatch
        testSubscriber.assertValueCount(valuesCount)

        where:
        emulatedFiltersMatch | valuesCount
        true                 | 1
        false                | 0
    }

    @Unroll
    def "should call EmulatedScanFilterMatcher and emit value depending on returned values (if any matches) when ScanCallback will get a batched scan result"() {
        given:
        def capturedLeScanCallbackRef = captureScanCallback()
        def mockInternalScanResult = Mock RxBleInternalScanResult
        mockInternalScanResultCreator.create(_) >> mockInternalScanResult
        prepareObjectUnderTest(Mock(ScanSettings), null)
        objectUnderTest.asObservable().subscribe(testSubscriber)
        objectUnderTest.run()

        when:
        capturedLeScanCallbackRef.get().onBatchScanResults(Arrays.asList(Mock(ScanResult), Mock(ScanResult)))

        then:
        2 * mockEmulatedScanFilterMatecher.matches(_) >>> emulatedFiltersMatch
        testSubscriber.assertValueCount(valuesCount)

        where:
        emulatedFiltersMatch | valuesCount
        [false, false]       | 0
        [true, false]        | 1
        [false, true]        | 1
        [true, true]         | 2
    }

    def "should call RxBleAdapterWrapper.stopScan() when stopScan() will be called before RxBleAdapterWrapper.startScan() will return true"() {

        /*
        [D.S] The idea behind is:
        1. RxBleRadioOperationScanTest is started but RxBleAdapterWrapper.startLeScan() doesn't return yet
        2. RxBleRadioOperationScanTest is stopped
        3. RxBleAdapterWrapper.startLeScan() returns true
        Creating elegant tests for threading issues is dirty :/
         */

        given:
        Semaphore startScanReturnSemaphore = new Semaphore(0)
        Semaphore stopScanSemaphore = new Semaphore(0)
        def mockRxBleAdapterWrapper = new MockBleAdapterWrapper(null, startScanReturnSemaphore)
        prepareObjectUnderTest(mockRxBleAdapterWrapper, Mock(ScanSettings), null, null)

        new Thread(new Runnable() {

            @Override
            void run() {
                stopScanSemaphore.release()
                objectUnderTest.run()
            }
        }).start()

        stopScanSemaphore.acquire()
        Thread.sleep(500)
        objectUnderTest.stop()
        mockRxBleAdapterWrapper.numberOfTimesStopCalled = 0
        Thread.sleep(500)

        when:
        startScanReturnSemaphore.release()
        Thread.sleep(1000)

        then:
        mockRxBleAdapterWrapper.numberOfTimesStopCalled == 1
    }

    private AtomicReference<ScanCallback> captureScanCallback() {
        AtomicReference<ScanCallback> scanCallbackAtomicReference = new AtomicReference<>()
        mockAdapterWrapper.startLeScan(_, _, _) >> { List<ScanFilter> _, ScanSettings _1, ScanCallback scanCallback ->
            scanCallbackAtomicReference.set(scanCallback)
        }
        return scanCallbackAtomicReference
    }

    private static class MockBleAdapterWrapper extends RxBleAdapterWrapper {

        Semaphore acquireBeforeReturnStartScan = null

        int numberOfTimesStopCalled = 0

        MockBleAdapterWrapper(@Nullable BluetoothAdapter bluetoothAdapter, Semaphore acquireBeforeReturnStartScan) {
            super(bluetoothAdapter)
            this.acquireBeforeReturnStartScan = acquireBeforeReturnStartScan
        }

        @Override
        BluetoothDevice getRemoteDevice(String macAddress) {
            return null
        }

        @Override
        boolean hasBluetoothAdapter() {
            return true
        }

        @Override
        boolean isBluetoothEnabled() {
            return true
        }

        @Override
        void startLeScan(List<android.bluetooth.le.ScanFilter> scanFilters, android.bluetooth.le.ScanSettings scanSettings, ScanCallback scanCallback) {
            acquireBeforeReturnStartScan.acquire()
            Thread.sleep(500)
        }

        @Override
        void stopLeScan(ScanCallback scanCallback) {
            numberOfTimesStopCalled++
        }
    }
}
