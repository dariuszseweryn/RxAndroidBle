package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.operations.Operation
import com.polidea.rxandroidble.internal.util.ClientStateObservable
import dagger.Lazy
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult
import com.polidea.rxandroidble.internal.scan.InternalToExternalScanResultConverter
import com.polidea.rxandroidble.internal.scan.ScanSetup
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilder
import com.polidea.rxandroidble.scan.ScanSettings
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScan
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
import com.polidea.rxandroidble.internal.util.UUIDUtil
import rx.Observable
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

class RxBleClientTest extends Specification {

    FlatRxBleRadio rxBleRadio = new FlatRxBleRadio()
    RxBleClient objectUnderTest
    Context contextMock = Mock Context
    UUIDUtil uuidParserSpy = Spy UUIDUtil
    MockRxBleAdapterWrapper bleAdapterWrapperSpy = Spy MockRxBleAdapterWrapper
    MockRxBleAdapterStateObservable adapterStateObservable = Spy MockRxBleAdapterStateObservable
    MockLocationServicesStatus locationServicesStatusMock = Spy MockLocationServicesStatus
    RxBleDeviceProvider mockDeviceProvider = Mock RxBleDeviceProvider
    Lazy<ClientStateObservable> mockLazyClientStateObservable = Mock Lazy
    ScanSetupBuilder mockScanSetupBuilder = Mock ScanSetupBuilder
    Operation mockOperationScan = Mock Operation
    Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> mockObservableTransformer =
            new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                @Override
                Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                    return observable
                }
            }
    ScanSetup mockScanSetup = new ScanSetup(mockOperationScan, mockObservableTransformer)
    InternalToExternalScanResultConverter mockMapper = Mock InternalToExternalScanResultConverter
    private static someUUID = UUID.randomUUID()
    private static otherUUID = UUID.randomUUID()

    def setup() {
        setupWithRadio(rxBleRadio)
    }

    private void setupWithRadio(RxBleRadio radio) {
        contextMock.getApplicationContext() >> contextMock
        mockDeviceProvider.getBleDevice(_ as String) >> { String macAddress ->
            def device = Mock(RxBleDevice)
            device.macAddress >> macAddress
            device
        }
        mockOperationScan.run(_) >> Observable.never()
        mockScanSetupBuilder.build(_, _) >> mockScanSetup
        objectUnderTest = new RxBleClientImpl(
                bleAdapterWrapperSpy,
                radio,
                adapterStateObservable.asObservable(),
                uuidParserSpy,
                locationServicesStatusMock,
                mockLazyClientStateObservable,
                mockDeviceProvider,
                mockScanSetupBuilder,
                mockMapper,
                Executors.newSingleThreadExecutor(),
                ImmediateScheduler.INSTANCE
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
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true
    }

    def "should check if all the conditions are met at the time of each subscription to scan (Legacy)"() {
        given:
        def firstSubscriber = new TestSubscriber<>()
        def secondSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> bluetoothAvailable >> true
        bleAdapterWrapperSpy.isBluetoothEnabled() >> bluetoothEnabled >> true
        locationServicesStatusMock.isLocationPermissionOk() >> locationPermissionsOk >> true
        locationServicesStatusMock.isLocationProviderOk() >> locationProviderOk >> true
        def scanObservable = objectUnderTest.scanBleDevices(null)

        when:
        scanObservable.subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == reason
        }

        when:
        scanObservable.subscribe(secondSubscriber)

        then:
        secondSubscriber.assertNoErrors()
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true

        where:
        bluetoothAvailable | bluetoothEnabled | locationPermissionsOk | locationProviderOk | reason
        false              | true             | true                  | true               | BLUETOOTH_NOT_AVAILABLE
        true               | false            | true                  | true               | BLUETOOTH_DISABLED
        true               | true             | false                 | true               | LOCATION_PERMISSION_MISSING
        true               | true             | true                  | false              | LOCATION_SERVICES_DISABLED
    }


    def "should not start if all the conditions are not met at the time of subscription to scan (Legacy)"() {
        given:
        def firstSubscriber = new TestSubscriber<>()
        def secondSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> true >> bluetoothAvailable
        bleAdapterWrapperSpy.isBluetoothEnabled() >> true >> bluetoothEnabled
        locationServicesStatusMock.isLocationPermissionOk() >> true >> locationPermissionsOk
        locationServicesStatusMock.isLocationProviderOk() >> true >> locationProviderOk
        def scanObservable = objectUnderTest.scanBleDevices(null)

        when:
        scanObservable.subscribe(firstSubscriber)

        then:
        firstSubscriber.assertNoErrors()
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true

        when:
        scanObservable.subscribe(secondSubscriber)

        then:
        secondSubscriber.assertError {
            BleScanException exception -> exception.reason == reason
        }

        where:
        bluetoothAvailable | bluetoothEnabled | locationPermissionsOk | locationProviderOk | reason
        false              | true             | true                  | true               | BLUETOOTH_NOT_AVAILABLE
        true               | false            | true                  | true               | BLUETOOTH_DISABLED
        true               | true             | false                 | true               | LOCATION_PERMISSION_MISSING
        true               | true             | true                  | false              | LOCATION_SERVICES_DISABLED
    }

    def "should queue scan operation on subscribe (New API)"() {
        given:
        def radio = Mock(RxBleRadio)
        setupWithRadio(radio)
        def testSubscriber = new TestSubscriber<>()
        def scanObservable = objectUnderTest.scanBleDevices(Mock(ScanSettings))

        when:
        scanObservable.subscribe(testSubscriber)

        then:
        1 * radio.queue(mockOperationScan) >> Observable.empty()
    }

    def "should check if all the conditions are met at the time of each subscription to scan (New API)"() {
        given:
        def firstSubscriber = new TestSubscriber<>()
        def secondSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> bluetoothAvailable >> true
        bleAdapterWrapperSpy.isBluetoothEnabled() >> bluetoothEnabled >> true
        locationServicesStatusMock.isLocationPermissionOk() >> locationPermissionsOk >> true
        locationServicesStatusMock.isLocationProviderOk() >> locationProviderOk >> true
        def scanObservable = objectUnderTest.scanBleDevices(Mock(ScanSettings))

        when:
        scanObservable.subscribe(firstSubscriber)

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == reason
        }

        when:
        scanObservable.subscribe(secondSubscriber)

        then:
        secondSubscriber.assertNoErrors()

        where:
        bluetoothAvailable | bluetoothEnabled | locationPermissionsOk | locationProviderOk | reason
        false              | true             | true                  | true               | BLUETOOTH_NOT_AVAILABLE
        true               | false            | true                  | true               | BLUETOOTH_DISABLED
        true               | true             | false                 | true               | LOCATION_PERMISSION_MISSING
        true               | true             | true                  | false              | LOCATION_SERVICES_DISABLED
    }


    def "should not start if all the conditions are not met at the time of subscription to scan (New API)"() {
        given:
        def firstSubscriber = new TestSubscriber<>()
        def secondSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> true >> bluetoothAvailable
        bleAdapterWrapperSpy.isBluetoothEnabled() >> true >> bluetoothEnabled
        locationServicesStatusMock.isLocationPermissionOk() >> true >> locationPermissionsOk
        locationServicesStatusMock.isLocationProviderOk() >> true >> locationProviderOk
        def scanObservable = objectUnderTest.scanBleDevices(Mock(ScanSettings))

        when:
        scanObservable.subscribe(firstSubscriber)

        then:
        firstSubscriber.assertNoErrors()

        when:
        scanObservable.subscribe(secondSubscriber)

        then:
        secondSubscriber.assertError {
            BleScanException exception -> exception.reason == reason
        }

        where:
        bluetoothAvailable | bluetoothEnabled | locationPermissionsOk | locationProviderOk | reason
        false              | true             | true                  | true               | BLUETOOTH_NOT_AVAILABLE
        true               | false            | true                  | true               | BLUETOOTH_DISABLED
        true               | true             | false                 | true               | LOCATION_PERMISSION_MISSING
        true               | true             | true                  | false              | LOCATION_SERVICES_DISABLED
    }

    def "should not start scan until observable is subscribed"() {
        when:
        objectUnderTest.scanBleDevices(null)

        then:
        0 * bleAdapterWrapperSpy.startLegacyLeScan(_)
    }

    def "should stop scan after subscriber is unsubscribed from scan observable"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> true

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)
        scanSubscription.unsubscribe()

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
    }

    def "should stop and unsubscribe in case of scan throws exception"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> { throw new NullPointerException() }

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)

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
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
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
        0 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
    }

    def "should start scan only once even if observable has more subscribers"() {
        when:
        def scanObservable = objectUnderTest.scanBleDevices(null)
        scanObservable.subscribe()
        scanObservable.subscribe()

        then:
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true
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
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> false

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

    def "should emit BleScanException if BluetoothAdapter will be turned off during a scan"() {

        given:
        TestSubscriber testSubscriber = new TestSubscriber()
        mockMapper.call(_) >> {
            RxBleInternalScanResult _ ->
                System.out.println("XXX")
                return null
        } // does not matter as it will never be called
        objectUnderTest.scanBleDevices(Mock(ScanSettings)).subscribe(testSubscriber)

        when:
        adapterStateObservable.disableBluetooth()

        then:
        testSubscriber.assertError {
            BleScanException scanException -> scanException.reason == BLUETOOTH_DISABLED
        }
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

    def "should emit result with all parameters"() {
        given:
        TestSubscriber subscriber = new TestSubscriber<>()
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 10, scanRecord: [1, 2, 3] as byte[]

        when:
        objectUnderTest.scanBleDevices().subscribe(subscriber)

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

    // [DS 23.05.2017] TODO: cannot make it to work again although it should not be a problem because of using Emitter.setCancellation
//    /**
//     * This test reproduces issue: https://github.com/Polidea/RxAndroidBle/issues/17
//     * It first calls startLegacyLeScan method which takes 100ms to finish
//     * then it calls stopLegacyLeScan after 50ms but before startLegacyLeScan returns
//     */
//    def "should call stopLeScan only after startLeScan finishes and returns true"() {
//        given:
//        TestSubscriber testSubscriber = new TestSubscriber<>()
//        bleAdapterWrapperSpy.startLeScan(_) >> true
//        RxBleRadioOperationScan scanOperation = new RxBleRadioOperationScan(null, bleAdapterWrapperSpy, null) {
//
//            @Override
//            synchronized void protectedRun(Emitter<RxBleInternalScanResult> emitter, RadioReleaseInterface radioReleaseInterface) {
//                // simulate delay when starting scan
//                Thread.sleep(100)
//                super.protectedRun(emitter, radioReleaseInterface)
//            }
//        }
//        Thread stopScanThread = new Thread() {
//
//            @Override
//            void run() {
//                //unsubscribe before scan starts
//                Thread.sleep(50)
//                scanOperation.stop();
//            }
//        }
//        def scanTestRadio = new RxBleRadio() {
//
//            @Override
//            def <T> Observable<T> queue(Operation<T> operation) {
//                return operation
//                        .asObservable()
//                        .doOnSubscribe({
//                    stopScanThread.start()
//                    Thread runOperationThread = new Thread() {
//
//                        @Override
//                        void run() {
//                            def semaphore = new MockSemaphore()
//                            semaphore.awaitRelease()
//                            operation.run(semaphore)
//                        }
//                    }
//                    runOperationThread.start()
//                })
//            }
//        }
//
//        when:
//        scanTestRadio.queue(scanOperation).subscribe(testSubscriber)
//        waitForThreadsToCompleteWork()
//
//        then:
//        1 * bleAdapterWrapperSpy.startLegacyLeScan(_)
//
//        then:
//        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
//    }

    def "should throw UnsupportedOperationException if .getBleDevice() is called on system that has no Bluetooth capabilities"() {

        given:
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> false

        when:
        objectUnderTest.getBleDevice("AA:BB:CC:DD:EE:FF")

        then:
        thrown UnsupportedOperationException
    }

    def "should throw UnsupportedOperationException if .getBondedDevices() is called on system that has no Bluetooth capabilities"() {

        given:
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> false

        when:
        objectUnderTest.getBondedDevices()

        then:
        thrown UnsupportedOperationException
    }

    def "should get ClientStateObservable from Lazy when called .observeStateChanges()"() {

        when:
        objectUnderTest.observeStateChanges()

        then:
        1 * mockLazyClientStateObservable.get() >> Mock(ClientStateObservable)
    }

    public waitForThreadsToCompleteWork() {
        Thread.sleep(200) // Nasty :<
        true
    }
}
