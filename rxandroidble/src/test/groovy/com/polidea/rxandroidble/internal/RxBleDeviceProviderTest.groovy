package com.polidea.rxandroidble.internal

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.internal.cache.DeviceComponentCache
import rx.Observable
import spock.lang.Specification

import javax.inject.Provider

class RxBleDeviceProviderTest extends Specification {

    class StubDevice implements RxBleDevice {

        StubDevice(String macAddress) {
            this.macAddress = macAddress
        }

        private final String macAddress;

        @Override
        Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges() {
            throw UnsupportedOperationException()
        }

        @Override
        RxBleConnection.RxBleConnectionState getConnectionState() {
            throw UnsupportedOperationException()
        }

        @Override
        @Deprecated
        Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
            establishConnection(autoConnect)
        }

        @Override
        Observable<RxBleConnection> establishConnection(boolean autoConnect) {
            throw UnsupportedOperationException()
        }

        @Override
        String getName() {
            throw UnsupportedOperationException()
        }

        @Override
        String getMacAddress() {
            return macAddress
        }

        @Override
        BluetoothDevice getBluetoothDevice() {
            throw UnsupportedOperationException()
        }
    }

    RxBleDeviceProvider objectUnderTest

    def setup() {


        objectUnderTest = new RxBleDeviceProvider(
                new DeviceComponentCache(),
                new Provider<DeviceComponent.Builder>() {

                    @Override
                    DeviceComponent.Builder get() {
                        return new DeviceComponent.Builder() {

                            private RxBleDevice device

                            @Override
                            DeviceComponent build() {
                                return new DeviceComponent() {
                                    @Override
                                    RxBleDevice provideDevice() {
                                        return device
                                    }
                                }
                            }

                            @Override
                            DeviceComponent.Builder deviceModule(DeviceModule module) {
                                this.device = new StubDevice(module.macAddress)
                                return this
                            }
                        }
                    }
                }
        )
    }

    def "should return new BleDevice if getBleDevice was called for the first time"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)

        then:
        device.macAddress == macAddress
    }

    def "should return cached BleDevice for the same mac address"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)
        def secondDevice = objectUnderTest.getBleDevice(macAddress)

        then:
        device.is secondDevice
    }

    def "should return new BleDevice for the same mac address if reference was garbage collected"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'
        def device = objectUnderTest.getBleDevice(macAddress)
        def firstDeviceHashCode = System.identityHashCode(device)

        when:
        device = null
        System.gc()

        then:
        def secondDevice = objectUnderTest.getBleDevice(macAddress)
        firstDeviceHashCode != System.identityHashCode(secondDevice)
    }

    def "should return new BleDevice even though previously there was a call for different mac address"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'
        def differentAddress = 'BB:BB:BB:BB:BB:BB'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)
        def secondDevice = objectUnderTest.getBleDevice(differentAddress)

        then:
        device != secondDevice

        and:
        secondDevice.macAddress == differentAddress
    }
}
