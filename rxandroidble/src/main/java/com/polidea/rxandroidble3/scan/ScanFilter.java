package com.polidea.rxandroidble3.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.internal.ScanResultInterface;
import com.polidea.rxandroidble3.internal.logger.LoggerUtil;
import com.polidea.rxandroidble3.internal.scan.ScanFilterInterface;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Criteria for filtering result from Bluetooth LE scans. A {@link ScanFilter} allows clients to
 * restrict scan results to only those that are of interest to them.
 * <p>
 * Current filtering on the following fields are supported:
 * <li>Service UUIDs which identify the bluetooth gatt services running on the device.
 * <li>Name of remote Bluetooth LE device.
 * <li>Mac address of the remote device.
 * <li>Service data which is the data associated with a service.
 * <li>Manufacturer specific data which is the data associated with a particular manufacturer.
 *
 * @see ScanResult
 * @see BluetoothLeScanner
 *
 * https://code.google.com/p/android/issues/detail?id=180675
 * https://code.google.com/p/android/issues/detail?id=181561
 * https://code.google.com/p/android/issues/detail?id=313221
 */
 public class ScanFilter implements Parcelable, ScanFilterInterface {

    @Nullable
    private final String mDeviceName;

    @Nullable
    private final String mDeviceAddress;

    @Nullable
    private final ParcelUuid mServiceUuid;
    @Nullable
    private final ParcelUuid mServiceUuidMask;

    @Nullable
    private final ParcelUuid mServiceSolicitationUuid;
    @Nullable
    private final ParcelUuid mServiceSolicitationUuidMask;

    @Nullable
    private final ParcelUuid mServiceDataUuid;
    @Nullable
    private final byte[] mServiceData;
    @Nullable
    private final byte[] mServiceDataMask;

    private final int mManufacturerId;
    @Nullable
    private final byte[] mManufacturerData;
    @Nullable
    private final byte[] mManufacturerDataMask;
    private static final ScanFilter EMPTY = new ScanFilter.Builder().build();


    ScanFilter(@Nullable String name, @Nullable String deviceAddress, @Nullable ParcelUuid uuid,
               @Nullable ParcelUuid uuidMask, @Nullable ParcelUuid solicitationUuid,
               @Nullable ParcelUuid solicitationUuidMask, @Nullable ParcelUuid serviceDataUuid,
               @Nullable byte[] serviceData, @Nullable byte[] serviceDataMask,
               int manufacturerId, @Nullable byte[] manufacturerData, @Nullable byte[] manufacturerDataMask) {
        mDeviceName = name;
        mServiceUuid = uuid;
        mServiceUuidMask = uuidMask;
        mServiceSolicitationUuid = solicitationUuid;
        mServiceSolicitationUuidMask = solicitationUuidMask;
        mDeviceAddress = deviceAddress;
        mServiceDataUuid = serviceDataUuid;
        mServiceData = serviceData;
        mServiceDataMask = serviceDataMask;
        mManufacturerId = manufacturerId;
        mManufacturerData = manufacturerData;
        mManufacturerDataMask = manufacturerDataMask;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mDeviceName == null ? 0 : 1);
        if (mDeviceName != null) {
            dest.writeString(mDeviceName);
        }
        dest.writeInt(mDeviceAddress == null ? 0 : 1);
        if (mDeviceAddress != null) {
            dest.writeString(mDeviceAddress);
        }
        dest.writeInt(mServiceUuid == null ? 0 : 1);
        if (mServiceUuid != null) {
            dest.writeParcelable(mServiceUuid, flags);
            dest.writeInt(mServiceUuidMask == null ? 0 : 1);
            if (mServiceUuidMask != null) {
                dest.writeParcelable(mServiceUuidMask, flags);
            }
        }
        dest.writeInt(mServiceSolicitationUuid == null ? 0 : 1);
        if (mServiceSolicitationUuid != null) {
            dest.writeParcelable(mServiceSolicitationUuid, flags);
            dest.writeInt(mServiceSolicitationUuidMask == null ? 0 : 1);
            if (mServiceSolicitationUuidMask != null) {
                dest.writeParcelable(mServiceSolicitationUuidMask, flags);
            }
        }
        dest.writeInt(mServiceDataUuid == null ? 0 : 1);
        if (mServiceDataUuid != null) {
            dest.writeParcelable(mServiceDataUuid, flags);
            dest.writeInt(mServiceData == null ? 0 : 1);
            if (mServiceData != null) {
                dest.writeInt(mServiceData.length);
                dest.writeByteArray(mServiceData);

                dest.writeInt(mServiceDataMask == null ? 0 : 1);
                if (mServiceDataMask != null) {
                    dest.writeInt(mServiceDataMask.length);
                    dest.writeByteArray(mServiceDataMask);
                }
            }
        }
        dest.writeInt(mManufacturerId);
        dest.writeInt(mManufacturerData == null ? 0 : 1);
        if (mManufacturerData != null) {
            dest.writeInt(mManufacturerData.length);
            dest.writeByteArray(mManufacturerData);

            dest.writeInt(mManufacturerDataMask == null ? 0 : 1);
            if (mManufacturerDataMask != null) {
                dest.writeInt(mManufacturerDataMask.length);
                dest.writeByteArray(mManufacturerDataMask);
            }
        }
    }

    /**
     * A {@link android.os.Parcelable.Creator} to create {@link ScanFilter} from parcel.
     */
    public static final Creator<ScanFilter>
            CREATOR = new Creator<ScanFilter>() {

        @Override
        public ScanFilter[] newArray(int size) {
            return new ScanFilter[size];
        }

        @Override
        public ScanFilter createFromParcel(Parcel in) {
            ScanFilter.Builder builder = new ScanFilter.Builder();
            if (in.readInt() == 1) {
                builder.setDeviceName(in.readString());
            }
            if (in.readInt() == 1) {
                builder.setDeviceAddress(in.readString());
            }
            if (in.readInt() == 1) {
                ParcelUuid uuid = in.readParcelable(ParcelUuid.class.getClassLoader());
                builder.setServiceUuid(uuid);
                if (in.readInt() == 1) {
                    ParcelUuid uuidMask = in.readParcelable(
                            ParcelUuid.class.getClassLoader());
                    builder.setServiceUuid(uuid, uuidMask);
                }
            }
            if (in.readInt() == 1) {
                ParcelUuid solicitationUuid = in.readParcelable(
                        ParcelUuid.class.getClassLoader());
                builder.setServiceSolicitationUuid(solicitationUuid);
                if (in.readInt() == 1) {
                    ParcelUuid solicitationUuidMask = in.readParcelable(
                            ParcelUuid.class.getClassLoader());
                    builder.setServiceSolicitationUuid(solicitationUuid,
                            solicitationUuidMask);
                }
            }
            if (in.readInt() == 1) {
                ParcelUuid serviceDataUuid =
                        in.readParcelable(ParcelUuid.class.getClassLoader());
                if (in.readInt() == 1) {
                    int serviceDataLength = in.readInt();
                    byte[] serviceData = new byte[serviceDataLength];
                    in.readByteArray(serviceData);
                    if (in.readInt() == 0) {
                        builder.setServiceData(serviceDataUuid, serviceData);
                    } else {
                        int serviceDataMaskLength = in.readInt();
                        byte[] serviceDataMask = new byte[serviceDataMaskLength];
                        in.readByteArray(serviceDataMask);
                        builder.setServiceData(
                                serviceDataUuid, serviceData, serviceDataMask);
                    }
                }
            }

            int manufacturerId = in.readInt();
            if (in.readInt() == 1) {
                int manufacturerDataLength = in.readInt();
                byte[] manufacturerData = new byte[manufacturerDataLength];
                in.readByteArray(manufacturerData);
                if (in.readInt() == 0) {
                    builder.setManufacturerData(manufacturerId, manufacturerData);
                } else {
                    int manufacturerDataMaskLength = in.readInt();
                    byte[] manufacturerDataMask = new byte[manufacturerDataMaskLength];
                    in.readByteArray(manufacturerDataMask);
                    builder.setManufacturerData(manufacturerId, manufacturerData,
                            manufacturerDataMask);
                }
            }

            return builder.build();
        }
    };

    /**
     * Returns the filter set the device name field of Bluetooth advertisement data.
     */
    @Nullable
    public String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Returns the filter set on the service uuid.
     */
    @Nullable
    public ParcelUuid getServiceUuid() {
        return mServiceUuid;
    }

    @Nullable
    public ParcelUuid getServiceUuidMask() {
        return mServiceUuidMask;
    }

    /**
     * Returns the filter set on the service Solicitation uuid.
     */
    @Nullable
    public ParcelUuid getServiceSolicitationUuid() {
        return mServiceSolicitationUuid;
    }
    /**
     * Returns the filter set on the service Solicitation uuid mask.
     */
    @Nullable
    public ParcelUuid getServiceSolicitationUuidMask() {
        return mServiceSolicitationUuidMask;
    }

    @Nullable
    public String getDeviceAddress() {
        return mDeviceAddress;
    }

    @Nullable
    public byte[] getServiceData() {
        return mServiceData;
    }

    @Nullable
    public byte[] getServiceDataMask() {
        return mServiceDataMask;
    }

    @Nullable
    public ParcelUuid getServiceDataUuid() {
        return mServiceDataUuid;
    }

    /**
     * Returns the manufacturer id. -1 if the manufacturer filter is not set.
     */
    public int getManufacturerId() {
        return mManufacturerId;
    }

    @Nullable
    public byte[] getManufacturerData() {
        return mManufacturerData;
    }

    @Nullable
    public byte[] getManufacturerDataMask() {
        return mManufacturerDataMask;
    }

    /**
     * Check if the scan filter matches a {@code scanResult}. A scan result is considered as a match
     * if it matches all the field filters.
     */
    public boolean matches(ScanResultInterface scanResult) {
        if (scanResult == null) {
            return false;
        }
        String address = scanResult.getAddress();
        // Device match.
        if (mDeviceAddress != null && !mDeviceAddress.equals(address)) {
            return false;
        }

        ScanRecord scanRecord = scanResult.getScanRecord();

        // Local name match.
        if (mDeviceName != null) {
            if (!mDeviceName.equals(scanResult.getDeviceName())
                    && (scanRecord == null || !mDeviceName.equals(scanRecord.getDeviceName()))) {
                return false;
            }
        }

        // if scan record is null we cannot continue. For filter to pass, remaining filter properties must be empty
        if (scanRecord == null) {
            return mServiceUuid == null && mManufacturerData == null && mServiceData == null;
        }

        // UUID match.
        if (mServiceUuid != null && !matchesServiceUuids(mServiceUuid, mServiceUuidMask,
                scanRecord.getServiceUuids())) {
            return false;
        }

        // solicitation UUID match.
        if (mServiceSolicitationUuid != null && !matchesServiceSolicitationUuids(
                mServiceSolicitationUuid, mServiceSolicitationUuidMask,
                scanRecord.getServiceSolicitationUuids())) {
            return false;
        }

        // Service data match
        if (mServiceDataUuid != null) {
            if (!matchesPartialData(mServiceData, mServiceDataMask,
                    scanRecord.getServiceData(mServiceDataUuid))) {
                return false;
            }
        }

        // Manufacturer data match.
        if (mManufacturerId >= 0) {
            if (!matchesPartialData(mManufacturerData, mManufacturerDataMask,
                    scanRecord.getManufacturerSpecificData(mManufacturerId))) {
                return false;
            }
        }
        // All filters match.
        return true;
    }

    // Check if the uuid pattern is contained in a list of parcel uuids.
    private static boolean matchesServiceUuids(ParcelUuid uuid, ParcelUuid parcelUuidMask,
                                               List<ParcelUuid> uuids) {
        if (uuid == null) {
            return true;
        }
        if (uuids == null) {
            return false;
        }

        for (ParcelUuid parcelUuid : uuids) {
            UUID uuidMask = parcelUuidMask == null ? null : parcelUuidMask.getUuid();
            if (matchesServiceUuid(uuid.getUuid(), uuidMask, parcelUuid.getUuid())) {
                return true;
            }
        }
        return false;
    }

    // Check if the uuid pattern matches the particular service uuid.
    private static boolean matchesServiceUuid(UUID uuid, UUID mask, UUID data) {
        if (mask == null) {
            return uuid.equals(data);
        }
        if ((uuid.getLeastSignificantBits() & mask.getLeastSignificantBits())
                != (data.getLeastSignificantBits() & mask.getLeastSignificantBits())) {
            return false;
        }
        return ((uuid.getMostSignificantBits() & mask.getMostSignificantBits())
                == (data.getMostSignificantBits() & mask.getMostSignificantBits()));
    }

    /**
     * Check if the solicitation uuid pattern is contained in a list of parcel uuids.
     *
     */
    private static boolean matchesServiceSolicitationUuids(ParcelUuid solicitationUuid,
                                                           ParcelUuid parcelSolicitationUuidMask, List<ParcelUuid> solicitationUuids) {
        if (solicitationUuid == null) {
            return true;
        }
        if (solicitationUuids == null) {
            return false;
        }
        for (ParcelUuid parcelSolicitationUuid : solicitationUuids) {
            UUID solicitationUuidMask = parcelSolicitationUuidMask == null
                    ? null : parcelSolicitationUuidMask.getUuid();
            if (matchesServiceUuid(solicitationUuid.getUuid(), solicitationUuidMask,
                    parcelSolicitationUuid.getUuid())) {
                return true;
            }
        }
        return false;
    }

    // Check whether the data pattern matches the parsed data.
    private static boolean matchesPartialData(byte[] data, byte[] dataMask, byte[] parsedData) {
        if (parsedData == null || parsedData.length < data.length) {
            return false;
        }
        if (dataMask == null) {
            for (int i = 0; i < data.length; ++i) {
                if (parsedData[i] != data[i]) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < data.length; ++i) {
            if ((dataMask[i] & parsedData[i]) != (dataMask[i] & data[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "BluetoothLeScanFilter [mDeviceName=" + mDeviceName
                + ", " + LoggerUtil.commonMacMessage(mDeviceAddress)
                + ", mUuid=" + (mServiceUuid == null ? null : LoggerUtil.getUuidToLog(mServiceUuid.getUuid()))
                + ", mUuidMask=" + (mServiceUuidMask == null ? null : LoggerUtil.getUuidToLog(mServiceUuidMask.getUuid()))
                + ", mSolicitedUuid=" + (mServiceSolicitationUuid == null
                    ? null
                    : LoggerUtil.getUuidToLog(mServiceSolicitationUuid.getUuid()))
                + ", mSolicitedUuidMask=" + (mServiceSolicitationUuidMask == null
                    ? null
                    : LoggerUtil.getUuidToLog(mServiceSolicitationUuidMask.getUuid()))
                + ", mServiceDataUuid=" + (mServiceDataUuid == null ? null : LoggerUtil.getUuidToLog(mServiceDataUuid.getUuid()))
                + ", mServiceData=" + Arrays.toString(mServiceData)
                + ", mServiceDataMask=" + Arrays.toString(mServiceDataMask)
                + ", mManufacturerId=" + mManufacturerId
                + ", mManufacturerData=" + Arrays.toString(mManufacturerData)
                + ", mManufacturerDataMask=" + Arrays.toString(mManufacturerDataMask) + "]";
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                mDeviceName,
                mDeviceAddress,
                mManufacturerId,
                Arrays.hashCode(mManufacturerData),
                Arrays.hashCode(mManufacturerDataMask),
                mServiceDataUuid,
                Arrays.hashCode(mServiceData),
                Arrays.hashCode(mServiceDataMask),
                mServiceUuid,
                mServiceUuidMask,
                mServiceSolicitationUuid,
                mServiceSolicitationUuidMask
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScanFilter other = (ScanFilter) obj;
        return equals(mDeviceName, other.mDeviceName)
                && equals(mDeviceAddress, other.mDeviceAddress)
                && mManufacturerId == other.mManufacturerId
                && deepEquals(mManufacturerData, other.mManufacturerData)
                && deepEquals(mManufacturerDataMask, other.mManufacturerDataMask)
                && equals(mServiceDataUuid, other.mServiceDataUuid)
                && deepEquals(mServiceData, other.mServiceData)
                && deepEquals(mServiceDataMask, other.mServiceDataMask)
                && equals(mServiceUuid, other.mServiceUuid)
                && equals(mServiceUuidMask, other.mServiceUuidMask)
                && equals(mServiceSolicitationUuid, other.mServiceSolicitationUuid)
                && equals(mServiceSolicitationUuidMask, other.mServiceSolicitationUuidMask);
    }

    /**
     * Copied Object.equals()
     */
    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    /**
     * Copied Object.deepEquals()
     */
    private static boolean deepEquals(byte[] a, byte[] b) {
        return a == b || !(a == null || b == null) && Arrays.equals(a, b);
    }

    /**
     * Checks if the scanfilter is empty
     * @hide
     */
    public boolean isAllFieldsEmpty() {
        return this.equals(EMPTY);
    }

    /**
     * A function that returns an empty filter
     * @return an empty filter
     */
    public static ScanFilter empty() {
        return new ScanFilter.Builder().build();
    }

    /**
     * Builder class for {@link ScanFilter}.
     */
    public static final class Builder {

        private String mDeviceName;
        private String mDeviceAddress;

        private ParcelUuid mServiceUuid;
        private ParcelUuid mServiceUuidMask;

        private ParcelUuid mServiceSolicitationUuid;
        private ParcelUuid mServiceSolicitationUuidMask;

        private ParcelUuid mServiceDataUuid;
        private byte[] mServiceData;
        private byte[] mServiceDataMask;

        private int mManufacturerId = -1;
        private byte[] mManufacturerData;
        private byte[] mManufacturerDataMask;

        /**
         * Set filter on device name.
         */
        public ScanFilter.Builder setDeviceName(String deviceName) {
            mDeviceName = deviceName;
            return this;
        }

        /**
         * Set filter on device address.
         *
         * @param deviceAddress The device Bluetooth address for the filter. It needs to be in the
         *            format of "01:02:03:AB:CD:EF". The device address can be validated using
         *            {@link BluetoothAdapter#checkBluetoothAddress}.
         * @throws IllegalArgumentException If the {@code deviceAddress} is invalid.
         */
        public ScanFilter.Builder setDeviceAddress(String deviceAddress) {
            if (deviceAddress != null && !BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
                throw new IllegalArgumentException("invalid device address " + deviceAddress);
            }
            mDeviceAddress = deviceAddress;
            return this;
        }

        /**
         * Set filter on service uuid.
         */
        public ScanFilter.Builder setServiceUuid(ParcelUuid serviceUuid) {
            mServiceUuid = serviceUuid;
            mServiceUuidMask = null; // clear uuid mask
            return this;
        }

        /**
         * Set filter on partial service uuid. The {@code uuidMask} is the bit mask for the
         * {@code serviceUuid}. Set any bit in the mask to 1 to indicate a match is needed for the
         * bit in {@code serviceUuid}, and 0 to ignore that bit.
         *
         * @throws IllegalArgumentException If {@code serviceUuid} is {@code null} but
         *             {@code uuidMask} is not {@code null}.
         */
        public ScanFilter.Builder setServiceUuid(ParcelUuid serviceUuid, ParcelUuid uuidMask) {
            if (mServiceUuidMask != null && mServiceUuid == null) {
                throw new IllegalArgumentException("uuid is null while uuidMask is not null!");
            }
            mServiceUuid = serviceUuid;
            mServiceUuidMask = uuidMask;
            return this;
        }

        /**
         * Set filter on service solicitation uuid.
         */
        public ScanFilter.Builder setServiceSolicitationUuid(ParcelUuid solicitedServiceUuid) {
            mServiceSolicitationUuid = solicitedServiceUuid;
            mServiceSolicitationUuidMask = null; // clear uuid mask
            return this;
        }

        /**
         * Set filter on partial service Solicitation uuid. The {@code SolicitationUuidMask} is the
         * bit mask for the {@code serviceSolicitationUuid}. Set any bit in the mask to 1 to
         * indicate a match is needed for the bit in {@code serviceSolicitationUuid}, and 0 to
         * ignore that bit.
         *
         * @param serviceSolicitationUuid can only be null if solicitationUuidMask is null.
         * @param solicitationUuidMask can be null or a mask with no restriction.
         *
         * @throws IllegalArgumentException If {@code serviceSolicitationUuid} is {@code null} but
         *             {@code serviceSolicitationUuidMask} is not {@code null}.
         */
        public Builder setServiceSolicitationUuid(
                @Nullable ParcelUuid serviceSolicitationUuid,
                @Nullable ParcelUuid solicitationUuidMask) {
            if (solicitationUuidMask != null && serviceSolicitationUuid == null) {
                throw new IllegalArgumentException(
                        "SolicitationUuid is null while SolicitationUuidMask is not null!");
            }
            mServiceSolicitationUuid = serviceSolicitationUuid;
            mServiceSolicitationUuidMask = solicitationUuidMask;
            return this;
        }

        /**
         * Set filtering on service data.
         *
         * @throws IllegalArgumentException If {@code serviceDataUuid} is null.
         */
        public ScanFilter.Builder setServiceData(ParcelUuid serviceDataUuid, byte[] serviceData) {
            if (serviceDataUuid == null) {
                throw new IllegalArgumentException("serviceDataUuid is null");
            }
            mServiceDataUuid = serviceDataUuid;
            mServiceData = serviceData;
            mServiceDataMask = null; // clear service data mask
            return this;
        }

        /**
         * Set partial filter on service data. For any bit in the mask, set it to 1 if it needs to
         * match the one in service data, otherwise set it to 0 to ignore that bit.
         * <p>
         * The {@code serviceDataMask} must have the same length of the {@code serviceData}.
         *
         * @throws IllegalArgumentException If {@code serviceDataUuid} is null or
         *             {@code serviceDataMask} is {@code null} while {@code serviceData} is not or
         *             {@code serviceDataMask} and {@code serviceData} has different length.
         */
        public ScanFilter.Builder setServiceData(ParcelUuid serviceDataUuid,
                                                                      byte[] serviceData, byte[] serviceDataMask) {
            if (serviceDataUuid == null) {
                throw new IllegalArgumentException("serviceDataUuid is null");
            }
            if (mServiceDataMask != null) {
                if (mServiceData == null) {
                    throw new IllegalArgumentException(
                            "serviceData is null while serviceDataMask is not null");
                }
                // Since the mServiceDataMask is a bit mask for mServiceData, the lengths of the two
                // byte array need to be the same.
                if (mServiceData.length != mServiceDataMask.length) {
                    throw new IllegalArgumentException(
                            "size mismatch for service data and service data mask");
                }
            }
            mServiceDataUuid = serviceDataUuid;
            mServiceData = serviceData;
            mServiceDataMask = serviceDataMask;
            return this;
        }

        /**
         * Set filter on on manufacturerData. A negative manufacturerId is considered as invalid id.
         * <p>
         * Note the first two bytes of the {@code manufacturerData} is the manufacturerId.
         *
         * @throws IllegalArgumentException If the {@code manufacturerId} is invalid.
         */
        public ScanFilter.Builder setManufacturerData(int manufacturerId, byte[] manufacturerData) {
            if (manufacturerData != null && manufacturerId < 0) {
                throw new IllegalArgumentException("invalid manufacture id");
            }
            mManufacturerId = manufacturerId;
            mManufacturerData = manufacturerData;
            mManufacturerDataMask = null; // clear manufacturer data mask
            return this;
        }

        /**
         * Set filter on partial manufacture data. For any bit in the mask, set it the 1 if it needs
         * to match the one in manufacturer data, otherwise set it to 0.
         * <p>
         * The {@code manufacturerDataMask} must have the same length of {@code manufacturerData}.
         *
         * @throws IllegalArgumentException If the {@code manufacturerId} is invalid, or
         *             {@code manufacturerData} is null while {@code manufacturerDataMask} is not,
         *             or {@code manufacturerData} and {@code manufacturerDataMask} have different
         *             length.
         */
        public ScanFilter.Builder setManufacturerData(int manufacturerId, byte[] manufacturerData,
                                                                           byte[] manufacturerDataMask) {
            if (manufacturerData != null && manufacturerId < 0) {
                throw new IllegalArgumentException("invalid manufacture id");
            }
            if (mManufacturerDataMask != null) {
                if (mManufacturerData == null) {
                    throw new IllegalArgumentException(
                            "manufacturerData is null while manufacturerDataMask is not null");
                }
                // Since the mManufacturerDataMask is a bit mask for mManufacturerData, the lengths
                // of the two byte array need to be the same.
                if (mManufacturerData.length != mManufacturerDataMask.length) {
                    throw new IllegalArgumentException(
                            "size mismatch for manufacturerData and manufacturerDataMask");
                }
            }
            mManufacturerId = manufacturerId;
            mManufacturerData = manufacturerData;
            mManufacturerDataMask = manufacturerDataMask;
            return this;
        }

        /**
         * Build {@link ScanFilter}.
         *
         * @throws IllegalArgumentException If the filter cannot be built.
         */
        public ScanFilter build() {
            return new ScanFilter(mDeviceName, mDeviceAddress,
                    mServiceUuid, mServiceUuidMask,
                    mServiceSolicitationUuid, mServiceSolicitationUuidMask,
                    mServiceDataUuid, mServiceData, mServiceDataMask,
                    mManufacturerId, mManufacturerData, mManufacturerDataMask);
        }
    }
}