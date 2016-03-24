package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect
import com.polidea.rxandroidble.internal.util.BleConnectionCompat
import spock.lang.Specification
import spock.lang.Unroll

public class RxBleConnectionConnectorOperationsProviderTest extends Specification {

    RxBleConnectionConnectorOperationsProvider provider
    Context mockContext
    BluetoothDevice mockDevice
    RxBleGattCallback mockCallback
    BleConnectionCompat mockConnectionCompat

    def setup() {
        mockContext = Mock Context
        mockDevice = Mock BluetoothDevice
        mockCallback = Mock RxBleGattCallback
        mockConnectionCompat = Mock BleConnectionCompat
        provider = new RxBleConnectionConnectorOperationsProvider()
    }

    @Unroll
    def "provide() should return a android.support.v4.util.Pair with RxBleRadioOperationConnect and RxBleRadioOperationDisconnect #id"() {

        when:
        def pair = provider.provide(mockContext, mockDevice, autoConnectValue, mockConnectionCompat, mockCallback)

        then:
        assert pair instanceof RxBleConnectionConnectorOperationsProvider.RxBleOperations
        assert pair.connect instanceof RxBleRadioOperationConnect
        assert pair.disconnect instanceof RxBleRadioOperationDisconnect

        where:
        id | autoConnectValue
        0  | true
        1  | false
    }

    // TODO: would be great to test if the RxBleRadioOperationDisconnect will get BluetoothGatt through AtomicReference
}