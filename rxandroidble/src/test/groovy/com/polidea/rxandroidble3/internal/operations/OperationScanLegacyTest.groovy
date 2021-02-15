package com.polidea.rxandroidble3.internal.operations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.annotation.Nullable
import com.polidea.rxandroidble3.exceptions.BleScanException
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble3.internal.util.RxBleAdapterWrapper
import com.polidea.rxandroidble3.internal.util.ScanRecordParser
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

public class OperationScanLegacyTest extends Specification {

    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper
    ScanRecordParser mockScanRecordParser = Mock ScanRecordParser
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    LegacyScanOperation objectUnderTest

    def setup() {
        prepareObjectUnderTest(mockAdapterWrapper)
    }

    def prepareObjectUnderTest(RxBleAdapterWrapper adapterWrapper) {
        objectUnderTest = new LegacyScanOperation(null, adapterWrapper, mockScanRecordParser)
    }

    def "should call RxBleAdapterWrapper.startScan() when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockAdapterWrapper.startLegacyLeScan(_) >> true
    }

    def "asObservable() should not emit error when RxBleAdapterWrapper.startScan() returns true"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> true

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when RxBleAdapterWrapper.startScan() returns false"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> false

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError(BleScanException)
    }

    def "asObservable() should emit values when BleScanCallback will get scan results"() {

        given:
        AtomicReference<BluetoothAdapter.LeScanCallback> leScanCallbackAtomicReference = new AtomicReference<>()
        mockAdapterWrapper.startLegacyLeScan({ BluetoothAdapter.LeScanCallback leScanCallback ->
            leScanCallbackAtomicReference.set(leScanCallback)
            true
        }) >> true

        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        def scannedDevice = mockBluetoothDevice
        def scannedBytes = new byte[5]
        def scannedRssi = 5

        when:
        leScanCallbackAtomicReference.get().onLeScan(scannedDevice, scannedRssi, scannedBytes)

        then:
        testSubscriber.assertAnyOnNext {
            it.bluetoothDevice == scannedDevice &&
                    it.rssi == scannedRssi &&
                    it.scanRecord == scannedBytes
        }
    }

    @Unroll
    def "should release queue after run()"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> startScanResult

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        (1.._) * mockQueueReleaseInterface.release()

        where:
        startScanResult << [true, false]
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