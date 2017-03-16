package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import rx.observers.TestSubscriber
import spock.lang.Specification

class BluetoothGattProviderTest extends Specification {
    BluetoothGattProvider objectUnderTest
    TestSubscriber testSubscriber

    def setup() {
        objectUnderTest = new BluetoothGattProvider()
        testSubscriber = new TestSubscriber<>()
    }

    def "should return null after GATT storage was invalidated"() {
        given:
        objectUnderTest.updateBluetoothGatt(Mock(BluetoothGatt))
        objectUnderTest.invalidate()

        when:
        def capturedGatt = objectUnderTest.getBluetoothGatt()

        then:
        capturedGatt == null
    }

    def "should return null when asked for GATT initially"() {
        when:
        def capturedGatt = objectUnderTest.getBluetoothGatt()

        then:
        capturedGatt == null
    }

    def "should provide most first captured GATT instance"() {
        given:
        def firstGatt = Mock(BluetoothGatt)
        def secondGatt = Mock(BluetoothGatt)
        objectUnderTest.updateBluetoothGatt(firstGatt)
        objectUnderTest.updateBluetoothGatt(secondGatt)

        when:
        def capturedGatt = objectUnderTest.getBluetoothGatt()

        then:
        capturedGatt == firstGatt
    }
}
