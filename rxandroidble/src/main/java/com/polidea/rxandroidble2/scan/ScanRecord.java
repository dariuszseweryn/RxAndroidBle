package com.polidea.rxandroidble2.scan;

import android.os.ParcelUuid;
import androidx.annotation.Nullable;

import android.util.SparseArray;

import java.util.List;
import java.util.Map;

public interface ScanRecord {

    /**
     * Returns the advertising flags indicating the discoverable mode and capability of the device.
     * Returns -1 if the flag field is not set.
     */
    int getAdvertiseFlags();

    /**
     * Returns a list of service UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services.
     */
    List<ParcelUuid> getServiceUuids();

    /**
     * Returns a list of service solicitation UUIDs within the advertisement that are used to identify the
     * bluetooth GATT services the peripheral requires on the Central.
     */
    List<ParcelUuid> getServiceSolicitationUuids();

    /**
     * Returns a sparse array of manufacturer identifier and its corresponding manufacturer specific
     * data.
     */
    SparseArray<byte[]> getManufacturerSpecificData();

    /**
     * Returns the manufacturer specific data associated with the manufacturer id. Returns
     * {@code null} if the {@code manufacturerId} is not found.
     */
    @Nullable
    byte[] getManufacturerSpecificData(int manufacturerId);

    /**
     * Returns a map of service UUID and its corresponding service data.
     */
    Map<ParcelUuid, byte[]> getServiceData();

    /**
     * Returns the service data byte array associated with the {@code serviceUuid}. Returns
     * {@code null} if the {@code serviceDataUuid} is not found.
     */
    @Nullable
    byte[] getServiceData(ParcelUuid serviceDataUuid);

    /**
     * Returns the transmission power level of the packet in dBm. Returns {@link Integer#MIN_VALUE}
     * if the field is not set. This value can be used to calculate the path loss of a received
     * packet using the following equation:
     * <p>
     * <code>pathloss = txPowerLevel - rssi</code>
     */
    int getTxPowerLevel();

    /**
     * Returns the local name of the BLE device. The is a UTF-8 encoded string.
     */
    @Nullable
    String getDeviceName();

    /**
     * Returns raw bytes of scan record.
     */
    byte[] getBytes();
}
