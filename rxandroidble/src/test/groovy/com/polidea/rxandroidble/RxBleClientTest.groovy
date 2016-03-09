package com.polidea.rxandroidble

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.exceptions.BleScanException
import com.polidea.rxandroidble.internal.util.UUIDUtil
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

import static com.polidea.rxandroidble.exceptions.BleScanException.*

class RxBleClientTest extends Specification {

    FlatRxBleRadio rxBleRadio
    RxBleClient objectUnderTest
    Context contextMock = Mock Context
    UUIDUtil uuidParserSpy = Spy UUIDUtil
    MockRxBleAdapterWrapper bleAdapterWrapperSpy = Spy MockRxBleAdapterWrapper
    MockRxBleAdapterStateObservable adapterStateObservable = Spy MockRxBleAdapterStateObservable
    private static someUUID = UUID.randomUUID()
    private static otherUUID = UUID.randomUUID()

    def setup() {
        contextMock.getApplicationContext() >> contextMock
        rxBleRadio = new FlatRxBleRadio()
        objectUnderTest = new RxBleClientImpl(bleAdapterWrapperSpy, rxBleRadio, adapterStateObservable.asObservable(), uuidParserSpy)
    }

    def "should return same instance of client"() {
        given:
        def firstClient = RxBleClient.getInstance(contextMock)
        def secondClient = RxBleClient.getInstance(contextMock);

        expect:
        firstClient == secondClient
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

    def "should emit error if bluetooth scan failed to start"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()
        bleAdapterWrapperSpy.startLeScan(_) >> false

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)

        then:
        firstSubscriber.assertErrorClosure {
            BleScanException exception -> exception.reason == BLE_CANNOT_START
        }
    }

    def "should emit error if bluetooth was disabled during scan"() {
        given:
        TestSubscriber firstSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.scanBleDevices(null).subscribe(firstSubscriber)
        adapterStateObservable.disableBluetooth()

        then:
        firstSubscriber.assertErrorClosure {
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
        firstSubscriber.assertErrorClosure {
            BleScanException exception -> exception.reason == BLUETOOTH_NOT_AVAILABLE
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
}
