package com.polidea.rxandroidble3

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import com.polidea.rxandroidble3.exceptions.BleScanException

import com.polidea.rxandroidble3.internal.RxBleDeviceProvider
import com.polidea.rxandroidble3.internal.operations.Operation
import com.polidea.rxandroidble3.internal.scan.*
import com.polidea.rxandroidble3.internal.serialization.ClientOperationQueue
import com.polidea.rxandroidble3.internal.util.CheckerLocationPermission
import com.polidea.rxandroidble3.internal.util.ClientStateObservable
import com.polidea.rxandroidble3.internal.util.ScanRecordParser
import com.polidea.rxandroidble3.scan.BackgroundScanner
import com.polidea.rxandroidble3.scan.ScanSettings
import hkhc.electricspock.ElectricSpecification
import org.robolectric.annotation.Config
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.TestScheduler
import spock.lang.Unroll

import static com.polidea.rxandroidble3.exceptions.BleScanException.*

@SuppressWarnings("GrDeprecatedAPIUsage")
@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
class RxBleClientTest extends ElectricSpecification {

    BackgroundScanner backgroundScanner = Mock(BackgroundScanner)
    DummyOperationQueue dummyQueue = new DummyOperationQueue()
    RxBleClient objectUnderTest
    Context contextMock = Mock Context
    ScanRecordParser scanRecordParserSpy = Spy ScanRecordParser
    MockRxBleAdapterWrapper bleAdapterWrapperSpy = Spy MockRxBleAdapterWrapper
    MockRxBleAdapterStateObservable adapterStateObservable = Spy MockRxBleAdapterStateObservable
    MockLocationServicesStatus locationServicesStatusMock = Spy MockLocationServicesStatus
    RxBleDeviceProvider mockDeviceProvider = Mock RxBleDeviceProvider
    bleshadow.dagger.Lazy<ClientStateObservable> mockLazyClientStateObservable = Mock bleshadow.dagger.Lazy
    ScanSetupBuilder mockScanSetupBuilder = Mock ScanSetupBuilder
    Operation mockOperationScan = Mock Operation
    ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> mockObservableTransformer =
            new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {

                @Override
                ObservableSource<RxBleInternalScanResult> apply(@NonNull Observable<RxBleInternalScanResult> upstream) {
                    return upstream
                }
            }
    ScanSetup mockScanSetup = new ScanSetup(mockOperationScan, mockObservableTransformer)
    ScanPreconditionsVerifier mockScanPreconditionVerifier = Mock ScanPreconditionsVerifier
    InternalToExternalScanResultConverter mockMapper = Mock InternalToExternalScanResultConverter
    CheckerLocationPermission mockCheckerLocationPermission = Mock CheckerLocationPermission
    private static someUUID = UUID.randomUUID()
    private static otherUUID = UUID.randomUUID()
    private static Date suggestedDateToRetry = new Date()

    private static List<Closure<Observable<RxBleScanResult>>> scanStarters = [
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
                scanRecordParserSpy,
                locationServicesStatusMock,
                mockLazyClientStateObservable,
                mockDeviceProvider,
                mockScanSetupBuilder,
                mockScanPreconditionVerifier,
                mockMapper,
                new TestScheduler(),
                Mock(ClientComponent.ClientComponentFinalizer),
                backgroundScanner,
                mockCheckerLocationPermission
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
        scanObservable.test()

        then:
        1 * mockScanPreconditionVerifier.verify(true)

        where:
        scanStarter << scanStarters
    }

    @Unroll
    def "should pass shouldCheckLocationServices value to ScanPreconditionVerifier.verify() accordingly when called with RxBleClient.scanBleDevices(ScanSettings, ScanFilters...)"() {

        given:
        ScanSettings scanSettings = new ScanSettings.Builder().setShouldCheckLocationServicesState(shouldCheck).build()
        def scanObservable = objectUnderTest.scanBleDevices(scanSettings)

        when:
        scanObservable.test()

        then:
        1 * mockScanPreconditionVerifier.verify(shouldCheck)

        where:
        shouldCheck << [true, false]
    }

    def "should call ScanPreconditionVerifier.verify(true) when called with RxBleClient.scanBleDevices(UUID...)"() {

        given:
        def scanObservable = objectUnderTest.scanBleDevices()

        when:
        scanObservable.test()

        then:
        1 * mockScanPreconditionVerifier.verify(true)
    }

    @Unroll
    def "should proxy an error from ScanPreconditionVerifier.verify() when starting a scan"() {
        given:
        ClientOperationQueue mockQueue = Mock ClientOperationQueue
        Throwable testThrowable = new BleScanException(UNKNOWN_ERROR_CODE, new Date())
        mockScanPreconditionVerifier.verify(_) >> { throw testThrowable }
        def scanObservable = scanStarter.call(objectUnderTest)

        when:
        def testSubscriber = scanObservable.test()

        then:
        testSubscriber.assertError(testThrowable)

        and:
        0 * mockQueue.queue(_) >> Observable.empty()

        where:
        scanStarter << scanStarters
    }

    def "should start BLE scan if subscriber subscribes to the scan observable"() {
        when:
        objectUnderTest.scanBleDevices(null).test()

        then:
        1 * bleAdapterWrapperSpy.startLegacyLeScan(_) >> true
    }

    def "should queue scan operation on subscribe (New API)"() {
        given:
        def queue = Mock(ClientOperationQueue)
        setupWithQueue(queue)
        def scanObservable = objectUnderTest.scanBleDevices(Mock(ScanSettings))

        when:
        scanObservable.test()

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
        objectUnderTest.scanBleDevices(null).test().dispose()

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
    }

    def "should stop and unsubscribe in case of scan throws exception"() {
        given:
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> { throw new NullPointerException() }

        when:
        def scanSubscription = objectUnderTest.scanBleDevices(null).test()

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)

        and:
        scanSubscription.assertTerminated()
    }

    def "should stop scan after all subscribers are unsubscribed"() {
        when:
        def disposable = objectUnderTest.scanBleDevices(null).test()
        def secondDisposable = objectUnderTest.scanBleDevices(null).test()
        disposable.dispose()
        secondDisposable.dispose()

        then:
        1 * bleAdapterWrapperSpy.stopLegacyLeScan(_)
    }

    def "should not stop scan if not all subscribers are unsubscribed"() {
        when:
        def firstSubscription = objectUnderTest.scanBleDevices(null).test()
        objectUnderTest.scanBleDevices(null).test()
        firstSubscription.dispose()
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
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 0, scanRecord: [] as byte[]
        bluetoothDeviceDiscovered deviceMac: "BB:BB:BB:BB:BB:BB", rssi: 50, scanRecord: [] as byte[]

        when:
        def firstSubscriber = objectUnderTest.scanBleDevices(null).test()
        def secondSubscriber = objectUnderTest.scanBleDevices(null).test()

        then:
        firstSubscriber.assertValueCount 2

        and:
        secondSubscriber.assertValueCount 0
    }

    def "should emit BleScanException if bluetooth scan failed to start"() {
        given:
        bleAdapterWrapperSpy.startLegacyLeScan(_) >> false

        when:
        def firstSubscriber = objectUnderTest.scanBleDevices(null).test()

        then:
        firstSubscriber.assertError {
            BleScanException exception -> exception.reason == BLUETOOTH_CANNOT_START
        }
    }

    @Unroll
    def "should emit BleScanException if bluetooth was disabled during scan"() {
        when:
        def firstSubscriber = scanStarter.call(objectUnderTest).test()
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
        if (!isBluetoothEnabled)
            mockScanPreconditionVerifier.verify(_) >> { throw new BleScanException(BLUETOOTH_DISABLED) }

        when:
        def firstSubscriber = objectUnderTest.scanBleDevices(null).test()

        then:
        isBluetoothEnabled ? firstSubscriber.assertNoErrors()
                : firstSubscriber.assertError { BleScanException exception -> exception.reason == BLUETOOTH_DISABLED }

        where:
        [scanStarter, isBluetoothEnabled] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit error if bluetooth is not available"() {
        given:
        if (!hasBt)
            mockScanPreconditionVerifier.verify(_) >> { throw new BleScanException(BLUETOOTH_NOT_AVAILABLE) }

        when:
        def firstSubscriber = objectUnderTest.scanBleDevices(null).test()

        then:
        !hasBt ?
                firstSubscriber.assertError { BleScanException exception -> exception.reason == BLUETOOTH_NOT_AVAILABLE }
                : firstSubscriber.assertNoErrors()

        where:
        [scanStarter, hasBt] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if location permission was not granted"() {
        given:
        if (!permissionOk)
            mockScanPreconditionVerifier.verify(_) >> { throw new BleScanException(LOCATION_PERMISSION_MISSING) }

        when:
        TestObserver<RxBleScanResult> firstSubscriber = scanStarter.call(objectUnderTest).test()

        then:
        permissionOk ?
                firstSubscriber.assertNoErrors()
                : firstSubscriber.assertError { BleScanException exception -> exception.reason == LOCATION_PERMISSION_MISSING }

        where:
        [scanStarter, permissionOk] << [scanStarters, [true, false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if location services are not ok (LocationProviderOk:#providerOk)"() {
        given:
        if (!providerOk)
            mockScanPreconditionVerifier.verify(_) >> { throw new BleScanException(LOCATION_SERVICES_DISABLED) }


        when:
        def firstSubscriber = scanStarter.call(objectUnderTest).test()

        then:
        !providerOk ?
                firstSubscriber.assertError { BleScanException exception -> exception.reason == LOCATION_SERVICES_DISABLED }
                : firstSubscriber.assertNoErrors()

        where:
        [scanStarter, providerOk] << [scanStarters, [false]].combinations()
    }

    @Unroll
    def "should emit BleScanException if ScanPreconditionVerifier will suggest a date to start a scan"() {
        given:
        if (dateToRetry != null)
            mockScanPreconditionVerifier.verify(_) >> {
                throw new BleScanException(UNDOCUMENTED_SCAN_THROTTLE, dateToRetry)
            }

        when:
        def testSubscriber = scanStarter.call(objectUnderTest).test()

        then:
        (dateToRetry != null) ?
                testSubscriber.assertError { BleScanException e -> e.reason == UNDOCUMENTED_SCAN_THROTTLE && e.retryDateSuggestion == dateToRetry }
                : testSubscriber.assertNoErrors()

        where:
        [scanStarter, dateToRetry] << [scanStarters, [suggestedDateToRetry, null]].combinations()
    }

    def "should emit BleScanException if BluetoothAdapter will be turned off during a scan"() {

        given:
        mockMapper.call(_) >> {
            RxBleInternalScanResult _ -> return null
        } // does not matter as it will never be called
        def testSubscriber = objectUnderTest.scanBleDevices(Mock(ScanSettings)).test()

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
        scanRecordParserSpy.extractUUIDs(_) >>> publicServices

        when:
        TestObserver<RxBleScanResult> testSubscriber = objectUnderTest.scanBleDevices(filter as UUID[]).test()

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
        def filter = UUID.randomUUID()
        def secondUUID = UUID.randomUUID()
        def thirdUUID = UUID.randomUUID()
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 0, scanRecord: [] as byte[]
        scanRecordParserSpy.extractUUIDs(_) >> [filter, secondUUID, thirdUUID]

        when:
        def testSubscriber = objectUnderTest.scanBleDevices([filter] as UUID[]).test()

        then:
        testSubscriber.assertValueCount 1
    }

    def "should emit result with all parameters"() {
        given:
        bluetoothDeviceDiscovered deviceMac: "AA:AA:AA:AA:AA:AA", rssi: 10, scanRecord: [1, 2, 3] as byte[]

        when:
        def testObserver = objectUnderTest.scanBleDevices().test()

        then:
        testObserver.assertScanRecord(10, "AA:AA:AA:AA:AA:AA", [1, 2, 3] as byte[])
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
        bleAdapterWrapperSpy.addBondedDevice(mock)
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

    def "should provide injected background scanner"() {
        expect:
        backgroundScanner == objectUnderTest.getBackgroundScanner()
    }

    @Unroll
    def "should pass call to CheckerLocationPermission when called .isScanRuntimePermissionGranted() and proxy back the result"() {

        when:
        def result = objectUnderTest.isScanRuntimePermissionGranted()

        then:
        1 * mockCheckerLocationPermission.isScanRuntimePermissionGranted() >> expectedResult

        and:
        result == expectedResult

        where:
        expectedResult << [true, false]
    }

    def "should pass call to CheckerLocationPermission when called .getRecommendedScanRuntimePermissions() and proxy back the result"() {

        given:
        String[] resultRef = new String[0]

        when:
        def result = objectUnderTest.getRecommendedScanRuntimePermissions()

        then:
        1 * mockCheckerLocationPermission.getRecommendedScanRuntimePermissions() >> resultRef

        and:
        result == resultRef
    }

    def waitForThreadsToCompleteWork() {
        Thread.sleep(200) // Nasty :<
        true
    }
}
