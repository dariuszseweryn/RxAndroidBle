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
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.ReplaySubject;

/**
 * A mocked {@link RxBleClient}. Callers supply device parameters such as services,
 * characteristics and descriptors the mocked client returns them upon request.
 */
public class RxBleClientMock extends RxBleClient {

    public static class Builder {

        private ReplaySubject<RxBleDeviceMock> discoverableDevicesSubject;
        private Set<RxBleDevice> bondedDevices;

        /**
         * Build a new {@link RxBleClientMock}.
         */
        public Builder() {
            this.discoverableDevicesSubject = ReplaySubject.create();
            this.bondedDevices = new HashSet<>();
        }

        public Builder setDeviceDiscoveryObservable(@NonNull Observable<RxBleDeviceMock> discoverableDevicesObservable) {
            discoverableDevicesObservable.subscribe(this.discoverableDevicesSubject);
            return this;
        }

        /**
         * Add a {@link RxBleDevice} to the mock client.
         *
         * @param rxBleDevice device that the mocked client should contain. Use {@link DeviceBuilder} to create them.
         */
        public Builder addDevice(@NonNull RxBleDevice rxBleDevice) {
            this.discoverableDevicesSubject.onNext((RxBleDeviceMock) rxBleDevice);
            return this;
        }

        /**
         * Add a {@link RxBleDevice} to the list of bonded devices.
         *
         * @param rxBleDevice device that the mocked client should contain. Use {@link DeviceBuilder} to create them.
         */
        public Builder addBondedDevice(@NonNull RxBleDevice rxBleDevice) {
            bondedDevices.add(rxBleDevice);
            return this;
        }

        /**
         * Create the {@link RxBleClientMock} instance using the configured values.
         */
        public RxBleClientMock build() {
            return new RxBleClientMock(this);
        }
    }

    public static class DeviceBuilder {

        private int rssi = -1;
        private String deviceName;
        private String deviceMacAddress;
        private byte[] scanRecord;
        private RxBleDeviceServices rxBleDeviceServices;
        private Map<UUID, Observable<byte[]>> characteristicNotificationSources;

        /**
         * Build a new {@link RxBleDevice}.
         * <p>
         * Calling {@link #scanRecord}, {@link #rssi} and {@link #deviceMacAddress}
         * is required before calling {@link #build()}. All other methods
         * are optional.
         */
        public DeviceBuilder() {
            this.rxBleDeviceServices = new RxBleDeviceServices(new ArrayList<BluetoothGattService>());
            this.characteristicNotificationSources = new HashMap<>();
        }

        /**
         * Add a {@link BluetoothGattService} to the device. Calling this method is not required.
         *
         * @param uuid            service UUID
         * @param characteristics characteristics that the service should report. Use {@link CharacteristicsBuilder} to create them.
         */
        public DeviceBuilder addService(@NonNull UUID uuid, @NonNull List<BluetoothGattCharacteristic> characteristics) {
            BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, 0);
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                bluetoothGattService.addCharacteristic(characteristic);
            }
            rxBleDeviceServices.getBluetoothGattServices().add(bluetoothGattService);
            return this;
        }

        /**
         * Create the {@link RxBleDeviceMock} instance using the configured values.
         */
        public RxBleDevice build() {
            if (this.rssi == -1) throw new IllegalStateException("Rssi is required. DeviceBuilder#rssi should be called.");
            if (this.deviceMacAddress == null) throw new IllegalStateException("DeviceMacAddress required."
                    + " DeviceBuilder#deviceMacAddress should be called.");
            if (this.scanRecord == null) throw new IllegalStateException("ScanRecord required. DeviceBuilder#scanRecord should be called.");
            RxBleDeviceMock rxBleDeviceMock = new RxBleDeviceMock(deviceName,
                    deviceMacAddress,
                    scanRecord,
                    rssi,
                    rxBleDeviceServices,
                    characteristicNotificationSources);

            for (BluetoothGattService service : rxBleDeviceServices.getBluetoothGattServices()) {
                rxBleDeviceMock.addAdvertisedUUID(service.getUuid());
            }
            return rxBleDeviceMock;
        }

        /**
         * Set a device mac address. Calling this method is required.
         */
        public DeviceBuilder deviceMacAddress(@NonNull String deviceMacAddress) {
            this.deviceMacAddress = deviceMacAddress;
            return this;
        }

        /**
         * Set a device name. Calling this method is not required.
         */
        public DeviceBuilder deviceName(@NonNull String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        /**
         * Set an {@link Observable} that will be used to fire characteristic change notifications. It will be subscribed to after
         * a call to {@link com.polidea.rxandroidble.RxBleConnection#setupNotification(UUID)}. Calling this method is not required.
         *
         * @param characteristicUUID UUID of the characteristic that will be observed for notifications
         * @param sourceObservable   Observable that will be subscribed to in order to receive characteristic change notifications
         */
        public DeviceBuilder notificationSource(@NonNull UUID characteristicUUID, @NonNull Observable<byte[]> sourceObservable) {
            characteristicNotificationSources.put(characteristicUUID, sourceObservable);
            return this;
        }

        /**
         * Set a rssi that will be reported. Calling this method is not required.
         */
        public DeviceBuilder rssi(int rssi) {
            this.rssi = rssi;
            return this;
        }

        /**
         * Set a BLE scan record. Calling this method is not required.
         */
        public DeviceBuilder scanRecord(@NonNull byte[] scanRecord) {
            this.scanRecord = scanRecord;
            return this;
        }
    }

    public static class CharacteristicsBuilder {

        private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;

        /**
         * Build a new {@link BluetoothGattCharacteristic} list.
         * Should be used in pair with {@link DeviceBuilder#addService}
         */
        public CharacteristicsBuilder() {
            this.bluetoothGattCharacteristics = new ArrayList<>();
        }

        /**
         * Adds a {@link BluetoothGattCharacteristic} with specified parameters.
         *
         * @param uuid        characteristic UUID
         * @param data        locally stored value of the characteristic
         * @param descriptors list of characteristic descriptors. Use {@link DescriptorsBuilder} to create them.
         */
        public CharacteristicsBuilder addCharacteristic(@NonNull UUID uuid,
                                                        @NonNull byte[] data,
                                                        List<BluetoothGattDescriptor> descriptors) {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, 0, 0);
            for (BluetoothGattDescriptor descriptor : descriptors) {
                characteristic.addDescriptor(descriptor);
            }
            characteristic.setValue(data);
            this.bluetoothGattCharacteristics.add(characteristic);
            return this;
        }

        /**
         * Create the {@link List} of {@link BluetoothGattCharacteristic} using the configured values.
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
         * @param data locally stored value of the descriptor
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

    private Set<RxBleDevice> bondedDevices;
    private ReplaySubject<RxBleDeviceMock> discoveredDevicesSubject;

    private RxBleClientMock(Builder builder) {
        bondedDevices = builder.bondedDevices;
        discoveredDevicesSubject = builder.discoverableDevicesSubject;
    }

    @Override
    public RxBleDevice getBleDevice(@NonNull final String macAddress) {
        RxBleDevice rxBleDevice = discoveredDevicesSubject
            .first(new Func1<RxBleDeviceMock, Boolean>() {
                @Override
                public Boolean call(RxBleDeviceMock device) {
                    return device.getMacAddress().equals(macAddress);
                }
            })
            .toBlocking().first();

        if (rxBleDevice == null) {
            throw new IllegalStateException("Mock is not configured for a given mac address. Use Builder#addDevice method.");
        }

        return rxBleDevice;
    }

    @Override
    public Set<RxBleDevice> getBondedDevices() {
        return bondedDevices;
    }

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID... filterServiceUUIDs) {
        return createScanOperation(filterServiceUUIDs);
    }

    private RxBleScanResult convertToPublicScanResult(RxBleDevice bleDevice, Integer rssi, byte[] scanRecord) {
        return new RxBleScanResult(bleDevice, rssi, scanRecord);
    }

    @NonNull
    private Observable<RxBleScanResult> createScanOperation(@Nullable final UUID[] filterServiceUUIDs) {
        return discoveredDevicesSubject
                .filter(new Func1<RxBleDeviceMock, Boolean>() {
                    @Override
                    public Boolean call(RxBleDeviceMock rxBleDevice) {
                        return RxBleClientMock.this.filterDevice(rxBleDevice, filterServiceUUIDs);
                    }
                })
                .map(new Func1<RxBleDeviceMock, RxBleScanResult>() {
                    @Override
                    public RxBleScanResult call(RxBleDeviceMock rxBleDeviceMock) {
                        return RxBleClientMock.this.createRxBleScanResult(rxBleDeviceMock);
                    }
                });
    }

    @NonNull
    private RxBleScanResult createRxBleScanResult(RxBleDeviceMock rxBleDeviceMock) {
        return convertToPublicScanResult(rxBleDeviceMock, rxBleDeviceMock.getRssi(), rxBleDeviceMock.getScanRecord());
    }

    private boolean filterDevice(RxBleDevice rxBleDevice, @Nullable UUID[] filterServiceUUIDs) {

        if (filterServiceUUIDs == null || filterServiceUUIDs.length == 0) {
            return true;
        }

        List<UUID> advertisedUUIDs = ((RxBleDeviceMock) rxBleDevice).getAdvertisedUUIDs();

        for (UUID desiredUUID : filterServiceUUIDs) {

            if (!advertisedUUIDs.contains(desiredUUID)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Observable<ScanResult> scanBleDevices(ScanSettings scanSettings, ScanFilter... scanFilters) {
        return Observable.error(new RuntimeException("not implemented")); // TODO [DS]
    }

    @Override
    public Observable<State> observeStateChanges() {
        return Observable.just(State.READY);
    }

    @Override
    public State getState() {
        return State.READY;
    }
}
