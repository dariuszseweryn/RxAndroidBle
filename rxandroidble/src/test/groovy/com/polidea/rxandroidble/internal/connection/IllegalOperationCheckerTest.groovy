package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.internal.BleIllegalOperationException
import rx.observers.TestSubscriber
import spock.lang.Specification

class IllegalOperationCheckerTest extends Specification {

    IllegalOperationChecker objectUnderTest
    BluetoothGattCharacteristic bluetoothGattCharacteristic
    IllegalOperationHandler mockHandler
    BleIllegalOperationException mockException
    TestSubscriber testSubscriber
    final int NO_PROPERTIES = 0

    void setup() {
        mockHandler = Mock IllegalOperationHandler
        objectUnderTest = new IllegalOperationChecker(mockHandler)
        mockException = Mock BleIllegalOperationException
        bluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
        testSubscriber = new TestSubscriber()
    }

    def "should handle message if no property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> NO_PROPERTIES
        mockHandler.handleMismatchData(_, _) >> mockException

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertError(mockException)
    }

    def "should not handle message if the only needed property matches"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_READ

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic, BluetoothGattCharacteristic.PROPERTY_READ)
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertCompleted()
    }

    def "should not handle message if at least one of the needed properties match"() {
        given:
        bluetoothGattCharacteristic.getProperties() >> BluetoothGattCharacteristic.PROPERTY_WRITE

        when:
        objectUnderTest.checkAnyPropertyMatches(bluetoothGattCharacteristic,
                BluetoothGattCharacteristic.PROPERTY_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                        | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertCompleted()
    }
}