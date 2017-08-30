package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.support.annotation.Nullable
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

public class OperationScanApi18Test extends Specification {

    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper

    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface

    TestSubscriber testSubscriber = new TestSubscriber()

    InternalScanResultCreator mockInternalScanResultCreator = Mock InternalScanResultCreator

    EmulatedScanFilterMatcher mockEmulatedScanFilterMatcher = Mock EmulatedScanFilterMatcher

    ScanOperationApi18 objectUnderTest

    def setup() {
        prepareObjectUnderTest(mockAdapterWrapper)
    }

    def prepareObjectUnderTest(RxBleAdapterWrapper adapterWrapper) {
        objectUnderTest = new ScanOperationApi18(adapterWrapper, mockInternalScanResultCreator, mockEmulatedScanFilterMatcher)
    }

    def "asObservable() should not emit error when RxBleAdapterWrapper.startScan() returns true"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> true

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when RxBleAdapterWrapper.startScan() returns false"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> false
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError(BleScanException)
    }

    def "should call InternalScanResultCreator and emit value when BleScanCallback will get scan result"() {

        given:
        AtomicReference<BluetoothAdapter.LeScanCallback> capturedLeScanCallbackRef = captureScanCallback()
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
        def scannedDevice = Mock(BluetoothDevice)
        def scannedBytes = new byte[5]
        def scannedRssi = 5
        def mockInternalScanResult = Mock(RxBleInternalScanResult)
        mockEmulatedScanFilterMatcher.matches(_) >> true

        when:
        capturedLeScanCallbackRef.get().onLeScan(scannedDevice, scannedRssi, scannedBytes)

        then:
        mockInternalScanResultCreator.create(scannedDevice, scannedRssi, scannedBytes) >> mockInternalScanResult

        and:
        testSubscriber.assertValue(mockInternalScanResult)
    }

    @Unroll
    def "should call EmulatedScanFilterMatches.matches() and emit value depending on returned values (if any matches)"() {
        given:
        def capturedLeScanCallbackRef = captureScanCallback()
        def mockInternalScanResult = Mock RxBleInternalScanResult
        mockInternalScanResultCreator.create(_, _, _) >> mockInternalScanResult
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        when:
        capturedLeScanCallbackRef.get().onLeScan(Mock(BluetoothDevice), 0, new byte[0])

        then:
        1 * mockEmulatedScanFilterMatcher.matches(mockInternalScanResult) >> matches
        testSubscriber.assertValueCount(count)

        where:
        matches | count
        false   | 0
        true    | 1
    }

    @Unroll
    def "should release queue after run()"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> startScanResult

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        (1.._) * mockQueueReleaseInterface.release()

        where:
        startScanResult << [true, false]
    }

    private AtomicReference<BluetoothAdapter.LeScanCallback> captureScanCallback() {
        AtomicReference<BluetoothAdapter.LeScanCallback> leScanCallbackAtomicReference = new AtomicReference<>()
        mockAdapterWrapper.startLegacyLeScan({ BluetoothAdapter.LeScanCallback leScanCallback ->
            leScanCallbackAtomicReference.set(leScanCallback)
            true
        }) >> true
        return leScanCallbackAtomicReference;
    }

    private class MockBleAdapterWrapper extends RxBleAdapterWrapper {

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
        boolean startLegacyLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
            acquireBeforeReturnStartScan.acquire()
            Thread.sleep(500)
            return true
        }

        @Override
        void stopLegacyLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
            numberOfTimesStopCalled++
        }
    }
}
