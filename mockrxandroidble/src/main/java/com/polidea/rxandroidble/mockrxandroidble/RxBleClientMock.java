package com.polidea.rxandroidble.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import rx.Observable;

/**
 * A mocked ble client. Callers supply device parameters such as services,
 * characteristics and descriptors the mocked client returns them upon request
 */
public class RxBleClientMock implements RxBleClient {


    public static class Builder {

        private Integer rssi;
        private String deviceName;
        private String deviceMacAddress;
        private byte[] scanRecord;
        private RxBleDeviceServices rxBleDeviceServices;
        private Map<UUID, Observable<byte[]>> characteristicNotificationSources;

        /**
         * Build a new {@link RxBleClientMock}.
         * <p>
         * Calling {@link #rssi}, {@link #deviceMacAddress} and {@link #scanRecord} is required before calling {@link #build()}. All other methods
         * are optional.
         */
        public Builder() {
            this.rxBleDeviceServices = new RxBleDeviceServices(new ArrayList<>());
            this.characteristicNotificationSources = new HashMap<>();
        }

        /**
         * Add a {@link BluetoothGattService} to the device.
         *
         * @param uuid            service UUID
         * @param characteristics characteristics that the service should report. Use {@link CharacteristicsBuilder} to create them.
         */
        public Builder addService(UUID uuid, List<BluetoothGattCharacteristic> characteristics) {
            BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, 0);
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                bluetoothGattService.addCharacteristic(characteristic);
            }
            rxBleDeviceServices.getBluetoothGattServices().add(bluetoothGattService);
            return this;
        }

        /**
         * Create the {@link RxBleClientMock} instance using the configured values.
         */
        public RxBleClientMock build() {
            if (this.rssi == null) throw new IllegalStateException("Rssi required");
            if (this.deviceMacAddress == null) throw new IllegalStateException("DeviceMacAddress required");
            if (this.scanRecord == null) throw new IllegalStateException("ScanRecord required");
            return new RxBleClientMock(this);
        }

        /**
         * Set a device mac address.
         */
        public Builder deviceMacAddress(@NonNull String deviceMacAddress) {
            this.deviceMacAddress = deviceMacAddress;
            return this;
        }

        /**
         * Set a device name.
         */
        public Builder deviceName(@NonNull String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        /**
         * Set an {@link Observable} that will be used to fire characteristic change notifications
         *
         * @param characteristicUUID UUID of the characteristic that will be observed for notifications
         * @param sourceObservable   Observable that will be subscribed to in order to receive characteristic change notifications
         */
        public Builder notificationSource(UUID characteristicUUID, Observable<byte[]> sourceObservable) {
            characteristicNotificationSources.put(characteristicUUID, sourceObservable);
            return this;
        }

        /**
         * Set a rssi that will be reported.
         */
        public Builder rssi(@NonNull Integer rssi) {
            this.rssi = rssi;
            return this;
        }

        /**
         * Set a BLE scan record.
         */
        public Builder scanRecord(@NonNull byte[] scanRecord) {
            this.scanRecord = scanRecord;
            return this;
        }
    }

    public static class CharacteristicsBuilder {

        private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;

        /**
         * Build a new {@link BluetoothGattCharacteristic} list.
         * Should be used in pair with {@link Builder#addService}
         */
        public CharacteristicsBuilder() {
            this.bluetoothGattCharacteristics = new ArrayList<>();
        }

        /**
         * Adds a {@link BluetoothGattCharacteristic} with specified parameters.
         *
         * @param uuid characteristic UUID
         * @param data  locally stored value of the characteristic
         * @param descriptors list of characteristic descriptors. Use {@link DescriptorsBuilder} to create them.
         */
        public CharacteristicsBuilder addCharacteristic(@NonNull UUID uuid, @NonNull byte[] data, List<BluetoothGattDescriptor> descriptors) {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, 0, 0);
            for (BluetoothGattDescriptor descriptor : descriptors) {
                characteristic.addDescriptor(descriptor);
            }
            characteristic.setValue(data);
            this.bluetoothGattCharacteristics.add(characteristic);
            return this;
        }

        /**
         * Create the  {@link List} of {@link BluetoothGattCharacteristic} using the configured values.
         */
        public List<BluetoothGattCharacteristic> build() {
            return bluetoothGattCharacteristics;
        }
    }

    public static class DescriptorsBuilder {

        private List<BluetoothGattDescriptor> bluetoothGattDescriptors;

        /**
         * Build a new {@link BluetoothGattDescriptor} list.
         * Should be used in pair with {@link CharacteristicsBuilder#addCharacteristic}
         */
        public DescriptorsBuilder() {
            this.bluetoothGattDescriptors = new ArrayList<>();
        }

        /**
         * Adds a {@link BluetoothGattDescriptor} with specified parameters.
         *
         * @param uuid descriptor UUID
         * @param data  locally stored value of the descriptor
         */
        public DescriptorsBuilder addDescriptor(@NonNull UUID uuid, @NonNull byte[] data) {
            BluetoothGattDescriptor bluetoothGattDescriptor = new BluetoothGattDescriptor(uuid, 0);
            bluetoothGattDescriptor.setValue(data);
            bluetoothGattDescriptors.add(bluetoothGattDescriptor);
            return this;
        }

        /**
         * Create the  {@link List} of {@link BluetoothGattDescriptor} using the configured values.
         */
        public List<BluetoothGattDescriptor> build() {
            return bluetoothGattDescriptors;
        }
    }

    private RxBleDevice rxBleDevice;
    private Integer rssi;
    private byte[] scanRecord;
    private RxBleConnectionMock rxBleConnectionMock;

    private RxBleClientMock(Builder builder) {
        rxBleConnectionMock = new RxBleConnectionMock(builder.rxBleDeviceServices, builder.rssi, builder.characteristicNotificationSources);
        rxBleDevice = new RxBleDeviceMock(builder.deviceName, builder.deviceMacAddress, rxBleConnectionMock);
        rssi = builder.rssi;
        scanRecord = builder.scanRecord;
    }

    /**
     * Allows to simulate a situation when the Bluetooth device disconnected itself
     * i.e. because of an error.
     * After calling this method, {@link RxBleDevice} status will be changed to <i>DISCONNECTED</i>
     * and all of the observers that are subscribed to {@link com.polidea.rxandroidble.RxBleConnection} will get an <b>onError()</b> call
     * with {@link BleDisconnectedException}
     */
    public void disconnect() {
        rxBleConnectionMock.simulateDeviceDisconnect();
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
