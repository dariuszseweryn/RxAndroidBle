package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.operations.Operation
import com.polidea.rxandroidble.internal.scan.ScanPreconditionsVerifier
import com.polidea.rxandroidble.internal.util.ClientStateObservable
import dagger.Lazy
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult
import com.polidea.rxandroidble.internal.scan.InternalToExternalScanResultConverter
import com.polidea.rxandroidble.internal.scan.ScanSetup
import com.polidea.rxandroidble.internal.scan.ScanSetupBuilder
import com.polidea.rxandroidble.scan.ScanSettings

import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_CANNOT_START
import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_DISABLED
import static com.polidea.rxandroidble.exceptions.BleScanException.BLUETOOTH_NOT_AVAILABLE
import static com.polidea.rxandroidble.exceptions.BleScanException.LOCATION_PERMISSION_MISSING
import static com.polidea.rxandroidble.exceptions.BleScanException.LOCATION_SERVICES_DISABLED
import static com.polidea.rxandroidble.exceptions.BleScanException.UNDOCUMENTED_SCAN_THROTTLE

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.RxBleDeviceProvider
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue
import com.polidea.rxandroidble.internal.util.UUIDUtil
import rx.Observable
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

class RxBleClientTest extends Specification {

    TestSubscriber testSubscriber = new TestSubscriber<>()

    DummyOperationQueue dummyQueue = new DummyOperationQueue()
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
    ScanPreconditionsVerifier mockScanPreconditionVerifier = Mock ScanPreconditionsVerifier
    InternalToExternalScanResultConverter mockMapper = Mock InternalToExternalScanResultConverter
    private static someUUID = UUID.randomUUID()
    private static otherUUID = UUID.randomUUID()

    private static Date suggestedDateToRetry = new Date()

    private static scanStarters = [
            { RxBleClient client -> client.scanBleDevices() },
            { RxBleClient client -> client.scanBleDevices(new ScanSettings.Builder().build()) },
    ]

    def setup() {
        setupWithQueue(dummyQueue)
    }

    private void setupWithQueue(ClientOperationQueue queue) {
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
                queue,
                adapterStateObservable.asObservable(),
                uuidParserSpy,
                locationServicesStatusMock,
                mockLazyClientStateObservable,
                mockDeviceProvider,
                mockScanSetupBuilder,
                mockScanPreconditionVerifier,
                mockMapper,
                ImmediateScheduler.INSTANCE,
                Mock(ClientComponent.ClientComponentFinalizer)
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

    @Unroll
    def "should call ScanPreconditionVerifier.verify() prior to queueing scan operation"() {
        given:
        def scanObservable = scanStarter.call(objectUnderTest)

        when:
        scanObservable.subscribe(testSubscriber)

        then:
        1 * mockScanPreconditionVerifier.verify()

        where:
        scanStarter << scanStarters
    }

    @Unroll
    def "should proxy an error from ScanPreconditionVerifier.verify() when starting a scan"() {
        given:
        ClientOperationQueue mockQueue = Mock ClientOperationQueue
        Throwable testThrowable = new BleScanException(BleScanException.UNKNOWN_ERROR_CODE, new Date())
        mockScanPreconditionVerifier.verify() >> { throw testThrowable }
        def scanObservable = scanStarter.call(objectUnderTest)

        when:
        scanObservable.subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testThrowable)

        and:
        0 * mockQueue.queue(_) >> Observable.empty()

        where:
        scanStarter << scanStarters
    }

    def "should start BLE scan if subscriber subscribes to the scan observable"() {
        when:
        objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)

        then:
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true
    }

    def "should queue scan operation on subscribe (New API)"() {
        given:
        def queue = Mock(ClientOperationQueue)
        setupWithQueue(queue)
        def testSubscriber = new TestSubscriber<>()
        def scanObservable = objectUnderTest.scanBleDevices(Mock(ScanSettings))

        when:
        scanObservable.subscribe(testSubscriber)

        then:
        1 * queue.queue(mockOperationScan) >> Observable.empty()
    }

    def "should not start scan until observable is subscribed"() {
        when:
        objectUnderTest.scanBleDevices(null)

        then:
        0 * bleAdapterWrapperSpy.startLegacyLeScan(_)
    }

    def "should stop scan after subscriber is unsubscribed from scan observable"() {
        given:
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> true

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).subscribe(testSubscriber)
        scanSubscription.unsubscribe()

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
    }

    def "should stop and unsubscribe in case of scan throws exception"() {
        given:
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

    @Unroll
    def "should emit BleScanException if bluetooth was disabled during scan"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()

        when:
        scanStarter.call(objectUnderTest).subscribe(firstSubscriber)
        adapterStateObservable.disableBluetooth()

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_DISABLED
        }

        where:
        scanStarter << scanStarters
    }

    @Unroll
    def "should emit BleScanException if bluetooth has been disabled scan"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> true
        bleAdapterWrapperSpy.isBluetoothEnabled() >> isBluetoothEnabled

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        if (isBluetoothEnabled) {
            firstSubscriber.assertNoErrors()
        } else {
            firstSubscriber.assertError { BleScanException exception -> exception.reason == BLUETOOTH_DISABLED }
        }

        where:
        [scanStarter, isBluetoothEnabled] << [scanStarters, [true, false]].combinations()
    }

    def "should emit error if bluetooth is not available"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.hasBluetoothAdapter() >> hasBt

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        if (!hasBt) {
            firstSubscriber.assertError { BleScanException exception -> exception.reason == BLUETOOTH_NOT_AVAILABLE }
        } else {
            firstSubscriber.assertNoErrors()
        }

        where:
        [scanStarter, hasBt] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if location permission was not granted"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        locationServicesStatusMock.isLocationPermissionOk = permissionOk

        when:
        scanStarter.call(objectUnderTest).subscribe(firstSubscriber)

        then:
        if (permissionOk) {
            firstSubscriber.assertNoErrors()
        } else {
            firstSubscriber.assertError { BleScanException exception -> exception.reason == LOCATION_PERMISSION_MISSING }
        }

        where:
        [scanStarter, permissionOk] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if location services are not ok (LocationProviderOk:#providerOk)"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        locationServicesStatusMock.isLocationProviderOk = providerOk

        when:
        scanStarter.call(objectUnderTest).subscribe(firstSubscriber)

        then:

        if (!providerOk)
            firstSubscriber.assertError { BleScanException exception -> exception.reason == LOCATION_SERVICES_DISABLED }
        else {
            firstSubscriber.assertNoErrors()
        }

        where:
        [scanStarter, providerOk] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if ScanPreconditionVerifier will suggest a date to start a scan"() {
        given:
        TestSubscriber testSubscriber = new TestSubscriber()
        mockScanPreconditionVerifier.suggestDateToRetry() >> dateToRetry

        when:
        scanStarter.call(objectUnderTest).subscribe(testSubscriber)

        then:
        if (dateToRetry != null) {
            testSubscriber.assertError { BleScanException e -> e.reason == UNDOCUMENTED_SCAN_THROTTLE && e.retryDateSuggestion == dateToRetry }
        } else {
            testSubscriber.assertNoErrors()
        }

        where:
        [scanStarter, dateToRetry] << [scanStarters, [suggestedDateToRetry, null]].combinations()
    }

    def "should emit BleScanException if BluetoothAdapter will be turned off during a scan"() {

        given:
        TestSubscriber testSubscriber = new TestSubscriber()
        mockMapper.call(_) >> {
            RxBleInternalScanResult _ -> return null
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
