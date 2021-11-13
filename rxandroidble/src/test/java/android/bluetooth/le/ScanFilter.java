package android.bluetooth.le;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothDevice.AddressType;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import java.util.List;
public class ScanFilter implements Parcelable {
    public static final ScanFilter EMPTY = new Builder().build();
    private ScanFilter(String name, String deviceAddress, ParcelUuid uuid,
            ParcelUuid uuidMask, ParcelUuid solicitationUuid,
            ParcelUuid solicitationUuidMask, ParcelUuid serviceDataUuid,
            byte[] serviceData, byte[] serviceDataMask,
            int manufacturerId, byte[] manufacturerData, byte[] manufacturerDataMask,
            @AddressType int addressType, byte[] irk) {
    }
    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel dest, int flags) {
        
    }
    public ScanFilter[] newArray(int size) {
        return new ScanFilter[size];
    }
    public ScanFilter createFromParcel(Parcel in) {
        return null;
    }
    public String getDeviceName() {
        return null;
    }
    public ParcelUuid getServiceUuid() {
        return null;
    }
    public ParcelUuid getServiceUuidMask() {
        return null;
    }
    public ParcelUuid getServiceSolicitationUuid() {
        return null;
    }
    public ParcelUuid getServiceSolicitationUuidMask() {
        return null;
    }
    public String getDeviceAddress() {
        return null;
    }
    public @AddressType int getAddressType() {
        return 0;
    }
    public byte[] getIrk() {
        return null;
    }
    public byte[] getServiceData() {
        return null;
    }
    public byte[] getServiceDataMask() {
        return null;
    }
    public ParcelUuid getServiceDataUuid() {
        return null;
    }
    public int getManufacturerId() {
        return 0;
    }
    public byte[] getManufacturerData() {
        return null;
    }
    public byte[] getManufacturerDataMask() {
        return null;
    }
    public boolean matches(ScanResult scanResult) {
        
        return true;
    }
    public static boolean matchesServiceUuids(ParcelUuid uuid, ParcelUuid parcelUuidMask,
            List<ParcelUuid> uuids) {
        
        return false;
    }
    public String toString() {
        return "";
    }
    public int hashCode() {
        return 0;
    }
    public boolean equals(Object obj) {
        return false;
    }
    public boolean isAllFieldsEmpty() {
        return EMPTY.equals(this);
    }
    public static final class Builder {
        public static final int LEN_IRK_OCTETS = 16;
        public Builder setDeviceName(String deviceName) {
            return this;
        }
        public Builder setDeviceAddress(String deviceAddress) {
            return setDeviceAddress(deviceAddress, BluetoothDevice.ADDRESS_TYPE_PUBLIC);
        }
        public Builder setDeviceAddress(String deviceAddress,
                                        @AddressType int addressType) {
            return this;
        }
        public Builder setServiceUuid(ParcelUuid serviceUuid) {
            return this;
        }
        public Builder setServiceUuid(ParcelUuid serviceUuid, ParcelUuid uuidMask) {
            return this;
        }
        public Builder setServiceSolicitationUuid(
                ParcelUuid serviceSolicitationUuid) {
            return this;
        }
        public Builder setServiceSolicitationUuid(
                ParcelUuid serviceSolicitationUuid,
                ParcelUuid solicitationUuidMask) {
            return this;
        }
        public Builder setServiceData(ParcelUuid serviceDataUuid, byte[] serviceData) {
            return this;
        }
        public Builder setServiceData(ParcelUuid serviceDataUuid,
                byte[] serviceData, byte[] serviceDataMask) {
            return this;
        }
        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData) {
            return this;
        }
        public Builder setManufacturerData(int manufacturerId, byte[] manufacturerData,
                byte[] manufacturerDataMask) {
            return this;
        }
        public ScanFilter build() {
            return new ScanFilter(null, null, null, null, null, null, null, null, null, 0, null, null, 0, null);
        }
    }
}
