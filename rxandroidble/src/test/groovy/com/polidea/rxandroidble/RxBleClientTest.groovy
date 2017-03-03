package com.polidea.rxandroidble

import java.util.concurrent.Executors

import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_CANNOT_START
import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_DISABLED
import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_NOT_AVAILABLE
import static com.polidea.rxandroidble.exceptions.BleScanException.LOCATION_PERMISSION_MISSING
import static com.polidea.rxandroidble.exceptions.BleScanException.LOCATION_SERVICES_DISABLED

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.RxBleDeviceProvider
import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.RxBleRadioOperation
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan
import com.polidea.rxandroidble.internal.util.UUIDUtil
import rx.Observable
import rx.Scheduler
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import spock.lang.Specification
import spock.lang.Unroll

class RxBleClientTest extends Specification {

    FlatRxBleRadio rxBleRadio = new FlatRxBleRadio()
    RxBleClient objectUnderTest
    Context contextMock = Mock Context
    UUIDUtil uuidParserSpy = Spy UUIDUtil
    MockRxBleAdapterWrapper bleAdapterWrapperSpy = Spy MockRxBleAdapterWrapper
    MockRxBleAdapterStateObservable adapterStateObservable = Spy MockRxBleAdapterStateObservable
    MockLocationServicesStatus locationServicesStatusMock = new MockLocationServicesStatus()
    RxBleDeviceProvider mockDeviceProvider = Mock RxBleDeviceProvider
    private static someUUID = UUID.randomUUID()
    private static otherUUID = UUID.randomUUID()

    def setup() {
        contextMock.getApplicationContext() >> contextMock
        mockDeviceProvider.getBleDevice(_ as String) >> { String macAddress ->
            def device = Mock(RxBleDevice)
            device.macAddress >> macAddress
            device
        }
        objectUnderTest = new RxBleClientImpl(
                bleAdapterWrapperSpy,
                rxBleRadio,
                adapterStateObservable.asObservable(),
                uuidParserSpy,
                locationServicesStatusMock,
                mockDeviceProvider,
                Executors.newSingleThreadExecutor()
        )
    }

    def "should return bonded devices"() {
        given:
        bluetoothDeviceBonded("AA:AA:AA:AA:AA:AA")
        bluetoothDeviceBonded("BB:BB:BB:BB:BB:BB")
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 0, scanRecord: [] as byte[]
        bluetoothDeviceDiscovered deviceMac: "BB:BB:BB:BB:BB:BB", rssi: 50, scanRecord: [] as byte[]

        when:
        def results = objectUnderTest.getBondedDevices()

        then:
        assert results.size() == 2
    }

    def "should start BLE scan if subscriber subscribes to the scan observable"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)

        then:
        1 * bleAdapterWrapperSpy.startLeScan(_) >> true
    }

    def "should not start scan until observable is subscribed"() {
        when:
        objectUnderTest.scanBleDevices(null)

        then:
        0 * bleAdapterWrapperSpy.startLeScan(_)
    }

    def "should stop scan after subscriber is unsubscribed from scan observable"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLeScan(_) >> true

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)
        scanSubscription.unsubscribe()

        then:
        1 * bleAdapterWrapperSpy.stopLeScan(_)
    }

    def "should stop and unsubscribe in case of scan throws exception"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLeScan(_) >> { throw new NullPointerException() }

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)

        then:
        1 * bleAdapterWrapperSpy.stopLeScan(_)

        and:
        scanSubscription.isUnsubscribed()
    }

    def "should stop scan after all subscribers are unsubscribed"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        TestSubscriber secondSubscriber = new TestSubscriber<>()

        when:
        def firstSubscription = objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)
        def secondSubscription = objectUnderTest.scanBleDevices(null).subscribe(secondSubscriber)
        firstSubscription.unsubscribe()
        secondSubscription.unsubscribe()

        then:
        1 * bleAdapterWrapperSpy.stopLeScan(_)
    }

    def "should not stop scan if not all subscribers are unsubscribed"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        TestSubscriber secondSubscriber = new TestSubscriber<>()

        when:
        def firstSubscription = objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)
        objectUnderTest.scanBleDevices(null).subscribe(secondSubscriber)
        firstSubscription.unsubscribe()
        // keep second subscriber subscribed

        then:
        0 * bleAdapterWrapperSpy.stopLeScan(_)
    }

    def "should start scan only once even if observable has more subscribers"() {
        when:
        def scanObservable = objectUnderTest.scanBleDevices(null)
        scanObservable.subscribe()
        scanObservable.subscribe()

        then:
        1 * bleAdapterWrapperSpy.startLeScan(_) >> true
    }

    def "should not replay scan results to second observer if it subscribed after scan emission"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        TestSubscriber secondSubscriber = new TestSubscriber<>()
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 0, scanRecord: [] as byte[]
        bluetoothDeviceDiscovered deviceMac: "BB:BB:BB:BB:BB:BB", rssi: 50, scanRecord: [] as byte[]

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)
        objectUnderTest.scanBleDevices(null).subscribe(secondSubscriber)

        then:
        firstSubscriber.assertValueCount 2

        and:
        secondSubscriber.assertValueCount 0
    }

    def "should emit BleScanException if bluetooth scan failed to start"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLeScan(_) >> false

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_CANNOT_START
        }
    }

    def "should emit BleScanException if bluetooth was disabled during scan"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)
        adapterStateObservable.disableBluetooth()

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_DISABLED
        }
    }

    def "should emit BleScanException if bluetooth has been disabled scan"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> true
        bleAdapterWrapperSpy.isBluetoothEnabled() >> false

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_DISABLED
        }
    }

    def "should emit error if bluetooth is not available"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> false

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_NOT_AVAILABLE
        }
    }

    def "should emit BleScanException if location permission was not granted"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        locationServicesStatusMock.isLocationPermissionOk = false

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == LOCATION_PERMISSION_MISSING
        }
    }

    @Unroll
    def "should emit BleScanException if location services are not ok (LocationProviderOk:#providerOk)"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        locationServicesStatusMock.isLocationProviderOk = providerOk

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:

        if (!providerOk)
            firstSubscriber.assertError { BleScanException exception -> exception.reason == LOCATION_SERVICES_DISABLED }
        else {
            firstSubscriber.assertNoErrors()
        }

        where:
        providerOk << [true, false]
    }

    @Unroll
    def "should emit devices only if matching filter (#description)"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        addressList.each { bluetoothDeviceDiscovered deviceMac: it, rssi: 0, scanRecord: [] as byte[] }
        uuidParserSpy.extractUUIDs(_) >>> publicServices

        when:
        objectUnderTest.scanBleDevices(filter as UUID[]).subscribe(testSubscriber)

        then:
        testSubscriber.assertValueCount expectedDevices.size()
        testSubscriber.assertScanRecordsByMacWithOrder(expectedDevices)

        where:
        addressList                                | publicServices                       | filter                | expectedDevices                            | description
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID]]                         | []                    | ["AA:AA:AA:AA:AA:AA"]                      | 'Empty filter, one public service'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[someUUID, someUUID]]               | []                    | ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | 'Empty filter, one public service, two devices'
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID, otherUUID]]              | []                    | ["AA:AA:AA:AA:AA:AA"]                      | 'Empty filter, two public services'
        ["AA:AA:AA:AA:AA:AA"]                      | [[]]                                 | []                    | ["AA:AA:AA:AA:AA:AA"]                      | 'Empty filter, no public services'
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID]]                         | null                  | ["AA:AA:AA:AA:AA:AA"]                      | 'No filter, one public service'
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID, otherUUID]]              | null                  | ["AA:AA:AA:AA:AA:AA"]                      | 'No filter, two public services'
        ["AA:AA:AA:AA:AA:AA"]                      | [[]]                                 | null                  | ["AA:AA:AA:AA:AA:AA"]                      | 'No filter, no public services'
        ["AA:AA:AA:AA:AA:AA"]                      | [[]]                                 | [someUUID]            | []                                         | 'One filter, device without public services'
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID]]                         | [someUUID]            | ["AA:AA:AA:AA:AA:AA"]                      | 'One filter, device with matching public service'
        ["AA:AA:AA:AA:AA:AA"]                      | [[someUUID, otherUUID]]              | [someUUID]            | ["AA:AA:AA:AA:AA:AA"]                      | 'One filter, device with matching public service and one more not matching'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[someUUID], [someUUID]]             | [someUUID]            | ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | 'One filter, two devices, both with one matching service'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[], [someUUID]]                     | [someUUID]            | ["AA:AA:AA:AA:AA:BB"]                      | 'One filter, two devices, one without public services, second with matching service'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[], []]                             | [someUUID, otherUUID] | []                                         | 'Two filtered UUIDs, two devices without public services'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[someUUID], [otherUUID]]            | [someUUID, otherUUID] | []                                         | 'Two filtered UUIDs, two devices, both matches only by one service'
        ["AA:AA:AA:AA:AA:AA", "AA:AA:AA:AA:AA:BB"] | [[someUUID, otherUUID], [otherUUID]] | [someUUID, otherUUID] | ["AA:AA:AA:AA:AA:AA"]                      | 'Two filtered UUIDs, two devices, one matches by both services, second matching only partially'
    }

    def "should emit device if has matching public service plus some more not defined in filter"() {
        given:
        def filter = UUID.randomUUID();
        def secondUUID = UUID.randomUUID();
        def thirdUUID = UUID.randomUUID();
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 0, scanRecord: [] as byte[]
        uuidParserSpy.extractUUIDs(_) >> [filter, secondUUID, thirdUUID]

        when:
        objectUnderTest.scanBleDevices([filter] as UUID[]).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertValueCount 1
    }

    def "should release radio after scan is configured"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        rxBleRadio.semaphore.isReleased()

    }

    def "should emit result with all parameters"() {
        given:
        TestSubscriber subscriber = new TestSubscriber<>()
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 10, scanRecord: [1, 2, 3] as byte[]

        when:
        objectUnderTest.scanBleDevices(null).subscribe(subscriber)

        then:
        subscriber.assertScanRecord(10, "AA:AA:AA:AA:AA:AA", [1, 2, 3] as byte[])
    }

    def bluetoothDeviceDiscovered(Map scanData) {
        def mock = Mock(BluetoothDevice)
        mock.getAddress() >> scanData['deviceMac']
        bleAdapterWrapperSpy.addScanResult(mock, scanData['rssi'], scanData['scanRecord'])
    }

    def bluetoothDeviceBonded(String address) {
        def mock = Mock(BluetoothDevice)
        mock.getAddress() >> address
        mock.hashCode() >> address.hashCode()
        bleAdapterWrapperSpy.addBondedDevice(mock);
    }

    /**
     * This test reproduces issue: https://github.com/Polidea/RxAndroidBle/issues/17
     * It first calls startLeScan method which takes 100ms to finish
     * then it calls stopLeScan after 50ms but before startLeScan returns
     */
    def "should call stopLeScan only after startLeScan finishes and returns true"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLeScan(_) >> true
        RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(null, bleAdapterWrapperSpy, null) {

            @Override
            synchronized void protectedRun() {
                // simulate delay when starting scan
                Thread.sleep(100)
                super.protectedRun()
            }
        }
        Thread stopScanThread = new Thread() {

            @Override
            void run() {
                //unsubscribe before scan starts
                Thread.sleep(50)
                scanOperation.stop();
            }
        }
        def scanTestRadio = new RxBleRadio() {

            def Scheduler scheduler() {
                return Schedulers.immediate()
            }

            @Override
            def <T> Observable<T> queue(RxBleRadioOperation<T> rxBleRadioOperation) {
                return rxBleRadioOperation
                        .asObservable()
                        .doOnSubscribe({
                    stopScanThread.start()
                    Thread runOperationThread = new Thread() {

                        @Override
                        void run() {
                            def semaphore = new MockSemaphore()
                            rxBleRadioOperation.setRadioBlockingSemaphore(semaphore)
                            semaphore.acquire()
                            rxBleRadioOperation.run()
                        }
                    }
                    runOperationThread.start()
                })
            }
        }

        when:
        scanTestRadio.queue(scanOperation).subscribe(testSubscriber)
        waitForThreadsToCompleteWork()

        then:
        1 * bleAdapterWrapperSpy.startLeScan(_)

        then:
        1 * bleAdapterWrapperSpy.stopLeScan(_)
    }

    public waitForThreadsToCompleteWork() {
        Thread.sleep(200) // Nasty :<
        true
    }
}
