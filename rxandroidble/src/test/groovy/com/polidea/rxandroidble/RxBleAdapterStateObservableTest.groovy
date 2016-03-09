package com.polidea.rxandroidble

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification
import rx.Subscription
import rx.observers.TestSubscriber

import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.*

@Config(manifest = Config.NONE)
class RxBleAdapterStateObservableTest extends RoboSpecification {
    def contextMock = Mock Context
    def objectUnderTest = new RxBleAdapterStateObservable(contextMock)
    BroadcastReceiver registeredReceiver

    def setup() {
        contextMock.getApplicationContext() >> contextMock
    }

    def "should register to correct receiver on subscribe"() {

        when:
        objectUnderTest.subscribe()

        then:
        1 * contextMock.registerReceiver(!null, {
            it.hasAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        })
    }

    def "should unregister after observable was unsubscribed"() {
        given:
        shouldCaptureRegisteredReceiver()
        Subscription subscription = objectUnderTest.subscribe()

        when:
        subscription.unsubscribe()

        then:
        1 * contextMock.unregisterReceiver(registeredReceiver)
    }

    def "should map all bluetooth change states"() {
        given:
        shouldCaptureRegisteredReceiver()
        def testSubscriber = new TestSubscriber()
        objectUnderTest.subscribe(testSubscriber)

        when:
        postStateChangeBroadcast(bluetoothChange)

        then:
        testSubscriber.assertValue(expectedStatus)

        where:
        bluetoothChange                    | expectedStatus
        BluetoothAdapter.STATE_ON          | STATE_ON
        BluetoothAdapter.STATE_TURNING_ON  | STATE_TURNING_ON
        BluetoothAdapter.STATE_TURNING_OFF | STATE_TURNING_OFF
        BluetoothAdapter.STATE_OFF         | STATE_OFF
        -1                                 | STATE_OFF
    }

    def "should report if state is usable"() {
        when:
        def isUsableCheck = bluetootState.isUsable();

        then:
        isUsableCheck == isUsable

        where:
        bluetootState     | isUsable
        STATE_ON          | true
        STATE_OFF         | false
        STATE_TURNING_OFF | false
        STATE_TURNING_ON  | false
    }

    public postStateChangeBroadcast(int bluetoothChange) {
        def intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED)
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, bluetoothChange)
        registeredReceiver.onReceive(contextMock, intent)
    }

    public BroadcastReceiver shouldCaptureRegisteredReceiver() {
        _ * contextMock.registerReceiver({
            BroadcastReceiver receiver ->
                this.registeredReceiver = receiver
        }, _)
    }
}
