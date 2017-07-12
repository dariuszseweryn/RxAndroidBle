package com.polidea.rxandroidble.internal.util

import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.exceptions.BleIllegalOperationException
import com.polidea.rxandroidble.internal.connection.IllegalOperationMessageCreator
import com.polidea.rxandroidble.internal.connection.ThrowingIllegalOperationHandler
import spock.lang.Specification

class ThrowingIllegalOperationHandlerTest extends Specification {

    ThrowingIllegalOperationHandler objectUnderTest
    IllegalOperationMessageCreator messageCreator = Mock IllegalOperationMessageCreator
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    int neededProperties = 0


    void setup() {
        objectUnderTest = new ThrowingIllegalOperationHandler(messageCreator)
    }

    def "should throw BleIllegalOperationException if no property matches"() {
        when:
        objectUnderTest.handleMismatchData(mockCharacteristic, neededProperties)

        then:
        thrown BleIllegalOperationException
    }
}
