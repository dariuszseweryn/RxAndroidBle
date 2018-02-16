package com.polidea.rxandroidble2.internal.connection

import android.bluetooth.BluetoothGatt
import spock.lang.Specification

class BluetoothGattProviderTest extends Specification {
    BluetoothGattProvider objectUnderTest

    def setup() {
        objectUnderTest = new BluetoothGattProvider()
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
