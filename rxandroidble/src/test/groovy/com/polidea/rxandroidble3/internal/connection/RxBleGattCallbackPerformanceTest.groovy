package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.TestScheduler
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import static android.bluetooth.BluetoothGatt.GATT_FAILURE
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS

class RxBleGattCallbackPerformanceTest extends Specification {

    def objectUnderTest = new RxBleGattCallback(new TestScheduler(), Mock(BluetoothGattProvider), mockDisconnectionRouter, new NativeCallbackDispatcher())
    @Shared
    def mockDisconnectionRouter = Mock DisconnectionRouter
    @Shared
    def mockBluetoothGatt = Mock BluetoothGatt
    @Shared
    def mockBluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    @Shared
    def mockBluetoothDevice = Mock BluetoothDevice
    @Shared
    def mockBluetoothDeviceMacAddress = "MacAddress"
    def iterationsCount = 1000000

    def setupSpec() {
        mockDisconnectionRouter.asObservable() >> Observable.empty()
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockBluetoothDeviceMacAddress
    }

    def "sanity check"() {

        expect:
        GATT_SUCCESS != GATT_FAILURE
    }

    @Ignore // not needed to be performed each time
    def "performance test gatt callback using RxJava API"() {
        given:
        def startedTimestamp = System.currentTimeMillis()
        objectUnderTest.onCharacteristicRead.test()

        when:
        invokeCharacteristicReadCallback(iterationsCount)

        then:
        testSubscriber.assertValueCount(iterationsCount)
        println("Test read callbacks with $iterationsCount took ${System.currentTimeMillis() - startedTimestamp}ms (Rx API)")
    }

    @Ignore // not needed to be performed each time
    def "performance test gatt callback using native callbacks"() {
        given:
        def startedTimestamp = System.currentTimeMillis()
        def testCallback = new TestCallback()
        objectUnderTest.setNativeCallback(testCallback)

        when:
        invokeCharacteristicReadCallback(iterationsCount)

        then:
        testCallback.readCount == iterationsCount
        println("Test read callbacks with $iterationsCount took ${System.currentTimeMillis() - startedTimestamp}ms (Native API)")
    }

    private invokeCharacteristicReadCallback(int iterationCount = 1) {
        iterationCount.times {
            objectUnderTest.getBluetoothGattCallback().onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_SUCCESS)
        }
    }

    static class TestCallback extends BluetoothGattCallback {
        int readCount = 0

        @Override
        void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCount++
        }
    }
}
