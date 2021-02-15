package com.polidea.rxandroidble3.mockrxandroidble.internal;

import android.os.ParcelUuid;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.scan.ScanRecord;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class ScanRecordDataConstructor {

    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private enum AdvDataType {
        Flags(0x01),
        ServiceUuids16BitPartial(0x02),
        ServiceUuids16BitComplete(0x03),
        ServiceUuids32BitPartial(0x04),
        ServiceUuids32BitComplete(0x05),
        ServiceUuids128BitPartial(0x06),
        ServiceUuids128BitComplete(0x07),
        LocalNameShort(0x08),
        LocalNameComplete(0x09),
        TxPowerLevel(0x0A),
        ServiceData16Bit(0x16),
        ServiceData32Bit(0x20),
        ServiceData128Bit(0x21),
        ManufacturerSpecificData(0xFF);

        byte value;
        AdvDataType(int value) {
            this.value = (byte) value;
        }
        public byte getValue() {
            return value;
        }
    }

    private static final UUID BASE_UUID =
            UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /** Length of bytes for 16 bit UUID */
    private static final int UUID_BYTES_16_BIT = 2;
    /** Length of bytes for 32 bit UUID */
    private static final int UUID_BYTES_32_BIT = 4;
    /** Length of bytes for 128 bit UUID */
    private static final int UUID_BYTES_128_BIT = 16;

    private static final int DEVICE_NAME_MAX = 26;

    private static final int LEGACY_MAX_DATA_LENGTH = 31;

    private ByteArrayOutputStream advertisementPacket;
    private ByteArrayOutputStream scanResponsePacket;
    private boolean requireLegacyDataLength;

    public ScanRecordDataConstructor(boolean requireLegacyDataLength) {
        this.requireLegacyDataLength = requireLegacyDataLength;
    }

    /**
     * Extract the Service Identifier or the actual uuid from the Parcel Uuid.
     * For example, if 0000110B-0000-1000-8000-00805F9B34FB is the parcel Uuid,
     * this function will return 110B
     *
     * @param parcelUuid
     * @return the service identifier.
     */
    private static int getServiceIdentifierFromParcelUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        long value = (uuid.getMostSignificantBits() & 0xFFFFFFFF00000000L) >>> 32;
        return (int) value;
    }

    /**
     * Parse UUID to bytes. The returned value is shortest representation, a 16-bit, 32-bit or
     * 128-bit UUID, Note returned value is little endian (Bluetooth).
     *
     * @param uuid uuid to parse.
     * @return shortest representation of {@code uuid} as bytes.
     * @throws IllegalArgumentException If the {@code uuid} is null.
     *
     * @hide
     */
    public static byte[] uuidToBytes(ParcelUuid uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        if (is16BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_16_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            return uuidBytes;
        }
        if (is32BitUuid(uuid)) {
            byte[] uuidBytes = new byte[UUID_BYTES_32_BIT];
            int uuidVal = getServiceIdentifierFromParcelUuid(uuid);
            uuidBytes[0] = (byte) (uuidVal & 0xFF);
            uuidBytes[1] = (byte) ((uuidVal & 0xFF00) >> 8);
            uuidBytes[2] = (byte) ((uuidVal & 0xFF0000) >> 16);
            uuidBytes[3] = (byte) ((uuidVal & 0xFF000000) >> 24);
            return uuidBytes;
        }
        // Construct a 128 bit UUID.
        long msb = uuid.getUuid().getMostSignificantBits();
        long lsb = uuid.getUuid().getLeastSignificantBits();
        byte[] uuidBytes = new byte[UUID_BYTES_128_BIT];
        ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(8, msb);
        buf.putLong(0, lsb);
        return uuidBytes;
    }
    /**
     * Check whether the given parcelUuid can be converted to 16 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 16 bit uuid, false otherwise.
     *
     * @hide
     */
    public static boolean is16BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getLeastSignificantBits()) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFF0000FFFFFFFFL) == 0x1000L);
    }
    /**
     * Check whether the given parcelUuid can be converted to 32 bit bluetooth uuid.
     *
     * @param parcelUuid
     * @return true if the parcelUuid can be converted to 32 bit uuid, false otherwise.
     *
     * @hide
     */
    public static boolean is32BitUuid(ParcelUuid parcelUuid) {
        UUID uuid = parcelUuid.getUuid();
        if (uuid.getLeastSignificantBits() != BASE_UUID.getLeastSignificantBits()) {
            return false;
        }
        if (is16BitUuid(parcelUuid)) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & 0xFFFFFFFFL) == 0x1000L);
    }

    private void addField(AdvDataType type,
                                 byte[] value) throws IllegalArgumentException {
        ByteArrayOutputStream outputStream = advertisementPacket;
        if (requireLegacyDataLength && advertisementPacket.size() + 2 + value.length > LEGACY_MAX_DATA_LENGTH) {
            if (scanResponsePacket.size() + 2 + value.length > LEGACY_MAX_DATA_LENGTH) {
                throw new IllegalArgumentException("Could not fit scan response inside legacy data packets");
            } else {
                outputStream = scanResponsePacket;
            }
        }
        outputStream.write(value.length + 1);
        outputStream.write(type.value);
        outputStream.write(value, 0, value.length);
    }

    /**
     * Construct the byte array for a given scan record. Copied and modified from Android source.
     * @param scanRecord The scan record to encode
     * @return The constructed scan record data
     * @throws UnsupportedEncodingException If the device name could not be encoded
     * @throws IllegalArgumentException If the data could not fit in legacy packets
     */
    public byte[] constructBytesFromScanRecord(@Nullable ScanRecord scanRecord) throws UnsupportedEncodingException,
            IllegalArgumentException {
        if (scanRecord == null) {
            return new byte[0];
        }

        advertisementPacket = new ByteArrayOutputStream();
        scanResponsePacket = new ByteArrayOutputStream();

        if (scanRecord.getAdvertiseFlags() != -1) {
            addField(AdvDataType.Flags, new byte[] {(byte) scanRecord.getAdvertiseFlags()});
        }

        String name = scanRecord.getDeviceName();
        if (name != null) {
            byte[] nameBytes = name.getBytes("UTF-8");
            int nameLength = nameBytes.length;
            AdvDataType type;
            if (nameLength > DEVICE_NAME_MAX) {
                nameLength = DEVICE_NAME_MAX;
                type = AdvDataType.LocalNameShort;
            } else {
                type = AdvDataType.LocalNameComplete;
            }
            addField(type, Arrays.copyOfRange(nameBytes, 0, nameLength));
        }
        SparseArray<byte[]> manuData = scanRecord.getManufacturerSpecificData();
        for (int i = 0; i < manuData.size(); i++) {
            int manufacturerId = manuData.keyAt(i);
            byte[] manufacturerData = manuData.get(manufacturerId);
            int dataLen = 2 + (manufacturerData == null ? 0 : manufacturerData.length);
            byte[] concatenated = new byte[dataLen];
            // First two bytes are manufacturer id in little-endian.
            concatenated[0] = (byte) (manufacturerId & 0xFF);
            concatenated[1] = (byte) ((manufacturerId >> 8) & 0xFF);
            if (manufacturerData != null) {
                System.arraycopy(manufacturerData, 0, concatenated, 2, manufacturerData.length);
            }
            addField(AdvDataType.ManufacturerSpecificData, concatenated);
        }
        if (scanRecord.getTxPowerLevel() != Integer.MIN_VALUE) {
            addField(AdvDataType.TxPowerLevel,
                    new byte[] {(byte) scanRecord.getTxPowerLevel()});
        }
        if (scanRecord.getServiceUuids() != null) {
            ByteArrayOutputStream serviceUuids16 = new ByteArrayOutputStream();
            ByteArrayOutputStream serviceUuids32 = new ByteArrayOutputStream();
            ByteArrayOutputStream serviceUuids128 = new ByteArrayOutputStream();
            for (ParcelUuid parcelUuid : scanRecord.getServiceUuids()) {
                byte[] uuid = uuidToBytes(parcelUuid);
                if (uuid.length == UUID_BYTES_16_BIT) {
                    serviceUuids16.write(uuid, 0, uuid.length);
                } else if (uuid.length == UUID_BYTES_32_BIT) {
                    serviceUuids32.write(uuid, 0, uuid.length);
                } else /*if (uuid.length == UUID_BYTES_128_BIT)*/ {
                    serviceUuids128.write(uuid, 0, uuid.length);
                }
            }
            if (serviceUuids16.size() != 0) {
                addField(AdvDataType.ServiceUuids16BitComplete, serviceUuids16.toByteArray());
            }
            if (serviceUuids32.size() != 0) {
                addField(AdvDataType.ServiceUuids32BitComplete, serviceUuids32.toByteArray());
            }
            if (serviceUuids128.size() != 0) {
                addField(AdvDataType.ServiceUuids128BitComplete, serviceUuids128.toByteArray());
            }
        }
        Map<ParcelUuid, byte[]> serviceData = scanRecord.getServiceData();
        if (!serviceData.isEmpty()) {
            for (ParcelUuid parcelUuid : serviceData.keySet()) {
                byte[] serviceDataBytes = serviceData.get(parcelUuid);
                byte[] uuid = uuidToBytes(parcelUuid);
                int uuidLen = uuid.length;
                int dataLen = uuidLen + (serviceDataBytes == null ? 0 : serviceDataBytes.length);
                byte[] concatenated = new byte[dataLen];
                System.arraycopy(uuid, 0, concatenated, 0, uuidLen);
                if (serviceDataBytes != null) {
                    System.arraycopy(serviceDataBytes, 0, concatenated, uuidLen, serviceDataBytes.length);
                }
                if (uuid.length == UUID_BYTES_16_BIT) {
                    addField(AdvDataType.ServiceData16Bit, concatenated);
                } else if (uuid.length == UUID_BYTES_32_BIT) {
                    addField(AdvDataType.ServiceData32Bit, concatenated);
                } else /*if (uuid.length == UUID_BYTES_128_BIT)*/ {
                    addField(AdvDataType.ServiceData128Bit, concatenated);
                }
            }
        }
        if (requireLegacyDataLength) {
            // pad adv data and scan response to 31 bytes
            if (advertisementPacket.size() < LEGACY_MAX_DATA_LENGTH) {
                byte[] padding = new byte[LEGACY_MAX_DATA_LENGTH - advertisementPacket.size()];
                advertisementPacket.write(padding, 0, padding.length);
            }
            if (scanResponsePacket.size() < LEGACY_MAX_DATA_LENGTH) {
                byte[] padding = new byte[LEGACY_MAX_DATA_LENGTH - scanResponsePacket.size()];
                scanResponsePacket.write(padding, 0, padding.length);
            }
            advertisementPacket.write(scanResponsePacket.toByteArray(), 0, LEGACY_MAX_DATA_LENGTH);
        }
        return advertisementPacket.toByteArray();
    }
}
