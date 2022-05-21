package android.bluetooth;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.UUID;
/**
 * Used for mocks and constants in rxandroidble, but instances created in mockrxandroidble, so the methods must be implemented
 */
public class BluetoothGattDescriptor implements Parcelable {
    public static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};
    public static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};
    public static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};
    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    public static final int PERMISSION_WRITE_SIGNED = 0x80;
    public static final int PERMISSION_WRITE_SIGNED_MITM = 0x100;
    protected UUID mUuid;
    protected int mInstance;
    protected int mPermissions;
    protected BluetoothGattCharacteristic mCharacteristic;
    protected byte[] mValue;
    public BluetoothGattDescriptor(UUID uuid, int permissions) {
        initDescriptor(null, uuid, 0, permissions);
    }
    BluetoothGattDescriptor(BluetoothGattCharacteristic characteristic, UUID uuid,
                                        int instance, int permissions) {
        initDescriptor(characteristic, uuid, instance, permissions);
    }
    public BluetoothGattDescriptor(UUID uuid, int instance, int permissions) {
        initDescriptor(null, uuid, instance, permissions);
    }
    private void initDescriptor(BluetoothGattCharacteristic characteristic, UUID uuid,
                                int instance, int permissions) {
        mCharacteristic = characteristic;
        mUuid = uuid;
        mInstance = instance;
        mPermissions = permissions;
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(new ParcelUuid(mUuid), 0);
        out.writeInt(mInstance);
        out.writeInt(mPermissions);
    }
    public static final Creator<BluetoothGattDescriptor> CREATOR =
            new Creator<BluetoothGattDescriptor>() {
                public BluetoothGattDescriptor createFromParcel(Parcel in) {
                    return new BluetoothGattDescriptor(in);
                }
                public BluetoothGattDescriptor[] newArray(int size) {
                    return new BluetoothGattDescriptor[size];
                }
            };
    private BluetoothGattDescriptor(Parcel in) {
        mUuid = ((ParcelUuid) in.readParcelable(null)).getUuid();
        mInstance = in.readInt();
        mPermissions = in.readInt();
    }
    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }
    void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
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
    public int getPermissions() {
        return mPermissions;
    }
    public byte[] getValue() {
        return mValue;
    }
    public boolean setValue(byte[] value) {
        mValue = value;
        return true;
    }
}
