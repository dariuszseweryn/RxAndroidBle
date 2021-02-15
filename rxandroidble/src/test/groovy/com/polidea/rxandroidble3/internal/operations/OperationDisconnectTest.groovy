package com.polidea.rxandroidble3.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.internal.connection.BluetoothGattProvider
import com.polidea.rxandroidble3.internal.connection.ConnectionStateChangeListener
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble3.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

import static android.bluetooth.BluetoothProfile.*
import static com.polidea.rxandroidble3.RxBleConnection.RxBleConnectionState.*

public class OperationDisconnectTest extends Specification {

    BluetoothDevice mockDevice = Mock BluetoothDevice
    String mockMacAddress = "mockMackAddress"
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothManager mockBluetoothManager = Mock BluetoothManager
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create()
    ConnectionStateChangeListener mockConnectionStateChangeListener = Mock ConnectionStateChangeListener
    BluetoothGattProvider mockBluetoothGattProvider
    DisconnectOperation objectUnderTest

    private def testWithGattProviderReturning(BluetoothGatt providedBluetoothGatt) {
        mockBluetoothGattProvider = Mock(BluetoothGattProvider)
        mockBluetoothGattProvider.getBluetoothGatt() >> providedBluetoothGatt
        mockGattCallback.getOnConnectionStateChange() >> connectionStatePublishSubject
        mockBluetoothGatt.getDevice() >> mockDevice
        prepareObjectUnderTest()
    }

    def "should complete if AtomicReference<BluetoothGatt> contains null and then release the queue"() {

        given:
        testWithGattProviderReturning(null)

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertComplete()

        then:
        mockBluetoothGattProvider.getBluetoothGatt() >> null
        1 * mockQueueReleaseInterface.release()
    }

    def "should call BluetoothGatt.close() if BluetoothGatt is disconnected at the time of running and then release the queue"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_DISCONNECTED

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.close()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    @Unroll
    def "should call BluetoothGatt.disconnect() if BluetoothGatt is not disconnected at the time of running and then BluetoothGatt.close() when RxBleGattCallback.getOnConnectionStateChange() will emit RxBleConnection.RxBleConnectionState.DISCONNECTED and then release the queue"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> initialState

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.disconnect()

        when:
        connectionStatePublishSubject.onNext(nextState)

        then:
        closeCalled * mockBluetoothGatt.close()

        then:
        closeCalled * mockQueueReleaseInterface.release()

        where:
        initialState        | nextState     | closeCalled
        STATE_CONNECTED     | CONNECTED     | 0
        STATE_CONNECTED     | CONNECTING    | 0
        STATE_CONNECTED     | DISCONNECTING | 0
        STATE_CONNECTED     | DISCONNECTED  | 1
        STATE_CONNECTING    | CONNECTED     | 0
        STATE_CONNECTING    | CONNECTING    | 0
        STATE_CONNECTING    | DISCONNECTING | 0
        STATE_CONNECTING    | DISCONNECTED  | 1
        STATE_DISCONNECTING | CONNECTED     | 0
        STATE_DISCONNECTING | CONNECTING    | 0
        STATE_DISCONNECTING | DISCONNECTING | 0
        STATE_DISCONNECTING | DISCONNECTED  | 1
    }

    def "should call connectionStateChangedAction with DISCONNECTING when run"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        def observable = objectUnderTest.run(mockQueueReleaseInterface)

        when:
        observable.subscribe()

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(DISCONNECTING)
    }

    def "should call connectionStateChangedAction with DISCONNECTED when completed"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_CONNECTED
        objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        connectionStatePublishSubject.onNext(DISCONNECTED)

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(DISCONNECTED)
    }

    def "should call connectionStateChangedAction with DISCONNECTED when error occurred"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_CONNECTED
        objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        connectionStatePublishSubject.onError(new Throwable("test"))

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(DISCONNECTED)
    }

    def "should call connectionStateChangedAction with DISCONNECTED when BluetoothGatt is null"() {

        given:
        testWithGattProviderReturning(null)

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(DISCONNECTED)
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new DisconnectOperation(mockGattCallback, mockBluetoothGattProvider, mockMacAddress,
                mockBluetoothManager, Schedulers.trampoline(), new MockOperationTimeoutConfiguration(Schedulers.computation()),
                mockConnectionStateChangeListener)
    }
}