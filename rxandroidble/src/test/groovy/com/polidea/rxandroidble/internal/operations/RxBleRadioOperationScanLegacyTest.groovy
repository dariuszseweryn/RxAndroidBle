package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.support.annotation.Nullable
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.RadioReleaseInterface
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import com.polidea.rxandroidble.internal.util.UUIDUtil
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

public class RxBleRadioOperationScanLegacyTest extends Specification {

    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper
    UUIDUtil mockUUIDUtil = Mock UUIDUtil
    RadioReleaseInterface mockRadioReleaseInterface = Mock RadioReleaseInterface
    TestSubscriber testSubscriber = new TestSubscriber()
    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice

    RxBleRadioOperationScanLegacy objectUnderTest

    def setup() {
        prepareObjectUnderTest(mockAdapterWrapper)
    }

    def prepareObjectUnderTest(RxBleAdapterWrapper adapterWrapper) {
        objectUnderTest = new RxBleRadioOperationScanLegacy(null, adapterWrapper, mockUUIDUtil)
    }

    def "should call RxBleAdapterWrapper.startScan() when run()"() {

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockAdapterWrapper.startLegacyLeScan(_) >> true
    }

    def "asObservable() should not emit error when RxBleAdapterWrapper.startScan() returns true"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> true

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when RxBleAdapterWrapper.startScan() returns false"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> false

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

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

        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)
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
    def "should release radio after run()"() {

        given:
        mockAdapterWrapper.startLegacyLeScan(_) >> startScanResult

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        (1.._) * mockRadioReleaseInterface.release()

        where:
        startScanResult << [true, false]
    }

    // [DS 23.05.2017] TODO: cannot make it to work again although it should not be a problem because of using Emitter.setCancellation
//    def "should call RxBleAdapterWrapper.stopScan() when scan will be unsubscribed  before RxBleAdapterWrapper.startScan() will return true"() {
//
//        /*
//        [D.S] The idea behind is:
//        1. RxBleRadioOperationScan is started but RxBleAdapterWrapper.startScan() doesn't return yet
//        2. RxBleRadioOperationScan is stopped
//        3. RxBleAdapterWrapper.startScan() returns true
//        Creating elegant tests for threading issues is dirty :/
//         */
//
//        given:
//        Semaphore startScanReturnSemaphore = new Semaphore(0)
//        Semaphore stopScanSemaphore = new Semaphore(0)
//        def mockBleAdapterWrapper = new MockBleAdapterWrapper(null, startScanReturnSemaphore)
//        prepareObjectUnderTest(mockBleAdapterWrapper)
//
//        new Thread(new Runnable() {
//
//            @Override
//            void run() {
//                stopScanSemaphore.release()
//                objectUnderTest.run()
//            }
//        }).start()
//
//        stopScanSemaphore.acquire()
//        Thread.sleep(500)
//        objectUnderTest.stop()
//        mockBleAdapterWrapper.numberOfTimesStopCalled = 0
//        Thread.sleep(500)
//
//        when:
//        startScanReturnSemaphore.release()
//        Thread.sleep(1000)
//
//        then:
//        mockBleAdapterWrapper.numberOfTimesStopCalled == 1
//    }

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