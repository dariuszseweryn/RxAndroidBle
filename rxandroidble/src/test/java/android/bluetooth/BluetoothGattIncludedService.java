package android.bluetooth;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.UUID;
/**
 * Used for mocks
 */
public class BluetoothGattIncludedService implements Parcelable {
    public BluetoothGattIncludedService(UUID uuid, int instanceId, int serviceType) {
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel out, int flags) {
    }
    public static final Creator<BluetoothGattIncludedService> CREATOR =
            new Creator<BluetoothGattIncludedService>() {
                public BluetoothGattIncludedService createFromParcel(Parcel in) {
                    return new BluetoothGattIncludedService(in);
                }
                public BluetoothGattIncludedService[] newArray(int size) {
                    return new BluetoothGattIncludedService[size];
                }
            };
    private BluetoothGattIncludedService(Parcel in) {
    }
    public UUID getUuid() {
        return null;
    }
    public int getInstanceId() {
        return 0;
    }
    public int getType() {
        return 0;
    }
}
