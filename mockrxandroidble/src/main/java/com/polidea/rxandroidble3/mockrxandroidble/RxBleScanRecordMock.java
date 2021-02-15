package com.polidea.rxandroidble3.mockrxandroidble;

import android.os.ParcelUuid;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.mockrxandroidble.internal.ScanRecordDataConstructor;
import com.polidea.rxandroidble3.scan.ScanRecord;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A mocked {@link ScanRecord}. Callers supply record parameters such as advertising flags,
 * service UUIDs, manufacturer specific data.
 */
public class RxBleScanRecordMock implements ScanRecord {

    private final int advertiseFlags;
    private final List<ParcelUuid> serviceUuids;
    private final List<ParcelUuid> serviceSolicitationUuids;
    private final SparseArray<byte[]> manufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> serviceData;
    private final int txPowerLevel;
    private final String deviceName;
    private final byte[] bytes;

    public RxBleScanRecordMock(
            int advertiseFlags,
            List<ParcelUuid> serviceUuids,
            List<ParcelUuid> serviceSolicitationUuids,
            SparseArray<byte[]> manufacturerSpecificData,
            Map<ParcelUuid, byte[]> serviceData,
            int txPowerLevel,
            String deviceName,
            boolean requireLegacyDataLength) throws UnsupportedEncodingException, IllegalArgumentException {
        this.advertiseFlags = advertiseFlags;
        this.serviceUuids = serviceUuids;
        this.serviceSolicitationUuids = serviceSolicitationUuids;
        this.manufacturerSpecificData = manufacturerSpecificData;
        this.serviceData = serviceData;
        this.txPowerLevel = txPowerLevel;
        this.deviceName = deviceName;
        this.bytes = new ScanRecordDataConstructor(requireLegacyDataLength).constructBytesFromScanRecord(this);
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

    @Nullable
    @Override
    public List<ParcelUuid> getServiceSolicitationUuids() {
        return serviceSolicitationUuids;
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

    /**
     * Builder class for {@link RxBleScanRecordMock}.
     */
    public static class Builder {
        private int advertiseFlags = -1;
        private final List<ParcelUuid> serviceUuids;
        private final List<ParcelUuid> serviceSolicitationUuids;
        private final SparseArray<byte[]> manufacturerSpecificData;
        private final Map<ParcelUuid, byte[]> serviceData;
        private int txPowerLevel = Integer.MIN_VALUE;
        private String deviceName = null;
        private boolean requireLegacyDataLength = false;

        public Builder() {
            serviceUuids = new ArrayList<>();
            serviceSolicitationUuids = new ArrayList<>();
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
         * Add a service solicitation UUID
         */
        public Builder addServiceSolicitionUuid(ParcelUuid uuid) {
            serviceSolicitationUuids.add(uuid);
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
         * Set if the ScanRecord will generate a byte array that fits into the legacy 31 byte advertisement packets (true) or will generate
         * an extended advertisement packet (false)
         */
        public Builder setRequireLegacyDataLength(boolean requireLegacyDataLength) {
            this.requireLegacyDataLength = requireLegacyDataLength;
            return this;
        }

        /**
         * Build {@link RxBleScanRecordMock}.
         *
         * @throws UnsupportedEncodingException If the device name could not be encoded
         * @throws IllegalArgumentException     If the data could not fit in legacy packets
         */
        public RxBleScanRecordMock build() throws UnsupportedEncodingException, IllegalArgumentException {
            return new RxBleScanRecordMock(
                    advertiseFlags,
                    serviceUuids,
                    serviceSolicitationUuids,
                    manufacturerSpecificData,
                    serviceData,
                    txPowerLevel,
                    deviceName,
                    requireLegacyDataLength
            );
        }
    }
}
