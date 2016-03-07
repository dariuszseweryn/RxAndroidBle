package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.support.v4.util.Pair
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect
import spock.lang.Specification
import spock.lang.Unroll

public class RxBleConnectionConnectorOperationsProviderTest extends Specification {

    RxBleConnectionConnectorOperationsProvider provider

    Context mockContext

    BluetoothDevice mockDevice

    RxBleGattCallback mockCallback

    def setup() {
        mockContext = Mock Context
        mockDevice = Mock BluetoothDevice
        mockCallback = Mock RxBleGattCallback

        provider = new RxBleConnectionConnectorOperationsProvider()
    }

    @Unroll
    def "provide() should return a android.support.v4.util.Pair with RxBleRadioOperationConnect and RxBleRadioOperationDisconnect #id"() {

        when:
        def pair = provider.provide(mockContext, mockDevice, autoConnectValue, mockCallback)

        then:
        assert pair instanceof Pair
        assert pair.first instanceof RxBleRadioOperationConnect
        assert pair.second instanceof RxBleRadioOperationDisconnect

        where:
        id | autoConnectValue
        0  | true
        1  | false
    }

    // TODO: would be great to test if the RxBleRadioOperationDisconnect will get BluetoothGatt through AtomicReference
}