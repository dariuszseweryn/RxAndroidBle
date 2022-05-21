package android.bluetooth.le;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

/**
 * Used for mocks and constants
 */
public class ScanResult implements Parcelable {
    public static final int DATA_COMPLETE = 0x00;
    public static final int DATA_TRUNCATED = 0x02;
    public static final int PHY_UNUSED = 0x00;
    public static final int SID_NOT_PRESENT = 0xFF;
    public static final int TX_POWER_NOT_PRESENT = 0x7F;
    public static final int PERIODIC_INTERVAL_NOT_PRESENT = 0x00;
    private static final int ET_LEGACY_MASK = 0x10;
    private static final int ET_CONNECTABLE_MASK = 0x01;
    @Deprecated
    public ScanResult(BluetoothDevice device, ScanRecord scanRecord, int rssi,
                      long timestampNanos) {

    }
    public ScanResult(BluetoothDevice device, int eventType, int primaryPhy, int secondaryPhy,
                      int advertisingSid, int txPower, int rssi, int periodicAdvertisingInterval,
                      ScanRecord scanRecord, long timestampNanos) {

    }
    private ScanResult(Parcel in) {
        readFromParcel(in);
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    private void readFromParcel(Parcel in) {
    }
    @Override
    public int describeContents() {
        return 0;
    }
    public BluetoothDevice getDevice() {
        return null;
    }
    public ScanRecord getScanRecord() {
        return null;
    }
    public int getRssi() {
        return 0;
    }
    @Override
    public int hashCode() {
        return 0;
    }
    @Override
    public boolean equals(Object obj) {
        return false;
    }
    @Override
    public String toString() {
        return "ScanResult";
    }
    public static final Creator<ScanResult> CREATOR = new Creator<ScanResult>() {
        @Override
        public ScanResult createFromParcel(Parcel source) {
            return new ScanResult(source);
        }
        @Override
        public ScanResult[] newArray(int size) {
            return new ScanResult[size];
        }
    };
}
