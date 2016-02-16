package com.polidea.rxandroidble.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleScanResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.Observable;

public class RxBleClientMock implements RxBleClient {

    public static class Builder {

        private Integer rssi;
        private String deviceName;
        private String deviceMacAddress;
        private byte[] scanRecord;

        private RxBleDeviceServices rxBleDeviceServices;

        public RxBleClientMock build() {
            return new RxBleClientMock(this);
        }

        public Builder deviceMacAddress(String deviceMacAddress) {
            this.deviceMacAddress = deviceMacAddress;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder rssi(Integer rssi) {
            this.rssi = rssi;
            return this;
        }

        public Builder rxBleDeviceServices(RxBleDeviceServices rxBleDeviceServices) {
            this.rxBleDeviceServices = rxBleDeviceServices;
            return this;
        }

        public Builder scanRecord(byte[] scanRecord) {
            this.scanRecord = scanRecord;
            return this;
        }
    }

    public static class ServicesBuilder {

        private List<BluetoothGattService> bluetoothGattServices;

        public ServicesBuilder() {
            this.bluetoothGattServices = new ArrayList<>();
        }

        public ServicesBuilder addService(UUID uuid, List<BluetoothGattCharacteristic> characteristics) {
            BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, 0);
            for(BluetoothGattCharacteristic characteristic : characteristics) {
                bluetoothGattService.addCharacteristic(characteristic);
            }
            bluetoothGattServices.add(bluetoothGattService);
            return this;
        }

        public RxBleDeviceServices build() {
            return new RxBleDeviceServices(bluetoothGattServices);
        }
    }


    public static class CharacteristicsBuilder {

        private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;

        public CharacteristicsBuilder() {
            this.bluetoothGattCharacteristics = new ArrayList<>();
        }

        public CharacteristicsBuilder addCharacteristic(UUID uuid, byte[] data, List<BluetoothGattDescriptor> descriptors) {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, 0, 0);
            for(BluetoothGattDescriptor descriptor : descriptors) {
                characteristic.addDescriptor(descriptor);
            }
            characteristic.setValue(data);
            this.bluetoothGattCharacteristics.add(characteristic);
            return this;
        }

        public List<BluetoothGattCharacteristic> build() {
            return bluetoothGattCharacteristics;
        }
    }

    public static class DescriptorsBuilder {

        private List<BluetoothGattDescriptor> bluetoothGattDescriptors;

        public DescriptorsBuilder() {
            this.bluetoothGattDescriptors = new ArrayList<>();
        }

        public DescriptorsBuilder addDescriptor(UUID uuid, byte[] data) {
            BluetoothGattDescriptor bluetoothGattDescriptor = new BluetoothGattDescriptor(uuid, 0);
            bluetoothGattDescriptor.setValue(data);
            bluetoothGattDescriptors.add(bluetoothGattDescriptor);
            return this;
        }

        public List<BluetoothGattDescriptor> build() {
            return bluetoothGattDescriptors;
        }
    }

    private RxBleDevice rxBleDevice;
    private Integer rssi;
    private byte[] scanRecord;


    private RxBleClientMock(Builder builder) {
        rxBleDevice = new RxBleDeviceMock(builder.deviceName, builder.deviceMacAddress, new RxBleConnectionMock(builder.rxBleDeviceServices, builder.rssi));
        rssi = builder.rssi;
        scanRecord = builder.scanRecord;
    }

    @Override
    public RxBleDevice getBleDevice(String bluetoothAddress) {
        return rxBleDevice;
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {
        return Observable.just(new RxBleScanResult(rxBleDevice, rssi, scanRecord));
    }
}
