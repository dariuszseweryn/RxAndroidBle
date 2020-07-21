package com.polidea.rxandroidble2.mockrxandroidble;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.RxBleScanResult;
import com.polidea.rxandroidble2.scan.ScanRecord;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mocked {@link ScanRecord}. Callers supply record parameters such as advertising flags,
 * service UUIDs, manufacturer specific data.
 */
public class RxBleScanRecordMock implements ScanRecord {

    private int advertiseFlags;
    private List<ParcelUuid> serviceUuids;
    private SparseArray<byte[]> manufacturerSpecificData;
    private Map<ParcelUuid, byte[]> serviceData;
    private int txPowerLevel;
    private String deviceName;
    private byte[] bytes;

    public RxBleScanRecordMock(
        int advertiseFlags,
        List<ParcelUuid> serviceUuids,
        SparseArray<byte[]> manufacturerSpecificData,
        Map<ParcelUuid, byte[]> serviceData,
        int txPowerLevel,
        String deviceName,
        byte[] bytes) {
        this.advertiseFlags = advertiseFlags;
        this.serviceUuids = serviceUuids;
        this.manufacturerSpecificData = manufacturerSpecificData;
        this.serviceData = serviceData;
        this.txPowerLevel = txPowerLevel;
        this.deviceName = deviceName;
        this.bytes = bytes;
    }

    /**
     * Builder class for {@link RxBleScanRecordMock}.
     */
    public class Builder {
        private int advertiseFlags;
        private List<ParcelUuid> serviceUuids;
        private SparseArray<byte[]> manufacturerSpecificData;
        private Map<ParcelUuid, byte[]> serviceData;
        private int txPowerLevel;
        private String deviceName;
        private byte[] bytes;

        public Builder() {
            serviceUuids = new ArrayList<ParcelUuid>();
            manufacturerSpecificData = new SparseArray<>();
            serviceData = new HashMap<>();
        }

        /**
         * Set the advertise flags
         */
        public Builder setAdvertiseFlags(int advertiseFlags) {
            this.advertiseFlags = advertiseFlags;
            return this;
        }

        /**
         * Add a service UUID
         */
        public Builder addServiceUuid(ParcelUuid uuid) {
            serviceUuids.add(uuid);
            return this;
        }

        /**
         * Add manufacturer specific data
         */
        public Builder addManufacturerSpecificData(int manufacturerId, byte[] manufacturerSpecificData) {
            this.manufacturerSpecificData.append(manufacturerId, manufacturerSpecificData);
            return this;
        }

        /**
         * Add service data
         */
        public Builder addServiceData(ParcelUuid uuid, byte[] data) {
            serviceData.put(uuid, data);
            return this;
        }

        /**
         * Set the tx power level
         */
        public Builder setTxPowerLevel(int txPowerLevel) {
            this.txPowerLevel = txPowerLevel;
            return this;
        }

        /**
         * Set the device name
         */
        public Builder setDeviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        /**
         * Build {@link RxBleScanRecordMock}.
         *
         * @throws IllegalArgumentException If the scan record cannot be built.
         */
        public RxBleScanRecordMock build() {
            // TODO: compile to bytes
            bytes = new byte[0];
            return new RxBleScanRecordMock(
                    advertiseFlags,
                    serviceUuids,
                    manufacturerSpecificData,
                    serviceData,
                    txPowerLevel,
                    deviceName,
                    bytes
                    );
        }
    }

    @Override
    public int getAdvertiseFlags() {
        return advertiseFlags;
    }

    @Nullable
    @Override
    public List<ParcelUuid> getServiceUuids() {
        return serviceUuids;
    }

    @Override
    public SparseArray<byte[]> getManufacturerSpecificData() {
        return manufacturerSpecificData;
    }

    @Nullable
    @Override
    public byte[] getManufacturerSpecificData(int manufacturerId) {
        return manufacturerSpecificData.get(manufacturerId);
    }

    @Override
    public Map<ParcelUuid, byte[]> getServiceData() {
        return serviceData;
    }

    @Nullable
    @Override
    public byte[] getServiceData(ParcelUuid serviceDataUuid) {
        return serviceData.get(serviceDataUuid);
    }

    @Override
    public int getTxPowerLevel() {
        return txPowerLevel;
    }

    @Nullable
    @Override
    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }
}
