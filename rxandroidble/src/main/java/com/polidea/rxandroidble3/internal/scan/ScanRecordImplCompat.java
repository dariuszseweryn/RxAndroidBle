package com.polidea.rxandroidble3.internal.scan;

import android.os.ParcelUuid;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.util.SparseArray;

import com.polidea.rxandroidble3.scan.ScanRecord;

import java.util.List;
import java.util.Map;

/**
 * Copy of v21 {@link android.bluetooth.le.ScanRecord} without parsing code
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanRecordImplCompat implements ScanRecord {

    // Flags of the advertising data.
    private final int advertiseFlags;

    @Nullable
    private final List<ParcelUuid> serviceUuids;

    @Nullable
    private final List<ParcelUuid> serviceSolicitationUuids;

    private final SparseArray<byte[]> manufacturerSpecificData;

    private final Map<ParcelUuid, byte[]> serviceData;

    // Transmission power level(in dB).
    private final int txPowerLevel;

    // Local name of the Bluetooth LE device.
    private final String deviceName;

    // Raw bytes of scan record.
    private final byte[] bytes;

    public ScanRecordImplCompat(
            @Nullable List<ParcelUuid> serviceUuids,
            @Nullable List<ParcelUuid> serviceSolicitationUuids,
            SparseArray<byte[]> manufacturerData,
            Map<ParcelUuid, byte[]> serviceData,
            int advertiseFlags,
            int txPowerLevel,
            String localName,
            byte[] bytes
    ) {
        this.serviceUuids = serviceUuids;
        this.serviceSolicitationUuids = serviceSolicitationUuids;
        this.manufacturerSpecificData = manufacturerData;
        this.serviceData = serviceData;
        this.deviceName = localName;
        this.advertiseFlags = advertiseFlags;
        this.txPowerLevel = txPowerLevel;
        this.bytes = bytes;
    }

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device.
     * Returns -1 if the flag field is not set.
     */
    public int getAdvertiseFlags() {
        return advertiseFlags;
    }

    /**
     * Returns a list of service UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services.
     */
    @Nullable
    public List<ParcelUuid> getServiceUuids() {
        return serviceUuids;
    }

    /**
     * Returns a list of service solicitation UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services the peripheral requires on the Central.
     */
    @Nullable
    public List<ParcelUuid> getServiceSolicitationUuids() {
        return serviceSolicitationUuids;
    }

    /**
     * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
     * data.
     */
    public SparseArray<byte[]> getManufacturerSpecificData() {
        return manufacturerSpecificData;
    }

    /**
     * Returns the manufacturer specific data associated with the manufacturer id. Returns
     * {@code null} if the {@code manufacturerId} is not found.
     */
    @Nullable
    public byte[] getManufacturerSpecificData(int manufacturerId) {
        return manufacturerSpecificData.get(manufacturerId);
    }

    /**
     * Returns a map of service UUID and its corresponding service data.
     */
    public Map<ParcelUuid, byte[]> getServiceData() {
        return serviceData;
    }

    /**
     * Returns the service data byte array associated with the {@code serviceUuid}. Returns
     * {@code null} if the {@code serviceDataUuid} is not found.
     */
    @Nullable
    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null) {
            return null;
        }
        return serviceData.get(serviceDataUuid);
    }

    /**
     * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE}
     * if the field is not set. This value can be used to calculate the path loss of a received
     * packet using the following equation:
     * <p>
     * <code>pathloss = txPowerLevel - rssi</code>
     */
    public int getTxPowerLevel() {
        return txPowerLevel;
    }

    /**
     * Returns the local name of the BLE device. The is a UTF-8 encoded string.
     */
    @Nullable
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns raw bytes of scan record.
     */
    public byte[] getBytes() {
        return bytes;
    }
}
