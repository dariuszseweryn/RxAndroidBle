package android.bluetooth;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/**
 * Used for mocks and constants in rxandroidble, but instances created in mockrxandroidble, so the methods must be implemented
 */
public class BluetoothGattCharacteristic implements Parcelable {
    public static final int PROPERTY_BROADCAST = 0x01;
    public static final int PROPERTY_READ = 0x02;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 0x04;
    public static final int PROPERTY_WRITE = 0x08;
    public static final int PROPERTY_NOTIFY = 0x10;
    public static final int PROPERTY_INDICATE = 0x20;
    public static final int PROPERTY_SIGNED_WRITE = 0x40;
    public static final int PROPERTY_EXTENDED_PROPS = 0x80;
    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;
    public static final int WRITE_TYPE_DEFAULT = 0x02;
    public static final int WRITE_TYPE_NO_RESPONSE = 0x01;
    public static final int WRITE_TYPE_SIGNED = 0x04;
    public static final int FORMAT_UINT8 = 0x11;
    public static final int FORMAT_UINT16 = 0x12;
    public static final int FORMAT_UINT32 = 0x14;
    public static final int FORMAT_SINT8 = 0x21;
    public static final int FORMAT_SINT16 = 0x22;
    public static final int FORMAT_SINT32 = 0x24;
    public static final int FORMAT_SFLOAT = 0x32;
    public static final int FORMAT_FLOAT = 0x34;
    protected UUID mUuid;
    protected int mInstance;
    protected int mProperties;
    protected int mPermissions;
    protected int mKeySize = 16;
    protected int mWriteType;
    protected BluetoothGattService mService;
    protected byte[] mValue;
    protected List<BluetoothGattDescriptor> mDescriptors;
    public BluetoothGattCharacteristic(UUID uuid, int properties, int permissions) {
        initCharacteristic(null, uuid, 0, properties, permissions);
    }
    BluetoothGattCharacteristic(BluetoothGattService service,
                                            UUID uuid, int instanceId,
                                            int properties, int permissions) {
        initCharacteristic(service, uuid, instanceId, properties, permissions);
    }
    public BluetoothGattCharacteristic(UUID uuid, int instanceId,
                                       int properties, int permissions) {
        initCharacteristic(null, uuid, instanceId, properties, permissions);
    }
    private void initCharacteristic(BluetoothGattService service,
                                    UUID uuid, int instanceId,
                                    int properties, int permissions) {
        mUuid = uuid;
        mInstance = instanceId;
        mProperties = properties;
        mPermissions = permissions;
        mService = service;
        mValue = null;
        mDescriptors = new ArrayList<BluetoothGattDescriptor>();
        if ((mProperties & PROPERTY_WRITE_NO_RESPONSE) != 0) {
            mWriteType = WRITE_TYPE_NO_RESPONSE;
        } else {
            mWriteType = WRITE_TYPE_DEFAULT;
        }
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {

    }
    public static final Creator<BluetoothGattCharacteristic> CREATOR =
            new Creator<BluetoothGattCharacteristic>() {
                public BluetoothGattCharacteristic createFromParcel(Parcel in) {
                    return new BluetoothGattCharacteristic(in);
                }
                public BluetoothGattCharacteristic[] newArray(int size) {
                    return new BluetoothGattCharacteristic[size];
                }
            };
    private BluetoothGattCharacteristic(Parcel in) {

    }
    public int getKeySize() {
        return mKeySize;
    }
    public boolean addDescriptor(BluetoothGattDescriptor descriptor) {
        mDescriptors.add(descriptor);
        descriptor.setCharacteristic(this);
        return true;
    }
     BluetoothGattDescriptor getDescriptor(UUID uuid, int instanceId) {
        for (BluetoothGattDescriptor descriptor : mDescriptors) {
            if (descriptor.getUuid().equals(uuid)
                    && descriptor.getInstanceId() == instanceId) {
                return descriptor;
            }
        }
        return null;
    }
    public BluetoothGattService getService() {
        return mService;
    }
    void setService(BluetoothGattService service) {
        mService = service;
    }
    public UUID getUuid() {
        return mUuid;
    }
    public int getInstanceId() {
        return mInstance;
    }
    public void setInstanceId(int instanceId) {
        mInstance = instanceId;
    }
    public int getProperties() {
        return mProperties;
    }
    public int getPermissions() {
        return mPermissions;
    }
    public int getWriteType() {
        return mWriteType;
    }
    public void setWriteType(int writeType) {
        mWriteType = writeType;
    }
    public void setKeySize(int keySize) {
        mKeySize = keySize;
    }
    public List<BluetoothGattDescriptor> getDescriptors() {
        return mDescriptors;
    }
    public BluetoothGattDescriptor getDescriptor(UUID uuid) {
        for (BluetoothGattDescriptor descriptor : mDescriptors) {
            if (descriptor.getUuid().equals(uuid)) {
                return descriptor;
            }
        }
        return null;
    }
    public byte[] getValue() {
        return mValue;
    }
    public Integer getIntValue(int formatType, int offset) {
        if ((offset + getTypeLen(formatType)) > mValue.length) return null;
        switch (formatType) {
            case FORMAT_UINT8:
                return unsignedByteToInt(mValue[offset]);
            case FORMAT_UINT16:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1]);
            case FORMAT_UINT32:
                return unsignedBytesToInt(mValue[offset], mValue[offset + 1],
                        mValue[offset + 2], mValue[offset + 3]);
            case FORMAT_SINT8:
                return unsignedToSigned(unsignedByteToInt(mValue[offset]), 8);
            case FORMAT_SINT16:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset],
                        mValue[offset + 1]), 16);
            case FORMAT_SINT32:
                return unsignedToSigned(unsignedBytesToInt(mValue[offset],
                        mValue[offset + 1], mValue[offset + 2], mValue[offset + 3]), 32);
        }
        return null;
    }
    public Float getFloatValue(int formatType, int offset) {
        if ((offset + getTypeLen(formatType)) > mValue.length) return null;
        switch (formatType) {
            case FORMAT_SFLOAT:
                return bytesToFloat(mValue[offset], mValue[offset + 1]);
            case FORMAT_FLOAT:
                return bytesToFloat(mValue[offset], mValue[offset + 1],
                        mValue[offset + 2], mValue[offset + 3]);
        }
        return null;
    }
    public String getStringValue(int offset) {
        if (mValue == null || offset > mValue.length) return null;
        byte[] strBytes = new byte[mValue.length - offset];
        for (int i = 0; i != (mValue.length - offset); ++i) strBytes[i] = mValue[offset + i];
        return new String(strBytes);
    }
    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }
    public boolean setValue(int value, int formatType, int offset) {
        int len = offset + getTypeLen(formatType);
        if (mValue == null) mValue = new byte[len];
        if (len > mValue.length) return false;
        switch (formatType) {
            case FORMAT_SINT8:
                value = intToSignedBits(value, 8);
                // Fall-through intended
            case FORMAT_UINT8:
                mValue[offset] = (byte) (value & 0xFF);
                break;
            case FORMAT_SINT16:
                value = intToSignedBits(value, 16);
                // Fall-through intended
            case FORMAT_UINT16:
                mValue[offset++] = (byte) (value & 0xFF);
                mValue[offset] = (byte) ((value >> 8) & 0xFF);
                break;
            case FORMAT_SINT32:
                value = intToSignedBits(value, 32);
                // Fall-through intended
            case FORMAT_UINT32:
                mValue[offset++] = (byte) (value & 0xFF);
                mValue[offset++] = (byte) ((value >> 8) & 0xFF);
                mValue[offset++] = (byte) ((value >> 16) & 0xFF);
                mValue[offset] = (byte) ((value >> 24) & 0xFF);
                break;
            default:
                return false;
        }
        return true;
    }
    public boolean setValue(int mantissa, int exponent, int formatType, int offset) {
        int len = offset + getTypeLen(formatType);
        if (mValue == null) mValue = new byte[len];
        if (len > mValue.length) return false;
        switch (formatType) {
            case FORMAT_SFLOAT:
                mantissa = intToSignedBits(mantissa, 12);
                exponent = intToSignedBits(exponent, 4);
                mValue[offset++] = (byte) (mantissa & 0xFF);
                mValue[offset] = (byte) ((mantissa >> 8) & 0x0F);
                mValue[offset] += (byte) ((exponent & 0x0F) << 4);
                break;
            case FORMAT_FLOAT:
                mantissa = intToSignedBits(mantissa, 24);
                exponent = intToSignedBits(exponent, 8);
                mValue[offset++] = (byte) (mantissa & 0xFF);
                mValue[offset++] = (byte) ((mantissa >> 8) & 0xFF);
                mValue[offset++] = (byte) ((mantissa >> 16) & 0xFF);
                mValue[offset] += (byte) (exponent & 0xFF);
                break;
            default:
                return false;
        }
        return true;
    }
    public boolean setValue(String value) {
        mValue = value.getBytes();
        return true;
    }
    private int getTypeLen(int formatType) {
        return formatType & 0xF;
    }
    private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }
    private int unsignedBytesToInt(byte b0, byte b1) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8));
    }
    private int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0) + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16) + (unsignedByteToInt(b3) << 24);
    }
    private float bytesToFloat(byte b0, byte b1) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + ((unsignedByteToInt(b1) & 0x0F) << 8), 12);
        int exponent = unsignedToSigned(unsignedByteToInt(b1) >> 4, 4);
        return (float) (mantissa * Math.pow(10, exponent));
    }
    private float bytesToFloat(byte b0, byte b1, byte b2, byte b3) {
        int mantissa = unsignedToSigned(unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8)
                + (unsignedByteToInt(b2) << 16), 24);
        return (float) (mantissa * Math.pow(10, b3));
    }
    private int unsignedToSigned(int unsigned, int size) {
        if ((unsigned & (1 << size - 1)) != 0) {
            unsigned = -1 * ((1 << size - 1) - (unsigned & ((1 << size - 1) - 1)));
        }
        return unsigned;
    }
    private int intToSignedBits(int i, int size) {
        if (i < 0) {
            i = (1 << size - 1) + (i & ((1 << size - 1) - 1));
        }
        return i;
    }
}
