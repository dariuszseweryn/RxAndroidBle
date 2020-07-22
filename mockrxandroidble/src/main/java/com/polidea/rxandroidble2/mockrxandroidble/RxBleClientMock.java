package com.polidea.rxandroidble2.mockrxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.scan.BackgroundScanner;
import com.polidea.rxandroidble2.scan.ScanCallbackType;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanRecord;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.ReplaySubject;

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
        private byte[] legacyScanRecord;
        private ScanRecord scanRecord;
        private RxBleDeviceServices rxBleDeviceServices;
        private BluetoothDevice bluetoothDevice;
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
            if (this.scanRecord == null && this.legacyScanRecord == null) throw new IllegalStateException("ScanRecord required. DeviceBuilder#scanRecord should be called.");
            RxBleDeviceMock rxBleDeviceMock;
            if(scanRecord == null) {
                 rxBleDeviceMock = new RxBleDeviceMock(deviceName,
                        deviceMacAddress,
                        legacyScanRecord,
                        rssi,
                        rxBleDeviceServices,
                        characteristicNotificationSources,
                        bluetoothDevice);
            } else {
                rxBleDeviceMock = new RxBleDeviceMock(deviceName,
                        deviceMacAddress,
                        scanRecord,
                        rssi,
                        rxBleDeviceServices,
                        characteristicNotificationSources,
                        bluetoothDevice);
            }

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
         * Sets a bluetooth device. Calling this method is not required.
         */
        public DeviceBuilder bluetoothDevice(@NonNull BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;
            return this;
        }

        /**
         * Set an {@link Observable} that will be used to fire characteristic change notifications. It will be subscribed to after
         * a call to {@link com.polidea.rxandroidble2.RxBleConnection#setupNotification(UUID)}. Calling this method is not required.
         *
         * @param characteristicUUID UUID of the characteristic that will be observed for notifications
         * @param sourceObservable   Observable that will be subscribed to in order to receive characteristic change notifications
         */
        public DeviceBuilder notificationSource(@NonNull UUID characteristicUUID, @NonNull Observable<byte[]> sourceObservable) {
            characteristicNotificationSources.put(characteristicUUID, sourceObservable);
            return this;
        }

        /**
         * Set a rssi that will be reported. Calling this method is required.
         */
        public DeviceBuilder rssi(int rssi) {
            this.rssi = rssi;
            return this;
        }

        /**
         * Set a BLE scan record. Calling either this method or the other scanRecord method is required.
         */
        public DeviceBuilder scanRecord(@NonNull byte[] scanRecord) {
            this.legacyScanRecord = scanRecord;
            return this;
        }

        /**
         * Set a BLE scan record. Calling this method is required.
         */
        public DeviceBuilder scanRecord(@NonNull ScanRecord scanRecord) {
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
            return addCharacteristic(uuid, data, 0, descriptors);
        }

        /**
         * Adds a {@link BluetoothGattCharacteristic} with specified parameters.
         *
         * @param uuid        characteristic UUID
         * @param data        locally stored value of the characteristic
         * @param properties  OR-ed {@link BluetoothGattCharacteristic} property constants
         * @param descriptors list of characteristic descriptors. Use {@link DescriptorsBuilder} to create them.
         */
        public CharacteristicsBuilder addCharacteristic(@NonNull UUID uuid,
                                                        @NonNull byte[] data,
                                                        int properties,
                                                        List<BluetoothGattDescriptor> descriptors) {
            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid, properties, 0);
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
                .filter(new Predicate<RxBleDeviceMock>() {
                    @Override
                    public boolean test(RxBleDeviceMock device) {
                        return device.getMacAddress().equals(macAddress);
                    }
                })
                .firstOrError()
                .blockingGet();

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

    private static RxBleScanResult convertToPublicLegacyScanResult(RxBleDevice bleDevice, Integer rssi, byte[] scanRecord) {
        return new RxBleScanResult(bleDevice, rssi, scanRecord);
    }

    @NonNull
    private Observable<RxBleScanResult> createScanOperation(@Nullable final UUID[] filterServiceUUIDs) {
        return discoveredDevicesSubject
                .filter(new Predicate<RxBleDeviceMock>() {
                    @Override
                    public boolean test(RxBleDeviceMock rxBleDevice) {
                        return RxBleClientMock.filterDevice(rxBleDevice, filterServiceUUIDs);
                    }
                })
                .map(new Function<RxBleDeviceMock, RxBleScanResult>() {
                    @Override
                    public RxBleScanResult apply(RxBleDeviceMock rxBleDeviceMock) {
                        return RxBleClientMock.this.createRxBleScanResult(rxBleDeviceMock);
                    }
                });
    }

    @NonNull
    private RxBleScanResult createRxBleScanResult(RxBleDeviceMock rxBleDeviceMock) {
        return convertToPublicLegacyScanResult(rxBleDeviceMock, rxBleDeviceMock.getRssi(), rxBleDeviceMock.getLegacyScanRecord());
    }

    private static boolean filterDevice(RxBleDevice rxBleDevice, @Nullable UUID[] filterServiceUUIDs) {

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
        return createScanOperation(scanSettings, scanFilters);
    }

    private static ScanResult convertToPublicScanResult(RxBleDevice bleDevice, Integer rssi, ScanRecord scanRecord) {
        return new ScanResult(bleDevice, rssi, System.currentTimeMillis()*1000000, ScanCallbackType.CALLBACK_TYPE_FIRST_MATCH, scanRecord);
    }

    @NonNull
    private ScanResult createScanResult(RxBleDeviceMock rxBleDeviceMock) {
        return convertToPublicScanResult(rxBleDeviceMock, rxBleDeviceMock.getRssi(), rxBleDeviceMock.getScanRecord());
    }

    @NonNull
    private Observable<ScanResult> createScanOperation(ScanSettings scanSettings, final ScanFilter... scanFilters) {
        return discoveredDevicesSubject
                .filter(new Predicate<RxBleDeviceMock>() {
                    @Override
                    public boolean test(RxBleDeviceMock rxBleDevice) {
                        return RxBleClientMock.filterDevice(rxBleDevice, scanFilters);
                    }
                })
                .map(new Function<RxBleDeviceMock, ScanResult>() {
                    @Override
                    public ScanResult apply(RxBleDeviceMock rxBleDeviceMock) {
                        return RxBleClientMock.this.createScanResult(rxBleDeviceMock);
                    }
                });
    }

    private static boolean maskedDataEquals(@NonNull byte[] data1, @NonNull byte[] data2, @Nullable byte[] mask) {
        if(mask == null) {
            return Arrays.equals(data1, data2);
        } else {
            if(data1.length != data2.length || data1.length != mask.length) {
                return false;
            }
            for(int i = 0; i < data1.length; i++) {
                if((data1[i] & mask[i]) != (data2[i] & mask[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static byte[] getDataFromUUID(@NonNull UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static boolean filterDevice(RxBleDevice rxBleDevice, ScanFilter... scanFilters) {

        if (scanFilters == null || scanFilters.length == 0) {
            return true;
        }
        RxBleDeviceMock mock = (RxBleDeviceMock)rxBleDevice;
        ScanRecord scanRecord = mock.getScanRecord();
        if(scanRecord == null) {
            return false;
        }

        String mac = mock.getMacAddress();
        String name = scanRecord.getDeviceName();
        Set<UUID> advertisedUUIDs = new HashSet<>(mock.getAdvertisedUUIDs());
        List<ParcelUuid> scanRecordAdvertisedUUIDs = scanRecord.getServiceUuids();
        if(scanRecordAdvertisedUUIDs != null) {
            for (ParcelUuid uuid : scanRecordAdvertisedUUIDs) {
                advertisedUUIDs.add(uuid.getUuid());
            }
        }

        for (ScanFilter filter : scanFilters) {
            ParcelUuid serviceUUIDMask = filter.getServiceUuidMask();
            ParcelUuid serviceUUID = filter.getServiceUuid();
            if(serviceUUIDMask != null && serviceUUID != null) {
                byte[] serviceUUIDMaskData = RxBleClientMock.getDataFromUUID(serviceUUIDMask.getUuid());
                byte[] serviceUUIDData = RxBleClientMock.getDataFromUUID(serviceUUID.getUuid());
                boolean found = false;
                for (UUID uuid: advertisedUUIDs) {
                    byte[] UUIDData = RxBleClientMock.getDataFromUUID(uuid);
                    if(RxBleClientMock.maskedDataEquals(serviceUUIDData, UUIDData, serviceUUIDMaskData)) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    return false;
                }
            } else if(serviceUUID != null && !advertisedUUIDs.contains(serviceUUID.getUuid())) {
                return false;
            }

            if(filter.getDeviceAddress() != null && !filter.getDeviceAddress().equals(mac)) {
                return false;
            }

            if(filter.getDeviceName() != null && !filter.getDeviceName().equals(name)) {
                return false;
            }

            byte[] manuFilterData = filter.getManufacturerData();
            if(manuFilterData != null) {
                byte[] manuData = scanRecord.getManufacturerSpecificData(filter.getManufacturerId());
                if(manuData == null || !RxBleClientMock.maskedDataEquals(manuData, manuFilterData, filter.getManufacturerDataMask())) {
                    return false;
                }
            }
            byte[] filterServiceData = filter.getServiceData();
            ParcelUuid serviceDataUuid = filter.getServiceDataUuid();
            if(serviceDataUuid != null && filterServiceData != null) {
                byte[] serviceData = scanRecord.getServiceData(serviceDataUuid);
                if(serviceData == null || !RxBleClientMock.maskedDataEquals(filterServiceData, serviceData, filter.getServiceDataMask())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public BackgroundScanner getBackgroundScanner() {
        throw new UnsupportedOperationException("Background scanning API is not supported by the mock.");
    }

    @Override
    public Observable<State> observeStateChanges() {
        return Observable.just(State.READY);
    }

    @Override
    public State getState() {
        return State.READY;
    }

    @Override
    public boolean isScanRuntimePermissionGranted() {
        return true;
    }

    @Override
    public String[] getRecommendedScanRuntimePermissions() {
        return new String[0];
    }
}
