package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.support.v4.util.Pair
import com.polidea.rxandroidble.FlatRxBleRadio
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.exceptions.BleGattException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import spock.lang.Specification

import static com.polidea.rxandroidble.exceptions.BleGattOperationType.CHARACTERISTIC_READ
import static com.polidea.rxandroidble.exceptions.BleGattOperationType.CHARACTERISTIC_WRITE
import static java.util.Collections.emptyList
import static rx.Observable.just

class RxBleConnectionTest extends Specification {
    def flatRadio = new FlatRxBleRadio()
    def callback = Mock RxBleGattCallback
    def bluetoothGattMock = Mock BluetoothGatt
    def objectUnderTest = new RxBleConnectionImpl(flatRadio, callback, bluetoothGattMock)
    def connectionStateChange = BehaviorSubject.create()
    def TestSubscriber testSubscriber

    def setup() {
        testSubscriber = new TestSubscriber()
        callback.getOnConnectionStateChange() >> connectionStateChange
    }

    def "should proxy connection state"() {
        given:
        connectionStateChange.onNext(pushedStatus)

        when:
        objectUnderTest.getConnectionState().subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(expectedState)

        where:
        pushedStatus                                       | expectedState
        RxBleConnection.RxBleConnectionState.CONNECTED     | RxBleConnection.RxBleConnectionState.CONNECTED
        RxBleConnection.RxBleConnectionState.DISCONNECTED  | RxBleConnection.RxBleConnectionState.DISCONNECTED
        RxBleConnection.RxBleConnectionState.CONNECTING    | RxBleConnection.RxBleConnectionState.CONNECTING
        RxBleConnection.RxBleConnectionState.DISCONNECTING | RxBleConnection.RxBleConnectionState.DISCONNECTING
    }

    def "should emit error if failed to start retrieving services"() {
        given:
        callback.getOnServicesDiscovered() >> PublishSubject.create()
        shouldGattContainNoServices()
        shouldFailStartingDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleGattException)
        testSubscriber.assertErrorClosure {
            it.bleGattOperationType == BleGattOperationType.SERVICE_DISCOVERY
        }
    }

    def "should return cached services"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe() // <-- it must be here hence mocks are not configured yet in given block.
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        (_..1) * bluetoothGattMock.getServices() >> emptyList()
        1 * callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }

    def "should return services instantly if they were already discovered"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        bluetoothGattMock.getServices() >> services

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        0 * bluetoothGattMock.discoverServices()
    }

    def "should try to discover services if cached services are empty"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()
        shouldGattContainNoServices()
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        1 * bluetoothGattMock.discoverServices() >> true
    }

    def "should emit empty there are no services during read operation"() {
        given:
        def uuid = UUID.randomUUID()
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.readCharacteristic(uuid).subscribe(testSubscriber)

        then:
        assertCharacteristicNotFoundDuringOperation(CHARACTERISTIC_READ)
    }

    def "should emit empty observable if characteristic was not found during read operation"() {
        given:
        def uuid = UUID.randomUUID()
        def service = Mock BluetoothGattService
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        service.getCharacteristic(_) >> null

        when:
        objectUnderTest.readCharacteristic(uuid).subscribe(testSubscriber)

        then:
        assertCharacteristicNotFoundDuringOperation(CHARACTERISTIC_READ)
    }

    def "should read first found characteristic with matching UUID"() {
        given:
        def firstCharacteristicUUID = UUID.randomUUID()
        def secondCharacteristicUUID = UUID.randomUUID()
        def firstCharacteristicValue = [1, 2, 3] as byte[]
        def secondCharacteristicValue = [3, 4, 5] as byte[]
        def service = Mock BluetoothGattService
        shouldServiceContainCharacteristic(service, firstCharacteristicUUID, firstCharacteristicValue)
        shouldServiceContainCharacteristic(service, secondCharacteristicUUID, secondCharacteristicValue)
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        shouldGattCallbackReturnDataOnRead(
                [uuid: secondCharacteristicUUID, value: secondCharacteristicValue],
                [uuid: firstCharacteristicUUID, value: firstCharacteristicValue])

        when:
        objectUnderTest.readCharacteristic(firstCharacteristicUUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(firstCharacteristicValue)
    }

    def "should emit empty there are no services during write operation"() {
        given:
        def uuid = UUID.randomUUID()
        def dataToWrite = [1, 2, 3] as byte[]
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.writeCharacteristic(uuid, dataToWrite).subscribe(testSubscriber)

        then:
        assertCharacteristicNotFoundDuringOperation(CHARACTERISTIC_WRITE)
    }

    def "should emit empty observable if characteristic was not found during write operation"() {
        given:
        def uuid = UUID.randomUUID()
        def dataToWrite = [1, 2, 3] as byte[]
        def service = Mock BluetoothGattService
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        service.getCharacteristic(_) >> null

        when:
        objectUnderTest.writeCharacteristic(uuid, dataToWrite).subscribe(testSubscriber)

        then:
        assertCharacteristicNotFoundDuringOperation(CHARACTERISTIC_WRITE)
    }

    public assertCharacteristicNotFoundDuringOperation(operationType) {
        testSubscriber.assertError(BleGattException)
        testSubscriber.assertErrorClosure { BleGattException exception ->
            exception.bleGattOperationType == operationType && exception.status == BleGattException.CHARACTERISTIC_NOT_FOUND
        }
    }

    public shouldServiceContainCharacteristic(BluetoothGattService service, UUID uuid, byte[] characteristicValue) {
        service.getCharacteristic(uuid) >> mockCharacteristicWithValue(uuid: uuid, value: characteristicValue)
    }

    public shouldGattCallbackReturnDataOnRead(Map... parameters) {
        callback.getOnCharacteristicRead() >> {
            Observable.from(parameters.collect { Pair.create it['uuid'], it['value'] })
        }
    }

    public mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic
    }

    public shouldGattContainServices(List list) {
        bluetoothGattMock.getServices() >> list
    }

    public shouldGattContainNoServices() {
        bluetoothGattMock.getServices() >> emptyList()
    }

    public shouldFailStartingDiscovery() {
        bluetoothGattMock.discoverServices() >> false
    }

    public shouldSuccessfullyStartDiscovery() {
        bluetoothGattMock.discoverServices() >> true
    }

    public shouldGattCallbackReturnServicesOnDiscovery(ArrayList<BluetoothGattService> services) {
        bluetoothGattMock.discoverServices() >> true
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }
}
