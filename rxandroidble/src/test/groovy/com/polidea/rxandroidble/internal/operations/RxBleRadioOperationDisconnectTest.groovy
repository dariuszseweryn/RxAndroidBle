package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import rx.Scheduler
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.android.schedulers.AndroidSchedulers
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Semaphore

import static android.bluetooth.BluetoothProfile.*
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.*

public class RxBleRadioOperationDisconnectTest extends Specification {

    BluetoothDevice mockDevice = Mock BluetoothDevice
    String mockMacAddress = "mockMackAddress"
    Semaphore mockSemaphore = Mock Semaphore
    BluetoothManager mockBluetoothManager = Mock BluetoothManager
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create()
    TestSubscriber<Void> testSubscriber = new TestSubscriber()
    BluetoothGattProvider mockBluetoothGattProvider
    RxBleRadioOperationDisconnect objectUnderTest

    def setupSpec() {
        AndroidSchedulers.reset()
        RxAndroidPlugins.getInstance().reset()
        RxAndroidPlugins.getInstance().registerSchedulersHook(
                new RxAndroidSchedulersHook() {

                    @Override
                    Scheduler getMainThreadScheduler() {
                        return Schedulers.immediate()
                    }
                }
        )
    }

    def teardownSpec() {
        AndroidSchedulers.reset()
        RxAndroidPlugins.getInstance().reset()
    }

    def setup() {
        mockBluetoothGattProvider = Mock(BluetoothGattProvider)
        mockBluetoothGattProvider.getBluetoothGatt() >>mockBluetoothGatt
        mockGattCallback.getOnConnectionStateChange() >> connectionStatePublishSubject
        mockBluetoothGatt.getDevice() >> mockDevice
        prepareObjectUnderTest()
    }

    def "should complete if AtomicReference<BluetoothGatt> contains null and then release the radio"() {
        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertCompleted()

        then:
        mockBluetoothGattProvider.getBluetoothGatt() >> null
        1 * mockSemaphore.release()
    }

    def "should call BluetoothGatt.close() if BluetoothGatt is disconnected at the time of running and then release the radio"() {

        given:
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_DISCONNECTED

        when:
        objectUnderTest.run()

        then:
        1 * mockBluetoothGatt.close()

        then:
        1 * mockSemaphore.release()
    }

    @Unroll
    def "should call BluetoothGatt.disconnect() if BluetoothGatt is not disconnected at the time of running and then BluetoothGatt.close() when RxBleGattCallback.getOnConnectionStateChange() will emit RxBleConnection.RxBleConnectionState.DISCONNECTED and then release the radio"() {

        given:
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> initialState

        when:
        objectUnderTest.run()

        then:
        1 * mockBluetoothGatt.disconnect()

        when:
        connectionStatePublishSubject.onNext(nextState)

        then:
        closeCalled * mockBluetoothGatt.close()

        then:
        closeCalled * mockSemaphore.release()

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

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationDisconnect(mockGattCallback, mockBluetoothGattProvider, mockMacAddress,
                mockBluetoothManager, ImmediateScheduler.INSTANCE, new MockOperationTimeoutConfiguration(Schedulers.computation()))
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}