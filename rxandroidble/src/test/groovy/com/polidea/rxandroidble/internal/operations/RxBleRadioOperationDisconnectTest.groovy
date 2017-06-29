package com.polidea.rxandroidble.internal.operations

import static android.bluetooth.BluetoothProfile.GATT
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTING

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import com.polidea.rxandroidble.internal.RadioReleaseInterface
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import rx.Observable
import rx.Scheduler
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll


public class RxBleRadioOperationDisconnectTest extends Specification {

    BluetoothDevice mockDevice = Mock BluetoothDevice
    String mockMacAddress = "mockMackAddress"
    RadioReleaseInterface mockRadioReleaseInterface = Mock RadioReleaseInterface
    BluetoothManager mockBluetoothManager = Mock BluetoothManager
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create()
    Action1<RxBleConnection.RxBleConnectionState> mockConnectionStateChangedAction = Mock Action1
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

    private def testWithGattProviderReturning(BluetoothGatt providedBluetoothGatt) {
        mockBluetoothGattProvider = Mock(BluetoothGattProvider)
        mockBluetoothGattProvider.getBluetoothGatt() >> providedBluetoothGatt
        mockGattCallback.getOnConnectionStateChange() >> connectionStatePublishSubject
        mockBluetoothGatt.getDevice() >> mockDevice
        prepareObjectUnderTest()
    }

    def "should complete if AtomicReference<BluetoothGatt> contains null and then release the radio"() {

        given:
        testWithGattProviderReturning(null)

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        testSubscriber.assertCompleted()

        then:
        mockBluetoothGattProvider.getBluetoothGatt() >> null
        1 * mockRadioReleaseInterface.release()
    }

    def "should call BluetoothGatt.close() if BluetoothGatt is disconnected at the time of running and then release the radio"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_DISCONNECTED

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockBluetoothGatt.close()

        then:
        1 * mockRadioReleaseInterface.release()
    }

    @Unroll
    def "should call BluetoothGatt.disconnect() if BluetoothGatt is not disconnected at the time of running and then BluetoothGatt.close() when RxBleGattCallback.getOnConnectionStateChange() will emit RxBleConnection.RxBleConnectionState.DISCONNECTED and then release the radio"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> initialState

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockBluetoothGatt.disconnect()

        when:
        connectionStatePublishSubject.onNext(nextState)

        then:
        closeCalled * mockBluetoothGatt.close()

        then:
        closeCalled * mockRadioReleaseInterface.release()

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
        def observable = objectUnderTest.run(mockRadioReleaseInterface)

        when:
        observable.subscribe()

        then:
        1 * mockConnectionStateChangedAction.call(DISCONNECTING)
    }

    def "should call connectionStateChangedAction with DISCONNECTED when completed"() {

        given:
        testWithGattProviderReturning(mockBluetoothGatt)
        mockBluetoothManager.getConnectionState(mockDevice, GATT) >> STATE_CONNECTED
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        when:
        connectionStatePublishSubject.onNext(DISCONNECTED)

        then:
        1 * mockConnectionStateChangedAction.call(DISCONNECTED)
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationDisconnect(mockGattCallback, mockBluetoothGattProvider, mockMacAddress,
                mockBluetoothManager, ImmediateScheduler.INSTANCE, new MockOperationTimeoutConfiguration(Schedulers.computation()),
                mockConnectionStateChangedAction)
    }
}