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
public class BluetoothGattService implements Parcelable {
    public static final int SERVICE_TYPE_PRIMARY = 0;
    public static final int SERVICE_TYPE_SECONDARY = 1;
    protected BluetoothDevice mDevice;
    protected UUID mUuid;
    protected int mInstanceId;
    protected int mHandles = 0;
    protected int mServiceType;
    protected List<BluetoothGattCharacteristic> mCharacteristics;
    protected List<BluetoothGattService> mIncludedServices;
    private boolean mAdvertisePreferred;
    public BluetoothGattService(UUID uuid, int serviceType) {
        mDevice = null;
        mUuid = uuid;
        mInstanceId = 0;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        mIncludedServices = new ArrayList<BluetoothGattService>();
    }
    BluetoothGattService(BluetoothDevice device, UUID uuid,
                                     int instanceId, int serviceType) {
        mDevice = device;
        mUuid = uuid;
        mInstanceId = instanceId;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        mIncludedServices = new ArrayList<BluetoothGattService>();
    }
    public BluetoothGattService(UUID uuid, int instanceId, int serviceType) {
        mDevice = null;
        mUuid = uuid;
        mInstanceId = instanceId;
        mServiceType = serviceType;
        mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        mIncludedServices = new ArrayList<BluetoothGattService>();
    }
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {
    }
    public static final Creator<BluetoothGattService> CREATOR =
            new Creator<BluetoothGattService>() {
                public BluetoothGattService createFromParcel(Parcel in) {
                    return new BluetoothGattService(in);
                }
                public BluetoothGattService[] newArray(int size) {
                    return new BluetoothGattService[size];
                }
            };
    private BluetoothGattService(Parcel in) {
    }
    BluetoothDevice getDevice() {
        return mDevice;
    }
    void setDevice(BluetoothDevice device) {
        mDevice = device;
    }
    public boolean addService(BluetoothGattService service) {
        mIncludedServices.add(service);
        return true;
    }
    public boolean addCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristics.add(characteristic);
        characteristic.setService(this);
        return true;
    }
    BluetoothGattCharacteristic getCharacteristic(UUID uuid, int instanceId) {
        for (BluetoothGattCharacteristic characteristic : mCharacteristics) {
            if (uuid.equals(characteristic.getUuid())
                    && characteristic.getInstanceId() == instanceId) {
                return characteristic;
            }
        }
        return null;
    }
    public void setInstanceId(int instanceId) {
        mInstanceId = instanceId;
    }
    int getHandles() {
        return mHandles;
    }
    public void setHandles(int handles) {
        mHandles = handles;
    }
    public void addIncludedService(BluetoothGattService includedService) {
        mIncludedServices.add(includedService);
    }
    public UUID getUuid() {
        return mUuid;
    }
    public int getInstanceId() {
        return mInstanceId;
    }
    public int getType() {
        return mServiceType;
    }
    public List<BluetoothGattService> getIncludedServices() {
        return mIncludedServices;
    }
    public List<BluetoothGattCharacteristic> getCharacteristics() {
        return mCharacteristics;
    }
    public BluetoothGattCharacteristic getCharacteristic(UUID uuid) {
        for (BluetoothGattCharacteristic characteristic : mCharacteristics) {
            if (uuid.equals(characteristic.getUuid())) {
                return characteristic;
            }
        }
        return null;
    }
    public boolean isAdvertisePreferred() {
        return mAdvertisePreferred;
    }
    public void setAdvertisePreferred(boolean advertisePreferred) {
        mAdvertisePreferred = advertisePreferred;
    }
}
