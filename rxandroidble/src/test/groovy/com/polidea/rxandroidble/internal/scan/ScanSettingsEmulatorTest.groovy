package com.polidea.rxandroidble.internal.scan

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.scan.ScanCallbackType
import com.polidea.rxandroidble.scan.ScanSettings
import java.util.concurrent.TimeUnit
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

public class ScanSettingsEmulatorTest extends Specification {

    private TestScheduler testScheduler = new TestScheduler()

    private TestSubscriber testSubscriber = new TestSubscriber()

    private ScanSettingsEmulator objectUnderTest = new ScanSettingsEmulator(testScheduler)

    @Unroll
    def "should emit only the first scan result or after a certain time from .emulateScanCallback(FIRST_MATCH)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("1")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult0)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
        }

        when:
        testScheduler.advanceTimeBy(delayTime, TimeUnit.MILLISECONDS)
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(valueCount)

        where:
        delayTime | valueCount
        1000      | 1
        9999      | 1
        10001     | 2
    }

    @Unroll
    def "should emit the first scan result of all devices scanned from .emulateScanCallback(FIRST_MATCH)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("2")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult0)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
        }

        when:
        testScheduler.advanceTimeBy(delayTime, TimeUnit.MILLISECONDS)
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(valueCount)

        where:
        delayTime | valueCount
        1000      | 2
        9999      | 2
        10001     | 2
    }

    def "should emit the first scan result of one device scanned from .emulateScanCallback(MATCH_LOST)"() {

        given:
        def scanResult = mockScan("0")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult)

        then:
        testSubscriber.assertNoValues()

        when:
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }
    }

    @Unroll
    def "should emit only the last scan result and after a certain time from .emulateScanCallback(MATCH_LOST)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("1")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST)).subscribe(testSubscriber)
        subject.onNext(scanResult0)

        when:
        testScheduler.advanceTimeBy(delayTime, TimeUnit.MILLISECONDS)

        then:
        testSubscriber.assertValueCount(0)

        when:
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(0)

        when:
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult1.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }

        where:
        delayTime << [1000, 9999]
    }

    def "should emit the last scan result of all devices scanned from .emulateScanCallback(MATCH_LOST)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("2")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_MATCH_LOST)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult0)

        then:
        testSubscriber.assertValueCount(0)

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(0)

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(2)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult1.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }
    }

    def "should emit the first and last scan result when scanned from .emulateScanCallback(FIRST_MATCH | MATCH_LOST)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("1")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult0)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
        }

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        when:
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(1)

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(2)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult1.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }
    }

    def "should emit the first and last scan result for all devices when scanned from .emulateScanCallback(FIRST_MATCH | MATCH_LOST)"() {

        given:
        def scanResult0 = mockScan("1")
        def scanResult1 = mockScan("2")
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH | ScanSettings.CALLBACK_TYPE_MATCH_LOST)).subscribe(testSubscriber)

        when:
        subject.onNext(scanResult0)

        then:
        testSubscriber.assertValueCount(1)

        and:
        testSubscriber.assertOneMatches { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
        }

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(1)

        when:
        subject.onNext(scanResult1)

        then:
        testSubscriber.assertValueCount(2)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult1.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH
        }

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(3)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult0.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }

        when:
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        then:
        testSubscriber.assertValueCount(4)

        and:
        testSubscriber.assertAnyOnNext { RxBleInternalScanResult it ->
            it.getBluetoothDevice() == scanResult1.getBluetoothDevice() && it.getScanCallbackType() == ScanCallbackType.CALLBACK_TYPE_MATCH_LOST
        }
    }

    def "should not alter the input if called from .emulateScanCallback(ALL_MATCHES)"() {

        given:
        PublishSubject<RxBleInternalScanResult> subject = PublishSubject.create()

        expect:
        subject.compose(objectUnderTest.emulateCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)) == subject
    }

    private def mockScan(String address) {
        def scanResult = Mock(RxBleInternalScanResult)
        def device = Mock(BluetoothDevice)
        device.getAddress() >> address
        scanResult.getBluetoothDevice() >> device
        return scanResult
    }
}