package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.Nullable
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.internal.scan.AndroidScanObjectsConverter
import com.polidea.rxandroidble2.internal.scan.EmulatedScanFilterMatcher
import com.polidea.rxandroidble2.internal.scan.InternalScanResultCreator
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

public class OperationScanApi21Test extends Specification {

    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    InternalScanResultCreator mockInternalScanResultCreator = Mock InternalScanResultCreator
    AndroidScanObjectsConverter mockAndroidScanObjectsCreator = Mock AndroidScanObjectsConverter
    EmulatedScanFilterMatcher mockEmulatedScanFilterMatecher = Mock EmulatedScanFilterMatcher
    ScanOperation objectUnderTest

    private prepareObjectUnderTest(ScanSettings scanSettings, ScanFilter[] offloadedScanFilters) {
        prepareObjectUnderTest(mockAdapterWrapper, scanSettings, offloadedScanFilters)
    }

    private prepareObjectUnderTest(RxBleAdapterWrapper rxBleAdapterWrapper, ScanSettings scanSettings, ScanFilter[] offloadedScanFilters) {
        objectUnderTest = new ScanOperationApi21(rxBleAdapterWrapper, mockInternalScanResultCreator, mockAndroidScanObjectsCreator, scanSettings, mockEmulatedScanFilterMatecher, offloadedScanFilters)
    }

    def "should call AndroidScanObjectsCreator and RxBleAdapterWrapper.startLeScan() when run() and release queue"() {

        given:
        ScanSettings scanSettings = Mock(ScanSettings)
        ScanFilter[] scanFilters = new ScanFilter[0];
        def mockAndroidScanSettings = Mock(android.bluetooth.le.ScanSettings)
        def mockAndroidScanFilters = new ArrayList<android.bluetooth.le.ScanFilter>()
        prepareObjectUnderTest(scanSettings, scanFilters)

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockAndroidScanObjectsCreator.toNativeFilters(scanFilters) >> mockAndroidScanFilters
        1 * mockAndroidScanObjectsCreator.toNativeSettings(scanSettings) >> mockAndroidScanSettings

        and:
        1 * mockAdapterWrapper.startLeScan(mockAndroidScanFilters, mockAndroidScanSettings, _)

        and:
        (1.._) * mockQueueReleaseInterface.release()
    }

    def "asObservable() should not emit error when BluetoothLeScannerCompat.startScan() will not throw"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothLeScannerCompat.startScan() will throw"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        mockAdapterWrapper.startLeScan(_, _, _) >> { _, _1, _2 -> throw new Throwable("test") }

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError(BleScanException)
    }

    @Unroll
    def "should call InternalScanResultCreator(int, android.bluetooth.le.ScanResult) and emit a value when ScanCallback will get a single scan result"() {

        given:
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        AtomicReference<ScanCallback> scanCallbackAtomicReference = captureScanCallback()
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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

    def "onScanFailed() should not result in exception being passed to RxJavaPlugins.onErrorHandler() if Observable was disposed."() {

        given:
        def capturedLeScanCallbackRef = captureScanCallback()
        def rxUnhandledExceptionRef = captureRxUnhandledExceptions()
        prepareObjectUnderTest(Mock(ScanSettings), null, null)
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        testSubscriber.dispose()

        when:
        capturedLeScanCallbackRef.get().onScanFailed(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)

        then:
        rxUnhandledExceptionRef.get() == null

        cleanup:
        RxJavaPlugins.setErrorHandler(null)

    }

    private AtomicReference<ScanCallback> captureScanCallback() {
        AtomicReference<ScanCallback> scanCallbackAtomicReference = new AtomicReference<>()
        mockAdapterWrapper.startLeScan(_, _, _) >> { List<ScanFilter> _, ScanSettings _1, ScanCallback scanCallback ->
            scanCallbackAtomicReference.set(scanCallback)
        }
        return scanCallbackAtomicReference
    }

    private static AtomicReference<Throwable> captureRxUnhandledExceptions() {
        AtomicReference<Throwable> unhandledExceptionAtomicReference = new AtomicReference<>()
        RxJavaPlugins.setErrorHandler({ throwable ->
            unhandledExceptionAtomicReference.set(throwable)
        })
        return unhandledExceptionAtomicReference
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
