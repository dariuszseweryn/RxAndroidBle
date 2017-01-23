package com.polidea.rxandroidble.internal.operations

import static android.bluetooth.BluetoothProfile.*
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.*

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference
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

public class RxBleRadioOperationDisconnectTest extends Specification {

    BluetoothDevice mockDevice = Mock BluetoothDevice

    Semaphore mockSemaphore = Mock Semaphore

    BluetoothManager mockBluetoothManager = Mock BluetoothManager

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    AtomicReference<BluetoothGatt> gattAtomicReference = new AtomicReference<BluetoothGatt>(mockBluetoothGatt)

    PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create()

    TestSubscriber<Void> testSubscriber = new TestSubscriber()

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
        mockGattCallback.getOnConnectionStateChange() >> connectionStatePublishSubject
        mockBluetoothGatt.getDevice() >> mockDevice
        prepareObjectUnderTest()
    }

    def "should complete if AtomicReference<BluetoothGatt> contains null and then release the radio"() {

        given:
        gattAtomicReference.set(null)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertCompleted()

        then:
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
        objectUnderTest = new RxBleRadioOperationDisconnect(mockGattCallback, gattAtomicReference, mockBluetoothManager, ImmediateScheduler.INSTANCE)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}