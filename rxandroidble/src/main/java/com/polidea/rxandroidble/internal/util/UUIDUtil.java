package com.polidea.rxandroidble.internal.util;

import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.SparseArray;

import com.polidea.rxandroidble.internal.scan.ScanRecordImplCompat;
import com.polidea.rxandroidble.scan.ScanRecord;
import com.polidea.rxandroidble.internal.RxBleLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Internal helper class for extracting list of Service UUIDs from Advertisement data
 *
 * @link http://stackoverflow.com/questions/31668791/how-can-i-read-uuids-from-advertisement-data-ios-overflow-area-in-android
 * @deprecated use {@link com.polidea.rxandroidble.helpers.AdvertisedServiceUUIDExtractor} instead. This class may change in later releases.
 */
@Deprecated
public class UUIDUtil {

    // The following data type values are assigned by Bluetooth SIG.
    // For more details refer to Bluetooth 4.1 specification, Volume 3, Part C, Section 18.
    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA = 0x16;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");
    /** Length of bytes for 16 bit UUID */
    public static final int UUID_BYTES_16_BIT = 2;
    /** Length of bytes for 32 bit UUID */
    public static final int UUID_BYTES_32_BIT = 4;
    /** Length of bytes for 128 bit UUID */
    public static final int UUID_BYTES_128_BIT = 16;

    private static final String UUID_BASE_FORMAT = "%08x-0000-1000-8000-00805f9b34fb";

    @Inject
    public UUIDUtil() {
    }

    public List<UUID> extractUUIDs(byte[] scanResult) {
        List<UUID> uuids = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(scanResult).order(ByteOrder.LITTLE_ENDIAN);

        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL: // Partial list of 16-bit UUIDs
                case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(UUID_BASE_FORMAT, buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL: // Partial list of 32-bit UUIDs
                case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE: // Complete list of 32-bit UUIDs
                    while (length >= 4) {
                        uuids.add(UUID.fromString(String.format(UUID_BASE_FORMAT, buffer.getInt())));
                        length -= 4;
                    }

                case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL: // Partial list of 128-bit UUIDs
                case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }

    @NonNull
    public Set<UUID> toDistinctSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }

    /**
     * Method is copied from post lollipop {@link android.bluetooth.le.ScanRecord}
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ScanRecord parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<>();
        Map<ParcelUuid, byte[]> serviceData = new HashMap<>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength, UUID_BYTES_16_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA:
                        // The first two bytes of the service data are service data UUID in little
                        // endian. The rest bytes are service data.
                        int serviceUuidLength = UUID_BYTES_16_BIT;
                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = parseUuidFrom(serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8)
                                + (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }
            return new ScanRecordImplCompat(serviceUuids, manufacturerData, serviceData,
                    advertiseFlag, txPowerLevel, localName, scanRecord);
        } catch (Exception e) {
            RxBleLog.e(e, "unable to parse scan record: " + Arrays.toString(scanRecord));
            // As the record is invalid, ignore all the parsed results for this packet
            // and return an empty record with raw scanRecord bytes in results
            return new ScanRecordImplCompat(null, null, null, -1, Integer.MIN_VALUE, null, scanRecord);
        }
    }

    private static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT
                && length != UUID_BYTES_128_BIT) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }
        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }
        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }

    // Parse service UUIDs.
    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                        int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }
}
